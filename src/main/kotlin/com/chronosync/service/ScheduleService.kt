package com.chronosync.service

import com.chronosync.dto.schedule.*
import com.chronosync.entity.OrganizationRole
import com.chronosync.entity.Plan
import com.chronosync.entity.Schedule
import com.chronosync.repository.OrganizationRepository
import com.chronosync.repository.ScheduleRepository
import com.chronosync.repository.UserRepository
import com.chronosync.security.UserPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository
) {

    @Transactional(readOnly = true)
    fun getTodaySchedules(principal: UserPrincipal): TodayScheduleResponse {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        val organization = organizationRepository.findById(principal.organizationId)
            .orElseThrow { IllegalArgumentException("Organization not found") }

        val schedules = if (isAdmin(principal.role)) {
            scheduleRepository.findByOrganizationIdAndDateRange(principal.organizationId, startOfDay, endOfDay)
        } else {
            scheduleRepository.findByOrganizationIdAndAssignedUserIdAndDateRange(
                principal.organizationId, principal.id, startOfDay, endOfDay
            )
        }

        return TodayScheduleResponse(
            date = today.toString(),
            userRole = principal.role,
            usage = UsageDto(
                used = organization.currentUsage,
                limit = organization.scheduleLimit
            ),
            schedules = schedules.map { it.toScheduleDto() }
        )
    }

    @Transactional(readOnly = true)
    fun getSchedules(principal: UserPrincipal, from: LocalDate, to: LocalDate): ScheduleListResponse {
        val startDate = from.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endDate = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        val schedules = if (isAdmin(principal.role)) {
            scheduleRepository.findByOrganizationIdAndDateRange(principal.organizationId, startDate, endDate)
        } else {
            scheduleRepository.findByOrganizationIdAndAssignedUserIdAndDateRange(
                principal.organizationId, principal.id, startDate, endDate
            )
        }

        return ScheduleListResponse(
            from = from.toString(),
            to = to.toString(),
            schedules = schedules.map { it.toScheduleDto() }
        )
    }

    @Transactional(readOnly = true)
    fun getSchedule(principal: UserPrincipal, scheduleId: UUID): ScheduleDto {
        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { IllegalArgumentException("Schedule not found") }

        if (schedule.organization.id != principal.organizationId) {
            throw IllegalArgumentException("Schedule not found")
        }

        if (!isAdmin(principal.role) && schedule.assignedUser?.id != principal.id) {
            throw IllegalArgumentException("Schedule not found")
        }

        return schedule.toScheduleDto()
    }

    @Transactional
    fun createSchedule(principal: UserPrincipal, request: CreateScheduleRequest): ScheduleDto {
        val organization = organizationRepository.findById(principal.organizationId)
            .orElseThrow { IllegalArgumentException("Organization not found") }

        if (!isAdmin(principal.role)) {
            throw IllegalArgumentException("Only admins can create schedules")
        }

        if (organization.plan.limit != -1 && organization.currentUsage >= organization.scheduleLimit) {
            throw IllegalArgumentException("Schedule limit reached. Please upgrade your plan.")
        }

        val assignedUser = request.assignedUserId?.let { userRepository.findById(UUID.fromString(it)).orElse(null) }

        val schedule = Schedule(
            title = request.title,
            startDateTime = request.startDateTime,
            endDateTime = request.endDateTime,
            organization = organization,
            assignedUser = assignedUser,
            clientName = request.clientName,
            clientEmail = request.clientEmail,
            clientPhone = request.clientPhone,
            clientAddress = request.clientAddress,
            notes = request.notes
        )

        val savedSchedule = scheduleRepository.save(schedule)

        val updatedOrg = organization.copy(currentUsage = organization.currentUsage + 1)
        organizationRepository.save(updatedOrg)

        return savedSchedule.toScheduleDto()
    }

    @Transactional
    fun updateSchedule(principal: UserPrincipal, scheduleId: UUID, request: UpdateScheduleRequest): ScheduleDto {
        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { IllegalArgumentException("Schedule not found") }

        if (schedule.organization.id != principal.organizationId) {
            throw IllegalArgumentException("Schedule not found")
        }

        if (!isAdmin(principal.role)) {
            throw IllegalArgumentException("Only admins can update schedules")
        }

        val assignedUser = request.assignedUserId?.let { userRepository.findById(UUID.fromString(it)).orElse(null) }

        val updatedSchedule = schedule.copy(
            title = request.title,
            startDateTime = request.startDateTime,
            endDateTime = request.endDateTime,
            assignedUser = assignedUser,
            clientName = request.clientName,
            clientEmail = request.clientEmail,
            clientPhone = request.clientPhone,
            clientAddress = request.clientAddress,
            notes = request.notes,
            updatedAt = Instant.now()
        )

        return scheduleRepository.save(updatedSchedule).toScheduleDto()
    }

    @Transactional
    fun deleteSchedule(principal: UserPrincipal, scheduleId: UUID) {
        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { IllegalArgumentException("Schedule not found") }

        if (schedule.organization.id != principal.organizationId) {
            throw IllegalArgumentException("Schedule not found")
        }

        if (!isAdmin(principal.role)) {
            throw IllegalArgumentException("Only admins can delete schedules")
        }

        val organization = schedule.organization
        val updatedOrg = organization.copy(currentUsage = (organization.currentUsage - 1).coerceAtLeast(0))
        organizationRepository.save(updatedOrg)

        scheduleRepository.delete(schedule)
    }

    private fun isAdmin(role: String): Boolean {
        return role == OrganizationRole.OWNER.name || role == OrganizationRole.MANAGER.name
    }

    private fun Schedule.toScheduleDto(): ScheduleDto {
        return ScheduleDto(
            id = this.id.toString(),
            title = this.title,
            startDateTime = this.startDateTime,
            endDateTime = this.endDateTime,
            organization = OrganizationInfoDto(
                id = this.organization.id.toString(),
                name = this.organization.name,
                plan = this.organization.plan.name,
                role = ""
            ),
            client = if (this.clientName != null) ClientDto(
                name = this.clientName!!,
                email = this.clientEmail ?: "",
                phoneNumber = this.clientPhone ?: "",
                address = this.clientAddress ?: ""
            ) else null,
            assignedUser = this.assignedUser?.let {
                UserInfoDto(
                    id = it.id.toString(),
                    name = "${it.firstName ?: ""} ${it.lastName ?: ""}".trim(),
                    email = it.email,
                    phoneNumber = it.phoneNumber,
                    address = null
                )
            },
            assignedUserId = this.assignedUser?.id?.toString(),
            notes = this.notes
        )
    }
}
