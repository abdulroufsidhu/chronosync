package com.chronosync.service

import com.chronosync.dto.profile.OrganizationInfo
import com.chronosync.dto.profile.ProfileResponse
import com.chronosync.dto.profile.UpdateProfileRequest
import com.chronosync.entity.User
import com.chronosync.repository.OrganizationUserRepository
import com.chronosync.repository.UserRepository
import com.chronosync.security.UserPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ProfileService(
    private val userRepository: UserRepository,
    private val organizationUserRepository: OrganizationUserRepository
) {

    @Transactional(readOnly = true)
    fun getProfile(principal: UserPrincipal): ProfileResponse {
        val userOrganizations = organizationUserRepository.findByUserId(principal.id)

        val organizations = userOrganizations.map { orgUser ->
            OrganizationInfo(
                id = orgUser.organization.id.toString(),
                name = orgUser.organization.name,
                plan = orgUser.organization.plan.name,
                role = orgUser.role.name
            )
        }

        val firstOrgUser = userOrganizations.firstOrNull()
        val name = "${firstOrgUser?.user?.firstName ?: ""} ${firstOrgUser?.user?.lastName ?: ""}".trim()
            .ifEmpty { principal.email }

        return ProfileResponse(
            name = name,
            email = principal.email,
            role = principal.role,
            organizations = organizations
        )
    }

    @Transactional
    fun updateProfile(principal: UserPrincipal, request: UpdateProfileRequest): ProfileResponse {
        val user = userRepository.findById(principal.id)
            .orElseThrow { IllegalArgumentException("User not found") }

        val updatedUser = user.copy(
            firstName = request.firstName ?: user.firstName,
            lastName = request.lastName ?: user.lastName,
            phoneNumber = request.phoneNumber ?: user.phoneNumber,
            updatedAt = Instant.now()
        )
        userRepository.save(updatedUser)

        return getProfile(principal)
    }
}
