package com.chronosync.dto.auth

import io.swagger.v3.oas.annotations.media.Schema

data class LoginRequest(
    @Schema(description = "User email address", example = "user@example.com")
    val email: String,

    @Schema(description = "User password", example = "password123")
    val password: String
)

data class RegisterRequest(
    @Schema(description = "Email for the admin user account", example = "admin@example.com")
    val email: String,

    @Schema(description = "Password for the admin user account", example = "SecureP@ss123")
    val password: String,

    @Schema(description = "Organization details")
    val organization: OrganizationInfoRegisterDto
)

data class OrganizationInfoRegisterDto(
    @Schema(description = "Name of the organization", example = "My Salon")
    val name: String,

    @Schema(description = "List of services offered", example = "[\"cutting\", \"beard\", \"facial\"]")
    val services: List<String>,

    @Schema(description = "Type of organization", example = "salon")
    val type: String,

    @Schema(
        description = "Role of the user in the organization. Must be one of: OWNER, MANAGER, MEMBER",
        example = "OWNER"
    )
    val role: String
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

    @Schema(description = "User email")
    val email: String,

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
    val role: String
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
