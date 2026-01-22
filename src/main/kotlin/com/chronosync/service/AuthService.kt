package com.chronosync.service

import com.chronosync.dto.auth.*
import com.chronosync.entity.Organization
import com.chronosync.entity.OrganizationRole
import com.chronosync.entity.OrganizationUser
import com.chronosync.entity.OrganizationUserStatus
import com.chronosync.entity.User
import com.chronosync.entity.UserStatus
import com.chronosync.repository.OrganizationRepository
import com.chronosync.repository.OrganizationUserRepository
import com.chronosync.repository.UserRepository
import com.chronosync.security.JwtUtil
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val organizationUserRepository: OrganizationUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil
) {

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
                email = user.email,
                role = orgUser.role.name
            ),
            organization = OrganizationDto(
                id = orgUser.organization.id.toString(),
                name = orgUser.organization.name,
                plan = orgUser.organization.plan.name,
                role = orgUser.role.name
            )
        )
    }

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already registered")
        }

        val password = passwordEncoder.encode(request.password)

        val user = User(
            email = request.email,
            password = password,
            status = UserStatus.ACTIVE
        )
        val savedUser = userRepository.save(user)

        val organization = Organization(
            name = request.organization.name,
            type = request.organization.type,
            services = request.organization.services,
            plan = com.chronosync.entity.Plan.FREE,
            scheduleLimit = 100,
            currentUsage = 0,
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
                email = savedUser.email,
                role = ownerRole.name
            ),
            organization = OrganizationDto(
                id = savedOrg.id.toString(),
                name = savedOrg.name,
                plan = savedOrg.plan.name,
                role = ownerRole.name
            )
        )
    }
}
