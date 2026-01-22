package com.chronosync.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtUtil(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expiration: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateToken(userId: UUID, email: String, organizationId: UUID, role: String): String {
        val claims = mapOf(
            "userId" to userId.toString(),
            "email" to email,
            "organizationId" to organizationId.toString(),
            "role" to role
        )
        return Jwts.builder()
            .claims(claims)
            .subject(userId.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Claims? {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: Exception) {
            null
        }
    }

    fun getUserId(claims: Claims): UUID = UUID.fromString(claims["userId"] as String)
    fun getEmail(claims: Claims): String = claims["email"] as String
    fun getOrganizationId(claims: Claims): UUID = UUID.fromString(claims["organizationId"] as String)
    fun getRole(claims: Claims): String = claims["role"] as String
}
