package com.chronosync.controller

import com.chronosync.dto.auth.*
import com.chronosync.dto.common.ApiResponse
import com.chronosync.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication APIs for login and registration")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticate an existing user and receive a JWT token"
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Login successful",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid credentials",
            )
        ]
    )
    fun login(@RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        return try {
            val response = authService.login(request)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @PostMapping("/register")
    @Operation(
        summary = "Register new organization",
        description = "Create a new organization with an admin user. The role field must be one of: OWNER, MANAGER, MEMBER"
    )
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        return try {
            val response = authService.register(request)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @PostMapping("/forgot-password")
    @Operation(
        summary = "Request password reset",
        description = "Send a password reset email to the user. Always returns success to prevent email enumeration."
    )
    fun forgotPassword(@RequestBody request: ForgotPasswordRequest): ResponseEntity<ApiResponse<TokenResponse>> {
        authService.forgotPassword(request.email)
        return ResponseEntity.ok(ApiResponse(
            success = true,
            data = TokenResponse(message = "If an account exists with this email, you will receive a password reset link.")
        ))
    }

    @PostMapping("/reset-password")
    @Operation(
        summary = "Reset password",
        description = "Reset password using the token from the email"
    )
    fun resetPassword(@RequestBody request: ResetPasswordRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        return try {
            val response = authService.resetPassword(request.token, request.newPassword)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @PostMapping("/magic-link")
    @Operation(
        summary = "Request magic link",
        description = "Send a magic link email for passwordless authentication. Always returns success to prevent email enumeration."
    )
    fun requestMagicLink(@RequestBody request: MagicLinkRequest): ResponseEntity<ApiResponse<TokenResponse>> {
        authService.requestMagicLink(request.email)
        return ResponseEntity.ok(ApiResponse(
            success = true,
            data = TokenResponse(message = "If an account exists with this email, you will receive a sign-in link.")
        ))
    }

    @GetMapping("/magic-link/verify")
    @Operation(
        summary = "Verify magic link",
        description = "Verify magic link token and return JWT token for authentication"
    )
    fun verifyMagicLink(@RequestParam token: String): ResponseEntity<ApiResponse<AuthResponse>> {
        return try {
            val response = authService.verifyMagicLink(token)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }
}
