package com.chronosync.service

import com.chronosync.dto.settings.ChangePasswordRequest
import com.chronosync.dto.settings.NotificationSettingsDto
import com.chronosync.dto.settings.PrivacySettingsDto
import com.chronosync.dto.settings.UpdateSettingsRequest
import com.chronosync.dto.settings.UserSettingsResponse
import com.chronosync.entity.User
import com.chronosync.entity.UserSettings
import com.chronosync.repository.UserRepository
import com.chronosync.repository.UserSettingsRepository
import com.chronosync.security.UserPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class UserSettingsService(
    private val userSettingsRepository: UserSettingsRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional(readOnly = true)
    fun getSettings(principal: UserPrincipal): UserSettingsResponse {
        val userSettings = userSettingsRepository.findByUserId(principal.id)
            ?: return createDefaultSettings(principal)

        return UserSettingsResponse(
            twoFactorEnabled = userSettings.twoFactorEnabled,
            notifications = NotificationSettingsDto(
                push = userSettings.pushNotifications,
                email = userSettings.emailNotifications,
                sms = userSettings.smsNotifications,
                scheduleReminders = userSettings.scheduleReminders
            ),
            privacy = PrivacySettingsDto(
                showAvailability = userSettings.showAvailability,
                dataAnalytics = userSettings.dataAnalytics
            )
        )
    }

    @Transactional
    fun updateSettings(principal: UserPrincipal, request: UpdateSettingsRequest): UserSettingsResponse {
        val user = userRepository.findById(principal.id)
            .orElseThrow { IllegalArgumentException("User not found") }

        val existingSettings = userSettingsRepository.findByUserId(principal.id)

        val updatedSettings = if (existingSettings != null) {
            existingSettings.copy(
                twoFactorEnabled = request.twoFactorEnabled ?: existingSettings.twoFactorEnabled,
                pushNotifications = request.notifications?.push ?: existingSettings.pushNotifications,
                emailNotifications = request.notifications?.email ?: existingSettings.emailNotifications,
                smsNotifications = request.notifications?.sms ?: existingSettings.smsNotifications,
                scheduleReminders = request.notifications?.scheduleReminders ?: existingSettings.scheduleReminders,
                showAvailability = request.privacy?.showAvailability ?: existingSettings.showAvailability,
                dataAnalytics = request.privacy?.dataAnalytics ?: existingSettings.dataAnalytics,
                updatedAt = Instant.now()
            )
        } else {
            UserSettings(
                user = user,
                twoFactorEnabled = request.twoFactorEnabled ?: false,
                pushNotifications = request.notifications?.push ?: true,
                emailNotifications = request.notifications?.email ?: true,
                smsNotifications = request.notifications?.sms ?: false,
                scheduleReminders = request.notifications?.scheduleReminders ?: true,
                showAvailability = request.privacy?.showAvailability ?: true,
                dataAnalytics = request.privacy?.dataAnalytics ?: true
            )
        }

        userSettingsRepository.save(updatedSettings)

        return UserSettingsResponse(
            twoFactorEnabled = updatedSettings.twoFactorEnabled,
            notifications = NotificationSettingsDto(
                push = updatedSettings.pushNotifications,
                email = updatedSettings.emailNotifications,
                sms = updatedSettings.smsNotifications,
                scheduleReminders = updatedSettings.scheduleReminders
            ),
            privacy = PrivacySettingsDto(
                showAvailability = updatedSettings.showAvailability,
                dataAnalytics = updatedSettings.dataAnalytics
            )
        )
    }

    @Transactional
    fun changePassword(principal: UserPrincipal, request: ChangePasswordRequest): Boolean {
        val user = userRepository.findById(principal.id)
            .orElseThrow { IllegalArgumentException("User not found") }

        if (!passwordEncoder.matches(request.currentPassword, user.password)) {
            throw IllegalArgumentException("Current password is incorrect")
        }

        if (request.newPassword.length < 8) {
            throw IllegalArgumentException("New password must be at least 8 characters long")
        }

        val updatedUser = user.copy(
            password = passwordEncoder.encode(request.newPassword),
            updatedAt = Instant.now()
        )
        userRepository.save(updatedUser)

        return true
    }

    private fun createDefaultSettings(principal: UserPrincipal): UserSettingsResponse {
        val user = userRepository.findById(principal.id)
            .orElseThrow { IllegalArgumentException("User not found") }

        val settings = UserSettings(
            user = user,
            twoFactorEnabled = false,
            pushNotifications = true,
            emailNotifications = true,
            smsNotifications = false,
            scheduleReminders = true,
            showAvailability = true,
            dataAnalytics = true
        )
        userSettingsRepository.save(settings)

        return UserSettingsResponse(
            twoFactorEnabled = false,
            notifications = NotificationSettingsDto(
                push = true,
                email = true,
                sms = false,
                scheduleReminders = true
            ),
            privacy = PrivacySettingsDto(
                showAvailability = true,
                dataAnalytics = true
            )
        )
    }
}
