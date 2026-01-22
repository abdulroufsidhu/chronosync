package com.chronosync.controller

import com.chronosync.dto.auth.LoginRequest
import com.chronosync.dto.auth.RegisterRequest
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
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid credentials",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            )
        ]
    )
    fun login(@RequestBody request: LoginRequest): ResponseEntity<ApiResponse<Any>> {
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
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Registration successful",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid input or email already registered",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            )
        ]
    )
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<ApiResponse<Any>> {
        return try {
            val response = authService.register(request)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }
}
