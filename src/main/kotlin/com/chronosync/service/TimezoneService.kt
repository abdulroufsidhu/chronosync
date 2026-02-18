package com.chronosync.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class TimezoneService(
    @Value("\${app.timezone.ip-api.enabled:true}") private val ipApiEnabled: Boolean,
    @Value("\${app.timezone.ip-api.url:https://ipapi.co}") private val ipApiUrl: String
) {
    private val logger = LoggerFactory.getLogger(TimezoneService::class.java)

    fun detectTimezoneFromIp(clientIp: String?): String {
        if (!ipApiEnabled || clientIp == null) {
            return "UTC"
        }

        // Handle local/development IPs
        if (isLocalIp(clientIp)) {
            logger.info("Local IP detected ($clientIp), using UTC as default timezone")
            return "UTC"
        }

        return try {
            val url = "$ipApiUrl/$clientIp/timezone/"
            val response = URL(url).readText().trim()

            if (response.isNotBlank() && isValidTimezone(response)) {
                logger.info("Detected timezone '$response' from IP: $clientIp")
                response
            } else {
                logger.warn("Invalid timezone response from IP API: $response")
                "UTC"
            }
        } catch (e: Exception) {
            logger.error("Failed to detect timezone from IP: $clientIp", e)
            "UTC"
        }
    }

    fun toOrganizationTime(instant: Instant, timezone: String): ZonedDateTime {
        return instant.atZone(ZoneId.of(timezone))
    }

    fun getTimezoneAbbreviation(timezone: String, instant: Instant = Instant.now()): String {
        return try {
            val zone = ZoneId.of(timezone)
            val rules = zone.rules
            val offset = rules.getOffset(instant)
            offset.toString()
        } catch (e: Exception) {
            logger.warn("Failed to get timezone abbreviation for: $timezone", e)
            "UTC"
        }
    }

    fun isValidTimezone(timezone: String): Boolean {
        return try {
            ZoneId.of(timezone)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isLocalIp(ip: String): Boolean {
        return ip == "127.0.0.1" ||
               ip == "0:0:0:0:0:0:0:1" ||
               ip == "::1" ||
               ip.startsWith("192.168.") ||
               ip.startsWith("10.") ||
               (ip.startsWith("172.") && ip.split(".")[1].toInt() in 16..31)
    }
}
