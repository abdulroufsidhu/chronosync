package com.chronosync.service

import com.chronosync.entity.Organization
import com.chronosync.entity.Plan
import com.chronosync.entity.Schedule
import com.chronosync.entity.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class CalendarServiceTest {

    private val calendarService = CalendarService()

    @Test
    fun `generateEvent creates valid ICS content`() {
        val organization = Organization(
            name = "Test Org",
            plan = Plan.FREE,
            timezone = "America/New_York"
        )

        val schedule = Schedule(
            title = "Test Appointment",
            startDateTime = Instant.parse("2024-01-15T14:00:00Z"),
            endDateTime = Instant.parse("2024-01-15T15:00:00Z"),
            organization = organization,
            clientName = "John Doe",
            clientEmail = "john@example.com",
            clientAddress = "123 Main St"
        )

        val ics = calendarService.generateEvent(schedule, organization)

        assertTrue(ics.contains("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("END:VCALENDAR"))
        assertTrue(ics.contains("BEGIN:VEVENT"))
        assertTrue(ics.contains("END:VEVENT"))
        assertTrue(ics.contains("SUMMARY:Test Appointment"))
        assertTrue(ics.contains(schedule.id.toString())) // UID
    }

    @Test
    fun `generateEvent includes timezone information`() {
        val organization = Organization(
            name = "Test Org",
            plan = Plan.FREE,
            timezone = "America/New_York"
        )

        val schedule = Schedule(
            title = "Test",
            startDateTime = Instant.now(),
            endDateTime = Instant.now().plus(1, ChronoUnit.HOURS),
            organization = organization
        )

        val ics = calendarService.generateEvent(schedule, organization)

        assertTrue(ics.contains("TZID:America/New_York"))
        assertTrue(ics.contains("BEGIN:VTIMEZONE"))
        assertTrue(ics.contains("END:VTIMEZONE"))
    }

    @Test
    fun `generateEvent handles cancellation`() {
        val organization = Organization(
            name = "Test Org",
            plan = Plan.FREE,
            timezone = "UTC"
        )

        val schedule = Schedule(
            title = "Cancelled Appointment",
            startDateTime = Instant.now(),
            endDateTime = Instant.now().plus(1, ChronoUnit.HOURS),
            organization = organization
        )

        val ics = calendarService.generateEvent(schedule, organization, method = CalendarMethod.CANCEL)

        assertTrue(ics.contains("METHOD:CANCEL"))
        assertTrue(ics.contains("STATUS:CANCELLED"))
    }

    @Test
    fun `generateGoogleCalendarLink creates valid URL`() {
        val organization = Organization(
            name = "Test Org",
            plan = Plan.FREE,
            timezone = "America/New_York"
        )

        val schedule = Schedule(
            title = "Test Appointment",
            startDateTime = Instant.parse("2024-01-15T14:00:00Z"),
            endDateTime = Instant.parse("2024-01-15T15:00:00Z"),
            organization = organization,
            clientAddress = "123 Main St"
        )

        val url = calendarService.generateGoogleCalendarLink(schedule, organization)

        assertTrue(url.startsWith("https://calendar.google.com/calendar/render?"))
        assertTrue(url.contains("action=TEMPLATE"))
        assertTrue(url.contains("Test+Appointment"))
        assertTrue(url.contains("123+Main+St"))
    }

    @Test
    fun `generateOutlookCalendarLink creates valid URL`() {
        val organization = Organization(
            name = "Test Org",
            plan = Plan.FREE,
            timezone = "UTC"
        )

        val schedule = Schedule(
            title = "Test Appointment",
            startDateTime = Instant.parse("2024-01-15T14:00:00Z"),
            endDateTime = Instant.parse("2024-01-15T15:00:00Z"),
            organization = organization
        )

        val url = calendarService.generateOutlookCalendarLink(schedule, organization)

        assertTrue(url.startsWith("https://outlook.live.com/calendar/0/deeplink/compose?"))
        assertTrue(url.contains("subject="))
    }

    @Test
    fun `generateYahooCalendarLink creates valid URL`() {
        val organization = Organization(
            name = "Test Org",
            plan = Plan.FREE,
            timezone = "UTC"
        )

        val schedule = Schedule(
            title = "Test Appointment",
            startDateTime = Instant.parse("2024-01-15T14:00:00Z"),
            endDateTime = Instant.parse("2024-01-15T15:00:00Z"),
            organization = organization
        )

        val url = calendarService.generateYahooCalendarLink(schedule, organization)

        assertTrue(url.startsWith("https://calendar.yahoo.com/?"))
        assertTrue(url.contains("v=60"))
    }

    @Test
    fun `generateEvent includes assigned user as organizer`() {
        val organization = Organization(
            name = "Test Org",
            plan = Plan.FREE,
            timezone = "UTC"
        )

        val user = User(
            email = "staff@example.com",
            password = "pass",
            firstName = "Jane",
            lastName = "Doe"
        )

        val schedule = Schedule(
            title = "Test",
            startDateTime = Instant.now(),
            endDateTime = Instant.now().plus(1, ChronoUnit.HOURS),
            organization = organization,
            assignedUser = user
        )

        val ics = calendarService.generateEvent(schedule, organization)

        assertTrue(ics.contains("ORGANIZER"))
        assertTrue(ics.contains("staff@example.com"))
    }

    @Test
    fun `generateEvent includes client as attendee`() {
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
            clientEmail = "client@example.com",
            clientName = "John Doe"
        )

        val ics = calendarService.generateEvent(schedule, organization)

        assertTrue(ics.contains("ATTENDEE"))
        assertTrue(ics.contains("client@example.com"))
        assertTrue(ics.contains("John Doe"))
    }
}
