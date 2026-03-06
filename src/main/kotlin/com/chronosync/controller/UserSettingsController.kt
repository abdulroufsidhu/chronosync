package com.chronosync.controller

import com.chronosync.dto.common.ApiResponse
import com.chronosync.dto.settings.ChangePasswordRequest
import com.chronosync.dto.settings.UpdateSettingsRequest
import com.chronosync.dto.settings.UserSettingsResponse
import com.chronosync.security.CurrentUser
import com.chronosync.security.UserPrincipal
import com.chronosync.service.UserSettingsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/settings")
class UserSettingsController(
    private val userSettingsService: UserSettingsService
) {

    @GetMapping
    fun getSettings(
        @CurrentUser principal: UserPrincipal
    ): ResponseEntity<ApiResponse<UserSettingsResponse>> {
        return try {
            val response = userSettingsService.getSettings(principal)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @PutMapping
    fun updateSettings(
        @CurrentUser principal: UserPrincipal,
        @RequestBody request: UpdateSettingsRequest
    ): ResponseEntity<ApiResponse<UserSettingsResponse>> {
        return try {
            val response = userSettingsService.updateSettings(principal, request)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @PutMapping("/password")
    fun changePassword(
        @CurrentUser principal: UserPrincipal,
        @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            userSettingsService.changePassword(principal, request)
            ResponseEntity.ok(ApiResponse(success = true, message = "Password changed successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }
}
