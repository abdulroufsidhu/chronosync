package com.chronosync.service

import com.chronosync.dto.notification.NotificationDto
import com.chronosync.dto.notification.NotificationListResponse
import com.chronosync.entity.*
import com.chronosync.repository.NotificationLogRepository
import com.chronosync.repository.NotificationRepository
import com.chronosync.repository.UserRepository
import com.chronosync.security.UserPrincipal
import org.slf4j.LoggerFactory
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

        schedule.assignedUser?.email?.let { email ->
            sendNotification(schedule, organization, RecipientType.STAFF, email, notificationType)
        }

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

    @Transactional(readOnly = true)
    fun getNotifications(principal: UserPrincipal, page: Int = 0, size: Int = 20): NotificationListResponse {
        val pageable = PageRequest.of(page, size)
        val notificationsPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(principal.id, pageable)
        val unreadCount = notificationRepository.countByUserIdAndIsReadFalse(principal.id)

        return NotificationListResponse(
            notifications = notificationsPage.content.map { it.toDto() },
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

        notificationRepository.save(notification.copy(isRead = true))
        return true
    }

    @Transactional
    fun markAllAsRead(principal: UserPrincipal): Boolean {
        notificationRepository.markAllAsReadByUserId(principal.id)
        return true
    }

    fun createNotification(user: User, type: NotificationType, title: String, message: String, actionUrl: String? = null) {
        notificationRepository.save(Notification(user = user, type = type, title = title, message = message, actionUrl = actionUrl))
    }

    private fun Notification.toDto() = NotificationDto(
        id = id.toString(),
        type = type.name.lowercase(),
        title = title,
        message = message,
        isRead = isRead,
        actionUrl = actionUrl,
        time = createdAt
    )
}
