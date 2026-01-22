package com.chronosync.dto.profile

import io.swagger.v3.oas.annotations.media.Schema

data class UpdateProfileRequest(
    @Schema(description = "User's first name", example = "John")
    val firstName: String? = null,

    @Schema(description = "User's last name", example = "Doe")
    val lastName: String? = null,

    @Schema(description = "User's phone number", example = "+1234567890")
    val phoneNumber: String? = null
)

data class ProfileResponse(
    @Schema(description = "User's full name")
    val name: String,

    @Schema(description = "User's email address")
    val email: String,

    @Schema(description = "User's role in the current organization", example = "OWNER")
    val role: String,

    @Schema(description = "List of organizations the user belongs to with their roles")
    val organizations: List<OrganizationInfo>
)

data class OrganizationInfo(
    @Schema(description = "Organization UUID")
    val id: String,

    @Schema(description = "Organization name")
    val name: String,

    @Schema(description = "Subscription plan", example = "FREE")
    val plan: String,

    @Schema(description = "User's role in this organization", example = "OWNER")
    val role: String
)
