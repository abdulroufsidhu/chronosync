package com.chronosync.dto.organization

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class UserListDto(
    @Schema(description = "User UUID")
    val id: String,

    @Schema(description = "User full name")
    val name: String,

    @Schema(description = "User email")
    val email: String,

    @Schema(description = "User role in the organization", example = "OWNER")
    val role: String,

    @Schema(description = "User status", example = "ACTIVE")
    val status: String,

    @Schema(description = "When the user was added to the organization")
    val createdAt: Instant,

    @Schema(description = "When the user was last updated")
    val updatedAt: Instant?,

    @Schema(description = "Who invited this user")
    val createdBy: CreatedByDto?
)

data class CreatedByDto(
    @Schema(description = "Creator UUID")
    val id: String,

    @Schema(description = "Creator name")
    val name: String,

    @Schema(description = "Creator email")
    val email: String,

    @Schema(description = "Creator role", example = "OWNER")
    val role: String
)

data class InviteUserRequest(
    @Schema(description = "Email of the user to invite", example = "staff@example.com")
    val email: String,

    @Schema(
        description = "Role to assign to the invited user. Must be one of: OWNER, MANAGER, MEMBER",
        example = "MEMBER"
    )
    val role: String
)

data class UpdateUserRoleRequest(
    @Schema(
        description = "New role for the user. Must be one of: MANAGER, MEMBER (cannot change OWNER role)",
        example = "MANAGER"
    )
    val role: String
)

data class UserDropdownDto(
    @Schema(description = "User UUID")
    val id: String,

    @Schema(description = "User name or email")
    val name: String
)

data class MemberStatsDto(
    @Schema(description = "Total number of schedules")
    val totalSchedules: Int,

    @Schema(description = "Number of schedules this month")
    val thisMonth: Int,

    @Schema(description = "Completion rate percentage")
    val completionRate: Int
)

data class UpdateOrganizationRequest(
    @Schema(description = "Organization name")
    val name: String? = null,

    @Schema(description = "Latitude of the organization's location", example = "40.7128")
    val latitude: Double? = null,

    @Schema(description = "Longitude of the organization's location", example = "-74.0060")
    val longitude: Double? = null
)
