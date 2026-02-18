package com.chronosync.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId

class TimezoneServiceTest {

    private val timezoneService = TimezoneService(
        ipApiEnabled = true,
        ipApiUrl = "https://ipapi.co"
    )

    @Test
    fun `detectTimezoneFromIp returns UTC for null IP`() {
        val result = timezoneService.detectTimezoneFromIp(null)
        assertEquals("UTC", result)
    }

    @Test
    fun `detectTimezoneFromIp returns UTC for localhost`() {
        val result = timezoneService.detectTimezoneFromIp("127.0.0.1")
        assertEquals("UTC", result)
    }

    @Test
    fun `detectTimezoneFromIp returns UTC for private IP`() {
        val result = timezoneService.detectTimezoneFromIp("192.168.1.1")
        assertEquals("UTC", result)
    }

    @Test
    fun `isValidTimezone returns true for valid timezone`() {
        assertTrue(timezoneService.isValidTimezone("America/New_York"))
        assertTrue(timezoneService.isValidTimezone("Europe/London"))
        assertTrue(timezoneService.isValidTimezone("UTC"))
    }

    @Test
    fun `isValidTimezone returns false for invalid timezone`() {
        assertFalse(timezoneService.isValidTimezone("Invalid/Timezone"))
        assertFalse(timezoneService.isValidTimezone(""))
        assertFalse(timezoneService.isValidTimezone("America"))
    }

    @Test
    fun `toOrganizationTime converts instant to zoned datetime`() {
        val instant = Instant.parse("2024-01-15T14:00:00Z")
        val result = timezoneService.toOrganizationTime(instant, "America/New_York")

        assertEquals(ZoneId.of("America/New_York"), result.zone)
        assertEquals(2024, result.year)
        assertEquals(1, result.monthValue)
    }

    @Test
    fun `getTimezoneAbbreviation returns offset string`() {
        val instant = Instant.parse("2024-01-15T14:00:00Z")
        val result = timezoneService.getTimezoneAbbreviation("America/New_York", instant)

        // EST is UTC-5, so should be "-05:00"
        assertTrue(result.contains("-05:00") || result.contains("-04:00"))
    }

    @Test
    fun `getTimezoneAbbreviation returns UTC for invalid timezone`() {
        val instant = Instant.now()
        val result = timezoneService.getTimezoneAbbreviation("Invalid", instant)

        assertEquals("UTC", result)
    }
}
