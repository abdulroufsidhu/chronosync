package com.chronosync.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
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

    private fun sendHtmlEmail(to: String, subject: String, htmlContent: String) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")

        helper.setFrom(emailFrom)
        helper.setTo(to)
        helper.setSubject(subject)
        helper.setText(htmlContent, true)

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
}
