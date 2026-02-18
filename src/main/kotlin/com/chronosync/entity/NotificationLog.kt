package com.chronosync.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notification_logs")
data class NotificationLog(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "schedule_id", nullable = false)
    val scheduleId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false)
    val recipientType: RecipientType,

    @Column(name = "recipient_email", nullable = false)
    val recipientEmail: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    val notificationType: NotificationType,

    @Column(name = "sent_at")
    val sentAt: Instant = Instant.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: NotificationStatus,

    @Column(name = "error_message")
    val errorMessage: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

enum class RecipientType {
    STAFF, CLIENT
}

enum class NotificationType {
    SCHEDULE_CREATED, SCHEDULE_UPDATED, SCHEDULE_CANCELLED
}

enum class NotificationStatus {
    SENT, FAILED
}
