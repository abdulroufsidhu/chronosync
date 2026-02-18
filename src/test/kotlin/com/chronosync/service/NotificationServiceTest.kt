package com.chronosync.service

import com.chronosync.entity.*
import com.chronosync.repository.NotificationLogRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import java.time.Instant
import java.time.temporal.ChronoUnit

class NotificationServiceTest {

    private lateinit var emailService: EmailService
    private lateinit var notificationLogRepository: NotificationLogRepository
    private lateinit var notificationService: NotificationService

    @BeforeEach
    fun setUp() {
        emailService = mock(EmailService::class.java)
        notificationLogRepository = mock(NotificationLogRepository::class.java)
        notificationService = NotificationService(emailService, notificationLogRepository)
    }

    @Test
    fun `sendScheduleNotifications sends to staff and client`() {
        val organization = Organization(
            name = "Test Org",
            plan = Plan.FREE,
            timezone = "UTC"
        )

        val staffUser = User(
            email = "staff@example.com",
            password = "pass",
            firstName = "Jane",
            lastName = "Doe"
        )

        val schedule = Schedule(
            title = "Test Appointment",
            startDateTime = Instant.now(),
            endDateTime = Instant.now().plus(1, ChronoUnit.HOURS),
            organization = organization,
            assignedUser = staffUser,
            clientEmail = "client@example.com"
        )

        notificationService.sendScheduleNotifications(
            schedule,
            organization,
            NotificationType.SCHEDULE_CREATED
        )

        // Verify emails were sent
        verify(emailService).sendScheduleNotification(
            schedule,
            organization,
            RecipientType.STAFF,
            NotificationType.SCHEDULE_CREATED
        )

        verify(emailService).sendScheduleNotification(
            schedule,
            organization,
            RecipientType.CLIENT,
            NotificationType.SCHEDULE_CREATED
        )

        // Verify logs were created
        verify(notificationLogRepository, times(2)).save(any())
    }

    @Test
    fun `sendScheduleNotifications only sends to staff when no client email`() {
        val organization = Organization(
            name = "Test Org",
            plan = Plan.FREE,
            timezone = "UTC"
        )

        val staffUser = User(
            email = "staff@example.com",
            password = "pass"
        )

        val schedule = Schedule(
            title = "Test",
            startDateTime = Instant.now(),
            endDateTime = Instant.now().plus(1, ChronoUnit.HOURS),
            organization = organization,
            assignedUser = staffUser,
            clientEmail = null // No client email
        )

        notificationService.sendScheduleNotifications(
            schedule,
            organization,
            NotificationType.SCHEDULE_CREATED
        )

        // Only staff email should be sent
        verify(emailService).sendScheduleNotification(
            schedule,
            organization,
            RecipientType.STAFF,
            NotificationType.SCHEDULE_CREATED
        )

        verify(notificationLogRepository, times(1)).save(any())
    }

    @Test
    fun `sendScheduleNotifications only sends to client when no assigned user`() {
        val organization = Organization(
            name = "Test Org",
            plan = Plan.FREE,
            timezone = "UTC"
        )

        val schedule = Schedule(
            title = "Test",
            startDateTime = Instant.now(),
            endDateTime = Instant.now().plus(1, ChronoUnit.HOURS),
            organization = organization,
            assignedUser = null, // No assigned user
            clientEmail = "client@example.com"
        )

        notificationService.sendScheduleNotifications(
            schedule,
            organization,
            NotificationType.SCHEDULE_CREATED
        )

        // Only client email should be sent
        verify(emailService).sendScheduleNotification(
            schedule,
            organization,
            RecipientType.CLIENT,
            NotificationType.SCHEDULE_CREATED
        )

        verify(notificationLogRepository, times(1)).save(any())
    }

    @Test
    fun `sendScheduleNotifications logs success`() {
        val organization = Organization(
            name = "Test Org",
            plan = Plan.FREE,
            timezone = "UTC"
        )

        val staffUser = User(
            email = "staff@example.com",
            password = "pass"
        )

        val schedule = Schedule(
            title = "Test",
            startDateTime = Instant.now(),
            endDateTime = Instant.now().plus(1, ChronoUnit.HOURS),
            organization = organization,
            assignedUser = staffUser
        )

        notificationService.sendScheduleNotifications(
            schedule,
            organization,
            NotificationType.SCHEDULE_UPDATED
        )

        // Verify notification was logged - verify save was called at least once
        verify(notificationLogRepository, atLeastOnce()).save(any())
    }
}
