package com.chronosync.dto.auth

import io.swagger.v3.oas.annotations.media.Schema

data class LoginRequest(
    @Schema(description = "User email address", example = "user@example.com")
    val email: String,

    @Schema(description = "User password", example = "password123")
    val password: String
)

data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    @Schema(description = "Phone number for the admin user account", example = "+1234567890")
    val phoneNumber: String? = null,

    @Schema(description = "Email for the admin user account", example = "admin@example.com")
    val email: String,

    @Schema(description = "Password for the admin user account", example = "SecureP@ss123")
    val password: String,

    @Schema(description = "Organization details", example = "")
    val organization: OrganizationInfoRegisterDto
)

data class OrganizationInfoRegisterDto(
    @Schema(description = "Name of the organization", example = "My Salon")
    val name: String,

    @Schema(
        description = "List of services offered",
        example = "[\"cutting\", \"beard\", \"facial\"]"
    )
    val services: List<String>,

    @Schema(description = "Type of organization", example = "salon")
    val type: String,

    @Schema(
        description = "Role of the user in the organization. Must be one of: OWNER, MANAGER, MEMBER",
        example = "OWNER"
    )
    val role: String,

    @Schema(
        description = "Timezone for the organization (IANA format). Auto-detected from coordinates or IP if not provided.",
        example = "America/New_York"
    )
    val timezone: String? = null,

    @Schema(description = "Latitude of the organization's location", example = "40.7128")
    val latitude: Double? = null,

    @Schema(description = "Longitude of the organization's location", example = "-74.0060")
    val longitude: Double? = null,

    @Schema(description = "City or country where the organization is based", example = "New York")
    val basedIn: String? = null
)

data class AuthResponse(
    @Schema(description = "JWT access token for authentication")
    val accessToken: String,

    @Schema(description = "User information")
    val user: UserDto,

    @Schema(description = "Organization information")
    val organization: OrganizationDto
)

data class UserDto(
    @Schema(description = "User UUID")
    val id: String,

    @Schema(description = "User full name")
    val name: String,

    @Schema(description = "User email")
    val email: String,

    val firstName: String?,

    val lastName: String?,

    @Schema(description = "User phone number", example = "+1234567890")
    val phoneNumber: String? = null,

    @Schema(description = "User role in the organization", example = "OWNER")
    val role: String
)

data class OrganizationDto(
    @Schema(description = "Organization UUID")
    val id: String,

    @Schema(description = "Organization name")
    val name: String,

    @Schema(description = "Subscription plan", example = "FREE")
    val plan: String,

    @Schema(description = "User's role in the organization", example = "OWNER")
    val role: String,

    @Schema(description = "Organization timezone (IANA format)", example = "America/New_York")
    val timezone: String,

    @Schema(description = "Latitude of the organization's location")
    val latitude: Double? = null,

    @Schema(description = "Longitude of the organization's location")
    val longitude: Double? = null,

    @Schema(description = "City or country where the organization is based")
    val basedIn: String? = null
)

data class ForgotPasswordRequest(
    @Schema(description = "User email address", example = "user@example.com")
    val email: String
)

data class ResetPasswordRequest(
    @Schema(description = "Reset token from email", example = "abc123token")
    val token: String,

    @Schema(description = "New password", example = "NewSecureP@ss123")
    val newPassword: String
)

data class MagicLinkRequest(
    @Schema(description = "User email address", example = "user@example.com")
    val email: String
)

data class TokenResponse(
    @Schema(description = "Success message")
    val message: String
)

data class SwitchOrganizationRequest(
    @Schema(description = "Organization UUID to switch to", example = "uuid-string")
    val organizationId: String
)

data class AcceptInvitationRequest(
    @Schema(description = "Invitation token from email", example = "invitation_token")
    val token: String,

    @Schema(description = "User's first name", example = "John")
    val firstName: String,

    @Schema(description = "User's last name", example = "Doe")
    val lastName: String,

    @Schema(description = "Password to set for the account", example = "SecureP@ss123")
    val password: String
)
