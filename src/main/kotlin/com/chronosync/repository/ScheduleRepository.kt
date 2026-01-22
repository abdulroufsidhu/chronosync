package com.chronosync.repository

import com.chronosync.entity.Schedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface ScheduleRepository : JpaRepository<Schedule, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<Schedule>

    @Query("SELECT s FROM Schedule s WHERE s.organization.id = :organizationId " +
            "AND s.startDateTime >= :from AND s.startDateTime < :to " +
            "ORDER BY s.startDateTime ASC")
    fun findByOrganizationIdAndDateRange(
        @Param("organizationId") organizationId: UUID,
        @Param("from") from: Instant,
        @Param("to") to: Instant
    ): List<Schedule>

    @Query("SELECT s FROM Schedule s WHERE s.organization.id = :organizationId " +
            "AND s.assignedUser.id = :userId " +
            "AND s.startDateTime >= :from AND s.startDateTime < :to " +
            "ORDER BY s.startDateTime ASC")
    fun findByOrganizationIdAndAssignedUserIdAndDateRange(
        @Param("organizationId") organizationId: UUID,
        @Param("userId") userId: UUID,
        @Param("from") from: Instant,
        @Param("to") to: Instant
    ): List<Schedule>

    fun countByOrganizationId(organizationId: UUID): Long

    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.organization.id = :organizationId " +
            "AND s.startDateTime >= :startOfMonth AND s.startDateTime < :endOfMonth")
    fun countByOrganizationIdAndMonth(
        @Param("organizationId") organizationId: UUID,
        @Param("startOfMonth") startOfMonth: Instant,
        @Param("endOfMonth") endOfMonth: Instant
    ): Long
}
