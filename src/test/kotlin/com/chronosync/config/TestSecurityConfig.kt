package com.chronosync.config

import com.chronosync.security.JwtAuthenticationFilter
import com.chronosync.security.JwtUtil
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestSecurityConfig {

    @Bean
    @Primary
    fun testJwtUtil(): JwtUtil {
        return JwtUtil(
            secret = "chronosync-jwt-secret-key-for-authentication-must-be-at-least-256-bits-long",
            expiration = 86400000L
        )
    }
}
