package com.chronosync.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_settings")
data class UserSettings(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Column(name = "two_factor_enabled", nullable = false)
    val twoFactorEnabled: Boolean = false,

    @Column(name = "push_notifications", nullable = false)
    val pushNotifications: Boolean = true,

    @Column(name = "email_notifications", nullable = false)
    val emailNotifications: Boolean = true,

    @Column(name = "sms_notifications", nullable = false)
    val smsNotifications: Boolean = false,

    @Column(name = "schedule_reminders", nullable = false)
    val scheduleReminders: Boolean = true,

    @Column(name = "show_availability", nullable = false)
    val showAvailability: Boolean = true,

    @Column(name = "data_analytics", nullable = false)
    val dataAnalytics: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    val updatedAt: Instant? = null
)
