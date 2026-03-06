package com.chronosync.dto.settings

import io.swagger.v3.oas.annotations.media.Schema

data class UserSettingsResponse(
    @Schema(description = "Whether two-factor authentication is enabled")
    val twoFactorEnabled: Boolean,

    @Schema(description = "Notification preferences")
    val notifications: NotificationSettingsDto,

    @Schema(description = "Privacy preferences")
    val privacy: PrivacySettingsDto
)

data class NotificationSettingsDto(
    @Schema(description = "Push notifications enabled")
    val push: Boolean,

    @Schema(description = "Email notifications enabled")
    val email: Boolean,

    @Schema(description = "SMS notifications enabled")
    val sms: Boolean,

    @Schema(description = "Schedule reminders enabled")
    val scheduleReminders: Boolean
)

data class PrivacySettingsDto(
    @Schema(description = "Show availability to others")
    val showAvailability: Boolean,

    @Schema(description = "Allow data analytics")
    val dataAnalytics: Boolean
)

data class UpdateSettingsRequest(
    @Schema(description = "Two-factor authentication enabled")
    val twoFactorEnabled: Boolean? = null,

    @Schema(description = "Notification preferences")
    val notifications: NotificationSettingsDto? = null,

    @Schema(description = "Privacy preferences")
    val privacy: PrivacySettingsDto? = null
)

data class ChangePasswordRequest(
    @Schema(description = "Current password")
    val currentPassword: String,

    @Schema(description = "New password")
    val newPassword: String
)
