package com.chronosync.service

import com.chronosync.entity.Organization
import com.chronosync.entity.Schedule
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class CalendarService {
    private val logger = LoggerFactory.getLogger(CalendarService::class.java)

    fun generateEvent(
        schedule: Schedule,
        organization: Organization,
        method: CalendarMethod = CalendarMethod.REQUEST,
        sequence: Int = 0
    ): String {
        val uid = schedule.id.toString()
        val zoneId = ZoneId.of(organization.timezone)

        // Convert Instants to LocalDateTime in organization's timezone
        val startLocal = schedule.startDateTime.atZone(zoneId).toLocalDateTime()
        val endLocal = schedule.endDateTime.atZone(zoneId).toLocalDateTime()

        val ics = buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//ChronoSync//Schedule//EN")
            appendLine("CALSCALE:GREGORIAN")
            appendLine("METHOD:$method")

            // VTIMEZONE block for organization's timezone
            append(getVTimezoneBlock(organization.timezone))

            appendLine("BEGIN:VEVENT")
            appendLine("UID:$uid")
            appendLine("SEQUENCE:$sequence")
            appendLine("DTSTAMP:${formatUtcDateTime(Instant.now())}")
            appendLine("DTSTART;TZID=${organization.timezone}:${formatLocalDateTime(startLocal)}")
            appendLine("DTEND;TZID=${organization.timezone}:${formatLocalDateTime(endLocal)}")
            appendLine("SUMMARY:${escapeIcsText(schedule.title)}")

            // Build description
            val description = buildString {
                schedule.clientName?.let { appendLine("Client: $it") }
                schedule.clientPhone?.let { appendLine("Phone: $it") }
                schedule.clientEmail?.let { appendLine("Email: $it") }
                schedule.notes?.let { appendLine("Notes: $it") }
            }.trim()

            if (description.isNotEmpty()) {
                appendLine("DESCRIPTION:${escapeIcsText(description)}")
            }

            schedule.clientAddress?.let {
                appendLine("LOCATION:${escapeIcsText(it)}")
            }

            // Organizer (assigned staff)
            schedule.assignedUser?.let { user ->
                val organizerName = user.getFullNameOrEmail()
                appendLine("ORGANIZER;CN=${escapeIcsText(organizerName)}:mailto:${user.email}")
            }

            // Attendee (client)
            schedule.clientEmail?.let { email ->
                val attendeeName = schedule.clientName ?: "Client"
                appendLine("ATTENDEE;ROLE=REQ-PARTICIPANT;CN=${escapeIcsText(attendeeName)}:mailto:$email")
            }

            if (method == CalendarMethod.CANCEL) {
                appendLine("STATUS:CANCELLED")
            } else {
                appendLine("STATUS:CONFIRMED")
            }

            appendLine("END:VEVENT")
            appendLine("END:VCALENDAR")
        }

        logger.debug("Generated ICS for schedule ${schedule.id}")
        return ics
    }

    private fun getVTimezoneBlock(timezone: String): String {
        return try {
            val zone = ZoneId.of(timezone)
            val rules = zone.rules
            val standardOffset = rules.getStandardOffset(Instant.now())

            buildString {
                appendLine("BEGIN:VTIMEZONE")
                appendLine("TZID:$timezone")

                // Standard time
                appendLine("BEGIN:STANDARD")
                appendLine("DTSTART:19700101T000000")
                appendLine("TZOFFSETFROM:${formatOffset(standardOffset)}")
                appendLine("TZOFFSETTO:${formatOffset(standardOffset)}")
                appendLine("END:STANDARD")

                appendLine("END:VTIMEZONE")
            }
        } catch (e: Exception) {
            logger.warn("Failed to generate VTIMEZONE for $timezone, using UTC", e)
            buildString {
                appendLine("BEGIN:VTIMEZONE")
                appendLine("TZID:UTC")
                appendLine("BEGIN:STANDARD")
                appendLine("DTSTART:19700101T000000")
                appendLine("TZOFFSETFROM:+0000")
                appendLine("TZOFFSETTO:+0000")
                appendLine("END:STANDARD")
                appendLine("END:VTIMEZONE")
            }
        }
    }

    private fun formatOffset(offset: ZoneOffset): String {
        val hours = offset.totalSeconds / 3600
        val minutes = kotlin.math.abs(offset.totalSeconds % 3600) / 60
        return String.format("%+03d%02d", hours, minutes)
    }

    private fun formatLocalDateTime(dt: LocalDateTime): String {
        return dt.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
    }

    private fun formatUtcDateTime(instant: Instant): String {
        return instant.atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
    }

    private fun escapeIcsText(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    fun generateGoogleCalendarLink(schedule: Schedule, organization: Organization): String {
        val zoneId = ZoneId.of(organization.timezone)
        val start = schedule.startDateTime.atZone(zoneId)
        val end = schedule.endDateTime.atZone(zoneId)
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

        val params = buildString {
            append("action=TEMPLATE")
            append("&text=${java.net.URLEncoder.encode(schedule.title, "UTF-8")}")
            append("&dates=${start.format(formatter)}/${end.format(formatter)}")
            schedule.clientAddress?.let {
                append("&location=${java.net.URLEncoder.encode(it, "UTF-8")}")
            }
            val details = buildString {
                schedule.clientName?.let { append("Client: $it\\n") }
                schedule.notes?.let { append("Notes: $it") }
            }
            if (details.isNotEmpty()) {
                append("&details=${java.net.URLEncoder.encode(details, "UTF-8")}")
            }
        }

        return "https://calendar.google.com/calendar/render?$params"
    }

    fun generateOutlookCalendarLink(schedule: Schedule, organization: Organization): String {
        val zoneId = ZoneId.of(organization.timezone)
        val start = schedule.startDateTime.atZone(zoneId)
        val end = schedule.endDateTime.atZone(zoneId)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        val params = buildString {
            append("subject=${java.net.URLEncoder.encode(schedule.title, "UTF-8")}")
            append("&startdt=${start.format(formatter)}")
            append("&enddt=${end.format(formatter)}")
            schedule.clientAddress?.let {
                append("&location=${java.net.URLEncoder.encode(it, "UTF-8")}")
            }
        }

        return "https://outlook.live.com/calendar/0/deeplink/compose?$params"
    }

    fun generateYahooCalendarLink(schedule: Schedule, organization: Organization): String {
        val zoneId = ZoneId.of(organization.timezone)
        val start = schedule.startDateTime.atZone(zoneId)
        val end = schedule.endDateTime.atZone(zoneId)
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

        val params = buildString {
            append("v=60")
            append("&title=${java.net.URLEncoder.encode(schedule.title, "UTF-8")}")
            append("&st=${start.format(formatter)}")
            append("&et=${end.format(formatter)}")
            schedule.clientAddress?.let {
                append("&in_loc=${java.net.URLEncoder.encode(it, "UTF-8")}")
            }
        }

        return "https://calendar.yahoo.com/?$params"
    }
}

enum class CalendarMethod {
    REQUEST, CANCEL
}
