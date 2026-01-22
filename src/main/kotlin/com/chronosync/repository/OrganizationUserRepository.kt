package com.chronosync.repository

import com.chronosync.entity.OrganizationRole
import com.chronosync.entity.OrganizationUser
import com.chronosync.entity.OrganizationUserStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrganizationUserRepository : JpaRepository<OrganizationUser, UUID> {
    fun findByUserIdAndOrganizationId(userId: UUID, organizationId: UUID): OrganizationUser?
    fun findByUserId(userId: UUID): List<OrganizationUser>
    fun findByOrganizationId(organizationId: UUID): List<OrganizationUser>
    fun findByOrganizationIdAndStatus(organizationId: UUID, status: OrganizationUserStatus): List<OrganizationUser>
    fun findByOrganizationIdAndRole(organizationId: UUID, role: OrganizationRole): List<OrganizationUser>
    fun existsByUserIdAndOrganizationId(userId: UUID, organizationId: UUID): Boolean
}
