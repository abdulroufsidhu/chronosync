package com.chronosync.repository

import com.chronosync.entity.Organization
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrganizationRepository : JpaRepository<Organization, UUID> {
    fun findByName(name: String): Organization?
}
