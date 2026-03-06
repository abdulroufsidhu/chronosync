package com.chronosync.service

import com.chronosync.dto.notification.NotificationDto
import com.chronosync.dto.notification.NotificationListResponse
import com.chronosync.entity.*
import com.chronosync.repository.NotificationLogRepository
import com.chronosync.repository.NotificationRepository
import com.chronosync.repository.UserRepository
import com.chronosync.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class NotificationService(
    private val emailService: EmailService,
    private val notificationLogRepository: NotificationLogRepository,
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    @Async
    fun sendScheduleNotifications(
        schedule: Schedule,
        organization: Organization,
        notificationType: NotificationType
    ) {
        logger.info("Sending $notificationType notifications for schedule ${schedule.id}")

        // Send to staff
        schedule.assignedUser?.email?.let { email ->
            sendNotification(schedule, organization, RecipientType.STAFF, email, notificationType)
        }

        // Send to client
        schedule.clientEmail?.let { email ->
            sendNotification(schedule, organization, RecipientType.CLIENT, email, notificationType)
        }
    }

    private fun sendNotification(
        schedule: Schedule,
        organization: Organization,
        recipientType: RecipientType,
        recipientEmail: String,
        notificationType: NotificationType
    ) {
        try {
            emailService.sendScheduleNotification(schedule, organization, recipientType, notificationType)

            notificationLogRepository.save(
                NotificationLog(
                    scheduleId = schedule.id,
                    recipientType = recipientType,
                    recipientEmail = recipientEmail,
                    notificationType = notificationType,
                    status = NotificationStatus.SENT
                )
            )

            logger.info("Notification sent successfully to $recipientEmail")
        } catch (e: Exception) {
            logger.error("Failed to send notification to $recipientEmail", e)

            notificationLogRepository.save(
                NotificationLog(
                    scheduleId = schedule.id,
                    recipientType = recipientType,
                    recipientEmail = recipientEmail,
                    notificationType = notificationType,
                    status = NotificationStatus.FAILED,
                    errorMessage = e.message
                )
            )
        }
    }

    // In-app notification methods
    @Transactional(readOnly = true)
    fun getNotifications(principal: UserPrincipal, page: Int = 0, size: Int = 20): NotificationListResponse {
        val pageable = PageRequest.of(page, size)
        val notificationsPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(principal.id, pageable)
        val unreadCount = notificationRepository.countByUserIdAndIsReadFalse(principal.id)

        val notifications = notificationsPage.content.map { it.toDto() }

        return NotificationListResponse(
            notifications = notifications,
            page = notificationsPage.number,
            size = notificationsPage.size,
            totalElements = notificationsPage.totalElements,
            totalPages = notificationsPage.totalPages,
            unreadCount = unreadCount
        )
    }

    @Transactional
    fun markAsRead(principal: UserPrincipal, notificationId: UUID): Boolean {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { IllegalArgumentException("Notification not found") }

        if (notification.user.id != principal.id) {
            throw IllegalArgumentException("Notification not found")
        }

        val updated = notification.copy(isRead = true)
        notificationRepository.save(updated)
        return true
    }

    @Transactional
    fun markAllAsRead(principal: UserPrincipal): Boolean {
        val notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(
            principal.id,
            PageRequest.of(0, Integer.MAX_VALUE)
        ).content.filter { !it.isRead }

        notifications.forEach { notification ->
            notificationRepository.save(notification.copy(isRead = true))
        }
        return true
    }

    fun createNotification(user: User, type: NotificationType, title: String, message: String, actionUrl: String? = null) {
        val notification = Notification(
            user = user,
            type = type,
            title = title,
            message = message,
            actionUrl = actionUrl
        )
        notificationRepository.save(notification)
    }

    private fun Notification.toDto(): NotificationDto {
        return NotificationDto(
            id = this.id.toString(),
            type = this.type.name.lowercase(),
            title = this.title,
            message = this.message,
            isRead = this.isRead,
            actionUrl = this.actionUrl,
            time = this.createdAt
        )
    }
}
