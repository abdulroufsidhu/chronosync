package com.chronosync.service

import com.chronosync.dto.organization.*
import com.chronosync.entity.OrganizationRole
import com.chronosync.entity.OrganizationUser
import com.chronosync.entity.OrganizationUserStatus
import com.chronosync.entity.User
import com.chronosync.entity.UserStatus
import com.chronosync.repository.OrganizationRepository
import com.chronosync.repository.OrganizationUserRepository
import com.chronosync.repository.UserRepository
import com.chronosync.security.UserPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OrganizationService(
    private val organizationUserRepository: OrganizationUserRepository,
    private val organizationRepository: OrganizationRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional(readOnly = true)
    fun getOrganizationUsers(principal: UserPrincipal): List<UserListDto> {
        val orgUsers = organizationUserRepository.findByOrganizationId(principal.organizationId)

        return orgUsers.map { orgUser ->
            val createdBy = orgUser.createdAt?.let { createdAt ->
                findCreator(orgUser, createdAt)
            }

            UserListDto(
                id = orgUser.user.id.toString(),
                email = orgUser.user.email,
                role = orgUser.role.name,
                status = orgUser.status.name,
                createdAt = orgUser.createdAt,
                updatedAt = orgUser.updatedAt,
                createdBy = createdBy
            )
        }
    }

    private fun findCreator(orgUser: OrganizationUser, createdAt: java.time.Instant): CreatedByDto? {
        val allUsers = organizationUserRepository.findByOrganizationId(orgUser.organization.id)
        val creator = allUsers.find { it.id != orgUser.id }
        return creator?.let {
            CreatedByDto(
                id = it.user.id.toString(),
                name = "${it.user.firstName ?: ""} ${it.user.lastName ?: ""}".trim(),
                email = it.user.email,
                role = it.role.name
            )
        }
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
                name = "${orgUser.user.firstName ?: ""} ${orgUser.user.lastName ?: ""}".trim().ifEmpty { orgUser.user.email }
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
        } else {
            val tempPassword = UUID.randomUUID().toString().take(8)
            val newUser = User(
                email = request.email,
                password = passwordEncoder.encode(tempPassword),
                status = UserStatus.PENDING
            )
            val savedUser = userRepository.save(newUser)

            val orgUser = OrganizationUser(
                user = savedUser,
                organization = organization,
                role = mapRole(request.role),
                status = OrganizationUserStatus.PENDING_INVITE
            )
            organizationUserRepository.save(orgUser)
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

        val updatedOrgUser = orgUser.copy(
            role = mapRole(request.role),
            updatedAt = java.time.Instant.now()
        )
        organizationUserRepository.save(updatedOrgUser)

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
}
