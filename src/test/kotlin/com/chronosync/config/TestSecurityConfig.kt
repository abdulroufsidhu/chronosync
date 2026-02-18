package com.chronosync.config

import com.chronosync.service.*
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.mail.javamail.JavaMailSender

@TestConfiguration
class TestSecurityConfig {

    @Bean
    @Primary
    fun javaMailSender(): JavaMailSender {
        return Mockito.mock(JavaMailSender::class.java)
    }

    @Bean
    @Primary
    fun calendarService(): CalendarService {
        return Mockito.mock(CalendarService::class.java)
    }

    @Bean
    @Primary
    fun timezoneService(): TimezoneService {
        val mock = Mockito.mock(TimezoneService::class.java)
        // Configure mock to return UTC by default for IP detection
        Mockito.`when`(mock.detectTimezoneFromIp(Mockito.anyString())).thenReturn("UTC")
        return mock
    }
}
