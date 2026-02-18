package com.chronosync.service

import com.chronosync.entity.*
import com.chronosync.repository.NotificationLogRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class NotificationService(
    private val emailService: EmailService,
    private val notificationLogRepository: NotificationLogRepository
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
}
