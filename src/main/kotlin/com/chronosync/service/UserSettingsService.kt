package com.chronosync.service

import com.chronosync.dto.settings.ChangePasswordRequest
import com.chronosync.dto.settings.NotificationSettingsDto
import com.chronosync.dto.settings.PrivacySettingsDto
import com.chronosync.dto.settings.UpdateSettingsRequest
import com.chronosync.dto.settings.UserSettingsResponse
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
        return userSettings.toResponse()
    }

    @Transactional
    fun updateSettings(principal: UserPrincipal, request: UpdateSettingsRequest): UserSettingsResponse {
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
            val user = userRepository.findById(principal.id)
                .orElseThrow { IllegalArgumentException("User not found") }
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
        return updatedSettings.toResponse()
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

        userRepository.save(user.copy(password = passwordEncoder.encode(request.newPassword), updatedAt = Instant.now()))
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
        return settings.toResponse()
    }

    private fun UserSettings.toResponse() = UserSettingsResponse(
        twoFactorEnabled = twoFactorEnabled,
        notifications = NotificationSettingsDto(
            push = pushNotifications,
            email = emailNotifications,
            sms = smsNotifications,
            scheduleReminders = scheduleReminders
        ),
        privacy = PrivacySettingsDto(
            showAvailability = showAvailability,
            dataAnalytics = dataAnalytics
        )
    )
}
