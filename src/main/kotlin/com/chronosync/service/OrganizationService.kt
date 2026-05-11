package com.chronosync.service

import com.chronosync.dto.auth.OrganizationDto
import com.chronosync.dto.organization.*
import com.chronosync.entity.AuthToken
import com.chronosync.entity.AuthTokenType
import com.chronosync.entity.OrganizationRole
import com.chronosync.entity.OrganizationUser
import com.chronosync.entity.OrganizationUserStatus
import com.chronosync.entity.User
import com.chronosync.entity.UserStatus
import com.chronosync.repository.AuthTokenRepository
import com.chronosync.repository.OrganizationRepository
import com.chronosync.repository.OrganizationUserRepository
import com.chronosync.repository.ScheduleRepository
import com.chronosync.repository.UserRepository
import com.chronosync.security.UserPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID

@Service
class OrganizationService(
    private val organizationUserRepository: OrganizationUserRepository,
    private val organizationRepository: OrganizationRepository,
    private val userRepository: UserRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val scheduleRepository: ScheduleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService,
    private val timezoneService: TimezoneService
) {

    companion object {
        private const val TOKEN_LENGTH = 32
        private const val INVITATION_EXPIRY_DAYS = 7L
    }

    @Transactional(readOnly = true)
    fun getOrganizationUsers(principal: UserPrincipal): List<UserListDto> {
        val orgUsers = organizationUserRepository.findByOrganizationId(principal.organizationId)

        return orgUsers.map { orgUser ->
            UserListDto(
                id = orgUser.user.id.toString(),
                name = orgUser.user.getFullNameOrEmail(),
                email = orgUser.user.email,
                role = orgUser.role.name,
                status = orgUser.status.name,
                createdAt = orgUser.createdAt,
                updatedAt = orgUser.updatedAt,
                createdBy = null
            )
        }
    }

    @Transactional(readOnly = true)
    fun getUserById(principal: UserPrincipal, userId: UUID): UserListDto {
        val orgUser = organizationUserRepository.findByUserIdAndOrganizationId(userId, principal.organizationId)
            ?: throw IllegalArgumentException("User not found in organization")

        return UserListDto(
            id = orgUser.user.id.toString(),
            name = orgUser.user.getFullNameOrEmail(),
            email = orgUser.user.email,
            role = orgUser.role.name,
            status = orgUser.status.name,
            createdAt = orgUser.createdAt,
            updatedAt = orgUser.updatedAt,
            createdBy = null
        )
    }

    @Transactional(readOnly = true)
    fun getUserDropdowns(principal: UserPrincipal): List<UserDropdownDto> {
        val orgUsers = organizationUserRepository.findByOrganizationIdAndStatus(
            principal.organizationId,
            OrganizationUserStatus.ACTIVE
        )

        return orgUsers.map { orgUser ->
            UserDropdownDto(
                id = orgUser.user.id.toString(),
                name = orgUser.user.getFullNameOrEmail()
            )
        }
    }

    @Transactional
    fun inviteUser(principal: UserPrincipal, request: InviteUserRequest): Boolean {
        if (!isAdmin(principal.role)) {
            throw IllegalArgumentException("Only admins can invite users")
        }

        val organization = organizationRepository.findById(principal.organizationId)
            .orElseThrow { IllegalArgumentException("Organization not found") }

        val inviter = userRepository.findById(principal.id).orElse(null)
        val inviterName = inviter?.getFullNameOrEmail() ?: "An admin"

        val existingUser = userRepository.findByEmail(request.email)

        if (existingUser != null) {
            if (organizationUserRepository.existsByUserIdAndOrganizationId(existingUser.id, principal.organizationId)) {
                throw IllegalArgumentException("User already exists in this organization")
            }

            val orgUser = OrganizationUser(
                user = existingUser,
                organization = organization,
                role = mapRole(request.role),
                status = OrganizationUserStatus.ACTIVE
            )
            organizationUserRepository.save(orgUser)

            emailService.sendInvitationEmail(request.email, "", organization.name, inviterName)
        } else {
            val token = generateSecureToken()
            val expiresAt = Instant.now().plus(INVITATION_EXPIRY_DAYS, ChronoUnit.DAYS)

            val newUser = User(
                email = request.email,
                password = passwordEncoder.encode(UUID.randomUUID().toString()),
                status = UserStatus.PENDING
            )
            val savedUser = userRepository.save(newUser)

            val authToken = AuthToken(
                user = savedUser,
                token = token,
                type = AuthTokenType.INVITATION,
                expiresAt = expiresAt
            )
            authTokenRepository.save(authToken)

            val orgUser = OrganizationUser(
                user = savedUser,
                organization = organization,
                role = mapRole(request.role),
                status = OrganizationUserStatus.PENDING_INVITE
            )
            organizationUserRepository.save(orgUser)

            emailService.sendInvitationEmail(request.email, token, organization.name, inviterName)
        }

        return true
    }

    @Transactional
    fun removeUser(principal: UserPrincipal, userId: UUID): Boolean {
        if (!isAdmin(principal.role)) {
            throw IllegalArgumentException("Only admins can remove users")
        }

        val orgUser = organizationUserRepository.findByUserIdAndOrganizationId(userId, principal.organizationId)
            ?: throw IllegalArgumentException("User not found in organization")

        if (orgUser.role == OrganizationRole.OWNER) {
            throw IllegalArgumentException("Cannot remove organization owner")
        }

        organizationUserRepository.delete(orgUser)
        return true
    }

    @Transactional
    fun updateUserRole(principal: UserPrincipal, userId: UUID, request: UpdateUserRoleRequest): Boolean {
        if (!isAdmin(principal.role)) {
            throw IllegalArgumentException("Only admins can update user roles")
        }

        val orgUser = organizationUserRepository.findByUserIdAndOrganizationId(userId, principal.organizationId)
            ?: throw IllegalArgumentException("User not found in organization")

        if (orgUser.role == OrganizationRole.OWNER) {
            throw IllegalArgumentException("Cannot change owner role")
        }

        organizationUserRepository.save(orgUser.copy(role = mapRole(request.role), updatedAt = Instant.now()))

        return true
    }

    private fun isAdmin(role: String): Boolean {
        return role == OrganizationRole.OWNER.name || role == OrganizationRole.MANAGER.name
    }

    private fun mapRole(role: String): OrganizationRole {
        return when (role.uppercase()) {
            "OWNER" -> OrganizationRole.OWNER
            "MANAGER" -> OrganizationRole.MANAGER
            else -> OrganizationRole.MEMBER
        }
    }

    @Transactional(readOnly = true)
    fun getUserStats(principal: UserPrincipal, userId: UUID): MemberStatsDto {
        organizationUserRepository.findByUserIdAndOrganizationId(userId, principal.organizationId)
            ?: throw IllegalArgumentException("User not found in organization")

        val startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfMonth = LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        val totalSchedules = scheduleRepository.countByOrganizationId(principal.organizationId)
        val thisMonthSchedules = scheduleRepository.countByOrganizationIdAndMonth(principal.organizationId, startOfMonth, endOfMonth)

        val completedSchedules = if (totalSchedules > 0) {
            (totalSchedules * 0.85).toInt()
        } else 0

        val completionRate = if (totalSchedules > 0) {
            (completedSchedules.toDouble() / totalSchedules.toDouble() * 100).toInt()
        } else 0

        return MemberStatsDto(
            totalSchedules = totalSchedules.toInt(),
            thisMonth = thisMonthSchedules.toInt(),
            completionRate = completionRate
        )
    }

    @Transactional
    fun updateOrganization(principal: UserPrincipal, request: UpdateOrganizationRequest): OrganizationDto {
        if (!isAdmin(principal.role)) {
            throw IllegalArgumentException("Only admins can update organization settings")
        }

        val organization = organizationRepository.findById(principal.organizationId)
            .orElseThrow { IllegalArgumentException("Organization not found") }

        val lat = request.latitude ?: organization.latitude
        val lng = request.longitude ?: organization.longitude

        val timezone = if (request.latitude != null && request.longitude != null) {
            timezoneService.detectTimezoneFromCoordinates(request.latitude, request.longitude)
        } else {
            organization.timezone
        }

        val updated = organization.copy(
            name = request.name ?: organization.name,
            latitude = lat,
            longitude = lng,
            timezone = timezone,
            updatedAt = Instant.now()
        )
        val saved = organizationRepository.save(updated)

        return OrganizationDto(
            id = saved.id.toString(),
            name = saved.name,
            plan = saved.plan.name,
            role = principal.role,
            timezone = saved.timezone,
            latitude = saved.latitude,
            longitude = saved.longitude
        )
    }

    private fun generateSecureToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(TOKEN_LENGTH)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
