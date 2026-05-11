package com.chronosync.service

import com.chronosync.dto.auth.*
import com.chronosync.entity.*
import com.chronosync.repository.AuthTokenRepository
import com.chronosync.repository.OrganizationRepository
import com.chronosync.repository.OrganizationUserRepository
import com.chronosync.repository.UserRepository
import com.chronosync.security.JwtUtil
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val organizationUserRepository: OrganizationUserRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
    private val emailService: EmailService,
    private val timezoneService: TimezoneService
) {

    companion object {
        private const val TOKEN_LENGTH = 32
        private const val PASSWORD_RESET_EXPIRY_HOURS = 1L
        private const val MAGIC_LINK_EXPIRY_MINUTES = 15L
        private const val INVITATION_EXPIRY_DAYS = 7L
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmailAndStatus(request.email, UserStatus.ACTIVE)
            ?: throw IllegalArgumentException("Invalid credentials")

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw IllegalArgumentException("Invalid credentials")
        }

        val orgUser = organizationUserRepository.findByUserId(user.id).firstOrNull()
            ?: throw IllegalStateException("User not associated with any organization")

        val token = jwtUtil.generateToken(
            userId = user.id,
            email = user.email,
            organizationId = orgUser.organization.id,
            role = orgUser.role.name
        )

        return AuthResponse(
            accessToken = token,
            user = UserDto(
                id = user.id.toString(),
                name = user.getFullNameOrEmail(),
                email = user.email,
                role = orgUser.role.name,
                firstName = user.firstName,
                lastName = user.lastName,
                phoneNumber = user.phoneNumber,
            ),
            organization = OrganizationDto(
                id = orgUser.organization.id.toString(),
                name = orgUser.organization.name,
                plan = orgUser.organization.plan.name,
                role = orgUser.role.name,
                timezone = orgUser.organization.timezone,
                latitude = orgUser.organization.latitude,
                longitude = orgUser.organization.longitude
            )
        )
    }

    @Transactional
    fun register(request: RegisterRequest, clientIp: String? = null): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already registered")
        }

        val password = passwordEncoder.encode(request.password)

        val user = User(
            email = request.email,
            password = password,
            status = UserStatus.ACTIVE,
            firstName = request.firstName,
            lastName = request.lastName,
            phoneNumber = request.phoneNumber,
        )
        val savedUser = userRepository.save(user)

        val lat = request.organization.latitude
        val lng = request.organization.longitude
        val timezone = when {
            lat != null && lng != null -> timezoneService.detectTimezoneFromCoordinates(lat, lng)
            request.organization.timezone != null -> request.organization.timezone
            else -> timezoneService.detectTimezoneFromIp(clientIp)
        }

        val organization = Organization(
            name = request.organization.name,
            type = request.organization.type,
            services = request.organization.services,
            plan = com.chronosync.entity.Plan.FREE,
            scheduleLimit = 100,
            currentUsage = 0,
            latitude = lat,
            longitude = lng,
            timezone = timezone,
            nextReset = Instant.now().plus(30, ChronoUnit.DAYS)
        )
        val savedOrg = organizationRepository.save(organization)

        val ownerRole = when (request.organization.role.uppercase()) {
            "OWNER" -> OrganizationRole.OWNER
            "MANAGER" -> OrganizationRole.MANAGER
            else -> OrganizationRole.MEMBER
        }

        val orgUser = OrganizationUser(
            user = savedUser,
            organization = savedOrg,
            role = ownerRole,
            status = OrganizationUserStatus.ACTIVE
        )
        organizationUserRepository.save(orgUser)

        val token = jwtUtil.generateToken(
            userId = savedUser.id,
            email = savedUser.email,
            organizationId = savedOrg.id,
            role = ownerRole.name
        )

        return AuthResponse(
            accessToken = token,
            user = UserDto(
                id = savedUser.id.toString(),
                name = savedUser.getFullNameOrEmail(),
                email = savedUser.email,
                role = ownerRole.name,
                firstName = savedUser.firstName,
                lastName = savedUser.lastName,
                phoneNumber = savedUser.phoneNumber,
            ),
            organization = OrganizationDto(
                id = savedOrg.id.toString(),
                name = savedOrg.name,
                plan = savedOrg.plan.name,
                role = ownerRole.name,
                timezone = savedOrg.timezone,
                latitude = savedOrg.latitude,
                longitude = savedOrg.longitude
            )
        )
    }

    @Transactional
    fun forgotPassword(email: String) {
        val user = userRepository.findByEmailAndStatus(email, UserStatus.ACTIVE)
            ?: return // Don't reveal if email exists

        // Invalidate any existing password reset tokens
        authTokenRepository.deleteByUserIdAndType(user.id, AuthTokenType.PASSWORD_RESET)

        // Generate new token
        val token = generateSecureToken()
        val expiresAt = Instant.now().plus(PASSWORD_RESET_EXPIRY_HOURS, ChronoUnit.HOURS)

        val authToken = AuthToken(
            user = user,
            token = token,
            type = AuthTokenType.PASSWORD_RESET,
            expiresAt = expiresAt
        )
        authTokenRepository.save(authToken)

        // Send email asynchronously
        emailService.sendPasswordResetEmail(email, token)
    }

    @Transactional
    fun resetPassword(token: String, newPassword: String): AuthResponse {
        val authToken = authTokenRepository.findByTokenAndType(token, AuthTokenType.PASSWORD_RESET)
            ?: throw IllegalArgumentException("Invalid or expired token")

        if (!authToken.isValid()) {
            throw IllegalArgumentException("Invalid or expired token")
        }

        // Validate password strength
        if (newPassword.length < 8) {
            throw IllegalArgumentException("Password must be at least 8 characters long")
        }

        val user = authToken.user

        // Update password
        val encodedPassword = passwordEncoder.encode(newPassword)
        val updatedUser = user.copy(
            password = encodedPassword,
            updatedAt = Instant.now()
        )
        userRepository.save(updatedUser)

        // Mark token as used
        val usedToken = authToken.copy(usedAt = Instant.now())
        authTokenRepository.save(usedToken)

        // Generate JWT and return auth response
        return createAuthResponse(updatedUser)
    }

    @Transactional
    fun requestMagicLink(email: String) {
        val user = userRepository.findByEmailAndStatus(email, UserStatus.ACTIVE)

        if (user != null) {
            // Invalidate any existing magic link tokens
            authTokenRepository.deleteByUserIdAndType(user.id, AuthTokenType.MAGIC_LINK)

            // Generate new token
            val token = generateSecureToken()
            val expiresAt = Instant.now().plus(MAGIC_LINK_EXPIRY_MINUTES, ChronoUnit.MINUTES)

            val authToken = AuthToken(
                user = user,
                token = token,
                type = AuthTokenType.MAGIC_LINK,
                expiresAt = expiresAt
            )
            authTokenRepository.save(authToken)

            // Send email asynchronously
            emailService.sendMagicLinkEmail(email, token)
        }
        // Don't reveal if email exists or not
    }

    @Transactional
    fun verifyMagicLink(token: String): AuthResponse {
        val authToken = authTokenRepository.findByTokenAndType(token, AuthTokenType.MAGIC_LINK)
            ?: throw IllegalArgumentException("Invalid or expired magic link")

        if (!authToken.isValid()) {
            throw IllegalArgumentException("Invalid or expired magic link")
        }

        val user = authToken.user

        // Mark token as used
        val usedToken = authToken.copy(usedAt = Instant.now())
        authTokenRepository.save(usedToken)

        // If email is not verified, mark it as verified
        if (!user.emailVerified) {
            val updatedUser = user.copy(emailVerified = true, updatedAt = Instant.now())
            userRepository.save(updatedUser)
            return createAuthResponse(updatedUser)
        }

        return createAuthResponse(user)
    }

    @Transactional
    fun switchOrganization(userId: UUID, organizationId: UUID): AuthResponse {
        val orgUser = organizationUserRepository.findByUserIdAndOrganizationId(userId, organizationId)
            ?: throw IllegalArgumentException("User does not belong to this organization")

        val token = jwtUtil.generateToken(
            userId = userId,
            email = orgUser.user.email,
            organizationId = organizationId,
            role = orgUser.role.name
        )

        return AuthResponse(
            accessToken = token,
            user = UserDto(
                id = orgUser.user.id.toString(),
                name = orgUser.user.getFullNameOrEmail(),
                email = orgUser.user.email,
                role = orgUser.role.name,
                firstName = orgUser.user.firstName,
                lastName = orgUser.user.lastName,
                phoneNumber = orgUser.user.phoneNumber,
            ),
            organization = OrganizationDto(
                id = orgUser.organization.id.toString(),
                name = orgUser.organization.name,
                plan = orgUser.organization.plan.name,
                role = orgUser.role.name,
                timezone = orgUser.organization.timezone,
                latitude = orgUser.organization.latitude,
                longitude = orgUser.organization.longitude
            )
        )
    }

    @Transactional
    fun acceptInvitation(token: String, firstName: String, lastName: String, password: String): AuthResponse {
        val authToken = authTokenRepository.findByTokenAndType(token, AuthTokenType.INVITATION)
            ?: throw IllegalArgumentException("Invalid or expired invitation")

        if (!authToken.isValid()) {
            throw IllegalArgumentException("Invalid or expired invitation")
        }

        if (password.length < 8) {
            throw IllegalArgumentException("Password must be at least 8 characters long")
        }

        val user = authToken.user
        val encodedPassword = passwordEncoder.encode(password)

        val updatedUser = user.copy(
            firstName = firstName,
            lastName = lastName,
            password = encodedPassword,
            status = UserStatus.ACTIVE,
            updatedAt = Instant.now()
        )
        userRepository.save(updatedUser)

        val orgUser = organizationUserRepository.findByUserId(user.id).firstOrNull()
            ?: throw IllegalStateException("User not associated with any organization")

        val updatedOrgUser = orgUser.copy(
            status = OrganizationUserStatus.ACTIVE,
            updatedAt = Instant.now()
        )
        organizationUserRepository.save(updatedOrgUser)

        val usedToken = authToken.copy(usedAt = Instant.now())
        authTokenRepository.save(usedToken)

        val newToken = jwtUtil.generateToken(
            userId = user.id,
            email = user.email,
            organizationId = orgUser.organization.id,
            role = orgUser.role.name
        )

        return AuthResponse(
            accessToken = newToken,
            user = UserDto(
                id = user.id.toString(),
                name = updatedUser.getFullNameOrEmail(),
                email = user.email,
                role = orgUser.role.name,
                firstName = updatedUser.firstName,
                lastName = updatedUser.lastName,
                phoneNumber = updatedUser.phoneNumber,
            ),
            organization = OrganizationDto(
                id = orgUser.organization.id.toString(),
                name = orgUser.organization.name,
                plan = orgUser.organization.plan.name,
                role = orgUser.role.name,
                timezone = orgUser.organization.timezone,
                latitude = orgUser.organization.latitude,
                longitude = orgUser.organization.longitude
            )
        )
    }

    private fun createAuthResponse(user: User): AuthResponse {
        val orgUser = organizationUserRepository.findByUserId(user.id).firstOrNull()
            ?: throw IllegalStateException("User not associated with any organization")

        val token = jwtUtil.generateToken(
            userId = user.id,
            email = user.email,
            organizationId = orgUser.organization.id,
            role = orgUser.role.name
        )

        return AuthResponse(
            accessToken = token,
            user = UserDto(
                id = user.id.toString(),
                name = user.getFullNameOrEmail(),
                email = user.email,
                role = orgUser.role.name,
                firstName = user.firstName,
                lastName = user.lastName,
                phoneNumber = user.phoneNumber,
            ),
            organization = OrganizationDto(
                id = orgUser.organization.id.toString(),
                name = orgUser.organization.name,
                plan = orgUser.organization.plan.name,
                role = orgUser.role.name,
                timezone = orgUser.organization.timezone,
                latitude = orgUser.organization.latitude,
                longitude = orgUser.organization.longitude
            )
        )
    }

    private fun generateSecureToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(TOKEN_LENGTH)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
