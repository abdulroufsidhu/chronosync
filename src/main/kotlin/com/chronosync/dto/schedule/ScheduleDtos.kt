package com.chronosync.dto.schedule

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class ScheduleDto(
    @Schema(description = "Schedule UUID")
    val id: String,

    @Schema(description = "Title of the schedule", example = "Client Appointment")
    val title: String,

    @Schema(description = "Start date and time in ISO format (UTC)")
    val startDateTime: Instant,

    @Schema(description = "End date and time in ISO format (UTC)")
    val endDateTime: Instant,

    @Schema(description = "Organization timezone (IANA format)", example = "America/New_York")
    val timezone: String,

    @Schema(description = "Organization details")
    val organization: OrganizationInfoDto? = null,

    @Schema(description = "Client information")
    val client: ClientDto? = null,

    @Schema(description = "Assigned user information")
    val assignedUser: UserInfoDto? = null,

    @Schema(description = "Assigned user UUID")
    val assignedUserId: String? = null,

    @Schema(description = "Additional notes")
    val notes: String? = null
)

data class OrganizationInfoDto(
    @Schema(description = "Organization UUID")
    val id: String,

    @Schema(description = "Organization name")
    val name: String,

    @Schema(description = "Subscription plan", example = "FREE")
    val plan: String,

    @Schema(description = "User role in the organization", example = "OWNER")
    val role: String,

    @Schema(description = "Organization timezone (IANA format)", example = "America/New_York")
    val timezone: String
)

data class ClientDto(
    @Schema(description = "Client name", example = "John Doe")
    val name: String,

    @Schema(description = "Client email", example = "john@example.com")
    val email: String,

    @Schema(description = "Client phone number", example = "+1234567890")
    val phoneNumber: String,

    @Schema(description = "Client address", example = "123 Main St")
    val address: String
)

data class UserInfoDto(
    @Schema(description = "User UUID")
    val id: String,

    @Schema(description = "User name")
    val name: String,

    @Schema(description = "User email")
    val email: String,

    @Schema(description = "User phone number")
    val phoneNumber: String? = null,

    @Schema(description = "User address")
    val address: String? = null
)

data class CreateScheduleRequest(
    @Schema(description = "Title of the schedule", example = "Haircut Appointment")
    val title: String,

    @Schema(description = "Start date and time in ISO format")
    val startDateTime: Instant,

    @Schema(description = "End date and time in ISO format")
    val endDateTime: Instant,

    @Schema(description = "UUID of the user to assign this schedule to")
    val assignedUserId: String? = null,

    @Schema(description = "Client name")
    val clientName: String? = null,

    @Schema(description = "Client email")
    val clientEmail: String? = null,

    @Schema(description = "Client phone number")
    val clientPhone: String? = null,

    @Schema(description = "Client address")
    val clientAddress: String? = null,

    @Schema(description = "Additional notes")
    val notes: String? = null
)

data class UpdateScheduleRequest(
    @Schema(description = "Title of the schedule")
    val title: String,

    @Schema(description = "Start date and time in ISO format")
    val startDateTime: Instant,

    @Schema(description = "End date and time in ISO format")
    val endDateTime: Instant,

    @Schema(description = "UUID of the user to assign this schedule to")
    val assignedUserId: String? = null,

    @Schema(description = "Client name")
    val clientName: String? = null,

    @Schema(description = "Client email")
    val clientEmail: String? = null,

    @Schema(description = "Client phone number")
    val clientPhone: String? = null,

    @Schema(description = "Client address")
    val clientAddress: String? = null,

    @Schema(description = "Additional notes")
    val notes: String? = null
)

data class TodayScheduleResponse(
    @Schema(description = "Date in YYYY-MM-DD format", example = "2025-01-22")
    val date: String,

    @Schema(description = "Current user's role in the organization", example = "OWNER")
    val userRole: String,

    @Schema(description = "Usage statistics")
    val usage: UsageDto,

    @Schema(description = "List of schedules for today")
    val schedules: List<ScheduleDto>
)

data class UsageDto(
    @Schema(description = "Number of schedules used this period")
    val used: Int,

    @Schema(description = "Maximum number of schedules allowed")
    val limit: Int
)

data class ScheduleListResponse(
    @Schema(description = "Start date in YYYY-MM-DD format")
    val from: String,

    @Schema(description = "End date in YYYY-MM-DD format")
    val to: String,

    @Schema(description = "List of schedules")
    val schedules: List<ScheduleDto>
)
