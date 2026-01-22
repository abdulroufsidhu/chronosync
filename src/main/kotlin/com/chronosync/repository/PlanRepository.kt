package com.chronosync.repository

import com.chronosync.entity.PlanEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PlanRepository : JpaRepository<PlanEntity, UUID> {
    fun findByName(name: String): PlanEntity?
}
