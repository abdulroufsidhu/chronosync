package com.chronosync.service

import com.chronosync.entity.NotificationType
import com.chronosync.entity.Organization
import com.chronosync.entity.RecipientType
import com.chronosync.entity.Schedule
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import jakarta.mail.internet.MimeMessage

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val calendarService: CalendarService,
    private val timezoneService: TimezoneService,
    @Value("\${app.frontend-url:http://localhost:3000}") private val frontendUrl: String,
    @Value("\${app.email-from:noreply@chronosync.com}") private val emailFrom: String
) {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    @Async
    fun sendPasswordResetEmail(email: String, token: String) {
        try {
            val resetUrl = "$frontendUrl/reset-password?token=$token"
            val subject = "Reset Your Password - ChronoSync"
            val content = buildPasswordResetEmail(resetUrl)

            sendHtmlEmail(email, subject, content)
            logger.info("Password reset email sent to: $email")
        } catch (e: Exception) {
            logger.error("Failed to send password reset email to: $email", e)
        }
    }

    @Async
    fun sendMagicLinkEmail(email: String, token: String) {
        try {
            val magicLinkUrl = "$frontendUrl/api/auth/magic-link/verify?token=$token"
            val subject = "Sign In to ChronoSync"
            val content = buildMagicLinkEmail(magicLinkUrl)

            sendHtmlEmail(email, subject, content)
            logger.info("Magic link email sent to: $email")
        } catch (e: Exception) {
            logger.error("Failed to send magic link email to: $email", e)
        }
    }

    @Async
    fun sendEmailVerification(email: String, token: String) {
        try {
            val verifyUrl = "$frontendUrl/verify-email?token=$token"
            val subject = "Verify Your Email - ChronoSync"
            val content = buildEmailVerificationEmail(verifyUrl)

            sendHtmlEmail(email, subject, content)
            logger.info("Email verification sent to: $email")
        } catch (e: Exception) {
            logger.error("Failed to send email verification to: $email", e)
        }
    }

    @Async
    fun sendInvitationEmail(email: String, token: String, organizationName: String, inviterName: String) {
        try {
            val invitationUrl = "$frontendUrl/accept-invitation?token=$token"
            val subject = "You're invited to join $organizationName on ChronoSync"
            val content = buildInvitationEmail(invitationUrl, organizationName, inviterName)

            sendHtmlEmail(email, subject, content)
            logger.info("Invitation email sent to: $email")
        } catch (e: Exception) {
            logger.error("Failed to send invitation email to: $email", e)
        }
    }

    @Async
    fun sendScheduleNotification(
        schedule: Schedule,
        organization: Organization,
        recipientType: RecipientType,
        notificationType: NotificationType
    ) {
        try {
            val (recipientEmail, recipientName) = when (recipientType) {
                RecipientType.STAFF -> schedule.assignedUser?.email to schedule.assignedUser?.getFullNameOrEmail()
                RecipientType.CLIENT -> schedule.clientEmail to schedule.clientName
            }

            if (recipientEmail.isNullOrBlank()) {
                logger.info("No email for recipient type $recipientType, skipping notification")
                return
            }

            val icsContent = calendarService.generateEvent(
                schedule,
                organization,
                method = if (notificationType == NotificationType.SCHEDULE_CANCELLED) CalendarMethod.CANCEL else CalendarMethod.REQUEST
            )

            val (subject, htmlContent) = buildScheduleEmail(
                schedule,
                organization,
                recipientType,
                notificationType,
                recipientName
            )

            sendEmail(
                to = recipientEmail,
                subject = subject,
                htmlContent = htmlContent,
                attachmentName = "appointment.ics",
                attachmentContent = icsContent.toByteArray(),
                attachmentType = "text/calendar"
            )

            logger.info("Schedule notification sent to: $recipientEmail (type: $notificationType)")
        } catch (e: Exception) {
            logger.error("Failed to send schedule notification for schedule ${schedule.id}", e)
            throw e // Re-throw so NotificationService can log the failure
        }
    }

    private fun buildScheduleEmail(
        schedule: Schedule,
        organization: Organization,
        recipientType: RecipientType,
        notificationType: NotificationType,
        recipientName: String?
    ): Pair<String, String> {
        val zoneId = ZoneId.of(organization.timezone)
        val startZoned = schedule.startDateTime.atZone(zoneId)
        val endZoned = schedule.endDateTime.atZone(zoneId)

        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

        val subject = when (notificationType) {
            NotificationType.SCHEDULE_CREATED -> "Appointment Confirmed - ${schedule.title}"
            NotificationType.SCHEDULE_UPDATED -> "Appointment Updated - ${schedule.title}"
            NotificationType.SCHEDULE_CANCELLED -> "Appointment Cancelled - ${schedule.title}"
            else -> "Notification - ${schedule.title}"
        }

        val htmlContent = if (recipientType == RecipientType.STAFF) {
            buildStaffScheduleEmail(
                schedule,
                organization,
                notificationType,
                recipientName,
                startZoned.format(dateFormatter),
                startZoned.format(timeFormatter),
                endZoned.format(timeFormatter),
                organization.timezone
            )
        } else {
            buildClientScheduleEmail(
                schedule,
                organization,
                notificationType,
                recipientName,
                startZoned.format(dateFormatter),
                startZoned.format(timeFormatter),
                endZoned.format(timeFormatter),
                organization.timezone
            )
        }

        return subject to htmlContent
    }

    private fun buildStaffScheduleEmail(
        schedule: Schedule,
        organization: Organization,
        notificationType: NotificationType,
        staffName: String?,
        date: String,
        startTime: String,
        endTime: String,
        timezone: String
    ): String {
        val title = when (notificationType) {
            NotificationType.SCHEDULE_CREATED -> "New Appointment Scheduled"
            NotificationType.SCHEDULE_UPDATED -> "Appointment Updated"
            NotificationType.SCHEDULE_CANCELLED -> "Appointment Cancelled"
            else -> "Notification"
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>$title</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">$title</h2>
                    <p>Hi ${staffName ?: "there"},</p>
                    
                    ${when (notificationType) {
                        NotificationType.SCHEDULE_CREATED -> "<p>A new appointment has been scheduled for you.</p>"
                        NotificationType.SCHEDULE_UPDATED -> "<p>An appointment has been updated.</p>"
                        NotificationType.SCHEDULE_CANCELLED -> "<p>An appointment has been cancelled.</p>"
                        else -> "<p>You have a new notification.</p>"
                    }}
                    
                    <div style="background: #f5f5f5; padding: 20px; margin: 20px 0; border-radius: 5px;">
                        <h3 style="margin-top: 0; color: #2c3e50;">${schedule.title}</h3>
                        <p><strong>Date:</strong> $date</p>
                        <p><strong>Time:</strong> $startTime - $endTime ($timezone)</p>
                        
                        ${schedule.clientName?.let { "<p><strong>Client:</strong> $it</p>" } ?: ""}
                        ${schedule.clientPhone?.let { "<p><strong>Phone:</strong> $it</p>" } ?: ""}
                        ${schedule.clientEmail?.let { "<p><strong>Email:</strong> $it</p>" } ?: ""}
                        ${schedule.clientAddress?.let { "<p><strong>Location:</strong> $it</p>" } ?: ""}
                        ${schedule.notes?.let { "<p><strong>Notes:</strong> $it</p>" } ?: ""}
                    </div>
                    
                    <p>We've attached a calendar file (.ics) that you can add to your calendar.</p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="$frontendUrl/schedules/${schedule.id}" 
                           style="background-color: #3498db; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;">
                            View in ChronoSync
                        </a>
                    </div>
                    
                    <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                    <p style="color: #999; font-size: 12px;">ChronoSync - ${organization.name}</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildClientScheduleEmail(
        schedule: Schedule,
        organization: Organization,
        notificationType: NotificationType,
        clientName: String?,
        date: String,
        startTime: String,
        endTime: String,
        timezone: String
    ): String {
        val title = when (notificationType) {
            NotificationType.SCHEDULE_CREATED -> "Your Appointment is Confirmed"
            NotificationType.SCHEDULE_UPDATED -> "Your Appointment has been Updated"
            NotificationType.SCHEDULE_CANCELLED -> "Your Appointment has been Cancelled"
            else -> "Notification"
        }

        val greeting = clientName?.let { "Hi $it," } ?: "Hello,"

        val googleLink = calendarService.generateGoogleCalendarLink(schedule, organization)
        val outlookLink = calendarService.generateOutlookCalendarLink(schedule, organization)
        val yahooLink = calendarService.generateYahooCalendarLink(schedule, organization)

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>$title</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">${organization.name}</h2>
                    <h3>$title</h3>
                    <p>$greeting</p>
                    
                    ${when (notificationType) {
                        NotificationType.SCHEDULE_CREATED -> "<p>Your appointment has been scheduled with us. Here are the details:</p>"
                        NotificationType.SCHEDULE_UPDATED -> "<p>Your appointment details have been updated. Here are the new details:</p>"
                        NotificationType.SCHEDULE_CANCELLED -> "<p>Your appointment has been cancelled. Here were the details:</p>"
                        else -> "<p>You have a new notification. Here are the details:</p>"
                    }}
                    
                    <div style="background: #f5f5f5; padding: 20px; margin: 20px 0; border-radius: 5px;">
                        <h3 style="margin-top: 0; color: #2c3e50;">${schedule.title}</h3>
                        <p><strong>Date:</strong> $date</p>
                        <p><strong>Time:</strong> $startTime - $endTime ($timezone)</p>
                        ${schedule.clientAddress?.let { "<p><strong>Location:</strong> $it</p>" } ?: ""}
                        ${schedule.notes?.let { "<p><strong>Notes:</strong> $it</p>" } ?: ""}
                    </div>
                    
                    ${if (notificationType != NotificationType.SCHEDULE_CANCELLED) """
                    <div style="margin: 20px 0;">
                        <p><strong>Add to your calendar:</strong></p>
                        <div style="text-align: center; margin: 15px 0;">
                            <a href="$googleLink" 
                               style="display: inline-block; margin: 5px; padding: 10px 20px; background: #4285f4; color: white; text-decoration: none; border-radius: 5px;">
                                Google Calendar
                            </a>
                            <a href="$outlookLink" 
                               style="display: inline-block; margin: 5px; padding: 10px 20px; background: #0078d4; color: white; text-decoration: none; border-radius: 5px;">
                                Outlook
                            </a>
                            <a href="$yahooLink" 
                               style="display: inline-block; margin: 5px; padding: 10px 20px; background: #6001d2; color: white; text-decoration: none; border-radius: 5px;">
                                Yahoo
                            </a>
                        </div>
                        <p style="font-size: 14px; color: #666;">
                            You can also open the attached .ics file to add this to any calendar app.
                        </p>
                    </div>
                    """ else ""}
                    
                    <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                    <p style="color: #999; font-size: 12px;">
                        This email was sent by ChronoSync on behalf of ${organization.name}.<br>
                        If you have any questions, please contact us at ${organization.name}.
                    </p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun sendHtmlEmail(to: String, subject: String, htmlContent: String) {
        sendEmail(to, subject, htmlContent)
    }

    private fun sendEmail(
        to: String,
        subject: String,
        htmlContent: String,
        attachmentName: String? = null,
        attachmentContent: ByteArray? = null,
        attachmentType: String? = null
    ) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")

        helper.setFrom(emailFrom)
        helper.setTo(to)
        helper.setSubject(subject)
        helper.setText(htmlContent, true)
        if (attachmentName != null && attachmentContent != null && attachmentType != null) {
            helper.addAttachment(attachmentName, ByteArrayResource(attachmentContent), attachmentType)
        }

        mailSender.send(message)
    }

    private fun buildPasswordResetEmail(resetUrl: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Reset Your Password</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">Reset Your Password</h2>
                    <p>Hello,</p>
                    <p>We received a request to reset your password. Click the button below to reset it:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="$resetUrl" style="background-color: #3498db; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;">Reset Password</a>
                    </div>
                    <p>Or copy and paste this link into your browser:</p>
                    <p style="word-break: break-all; color: #3498db;">$resetUrl</p>
                    <p>This link will expire in 1 hour for security reasons.</p>
                    <p>If you didn't request this password reset, you can safely ignore this email.</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                    <p style="color: #999; font-size: 12px;">ChronoSync - Your scheduling solution</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildMagicLinkEmail(magicLinkUrl: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Sign In to ChronoSync</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">Sign In to ChronoSync</h2>
                    <p>Hello,</p>
                    <p>Click the button below to sign in to your account:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="$magicLinkUrl" style="background-color: #27ae60; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;">Sign In</a>
                    </div>
                    <p>Or copy and paste this link into your browser:</p>
                    <p style="word-break: break-all; color: #27ae60;">$magicLinkUrl</p>
                    <p>This link will expire in 15 minutes for security reasons.</p>
                    <p>If you didn't request this sign-in link, you can safely ignore this email.</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                    <p style="color: #999; font-size: 12px;">ChronoSync - Your scheduling solution</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildEmailVerificationEmail(verifyUrl: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Verify Your Email</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">Verify Your Email Address</h2>
                    <p>Hello,</p>
                    <p>Thank you for signing up! Please verify your email address by clicking the button below:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="$verifyUrl" style="background-color: #9b59b6; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;">Verify Email</a>
                    </div>
                    <p>Or copy and paste this link into your browser:</p>
                    <p style="word-break: break-all; color: #9b59b6;">$verifyUrl</p>
                    <p>This link will expire in 24 hours.</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                    <p style="color: #999; font-size: 12px;">ChronoSync - Your scheduling solution</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildInvitationEmail(invitationUrl: String, organizationName: String, inviterName: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>You're Invited</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">You're Invited to Join $organizationName</h2>
                    <p>Hello,</p>
                    <p><strong>$inviterName</strong> has invited you to join their organization on ChronoSync.</p>
                    <p>ChronoSync is a powerful scheduling and team management platform. Click the button below to accept the invitation and create your account:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="$invitationUrl" style="background-color: #3498db; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;">Accept Invitation</a>
                    </div>
                    <p>Or copy and paste this link into your browser:</p>
                    <p style="word-break: break-all; color: #3498db;">$invitationUrl</p>
                    <p>This invitation will expire in 7 days.</p>
                    <p>If you weren't expecting this invitation, you can safely ignore this email.</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                    <p style="color: #999; font-size: 12px;">ChronoSync - Your scheduling solution</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
