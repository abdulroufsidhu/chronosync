package com.chronosync.controller

import com.chronosync.dto.common.ApiResponse
import com.chronosync.dto.profile.UpdateProfileRequest
import com.chronosync.security.CurrentUser
import com.chronosync.security.UserPrincipal
import com.chronosync.service.ProfileService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/profile")
class ProfileController(
    private val profileService: ProfileService
) {

    @GetMapping
    fun getProfile(@CurrentUser principal: UserPrincipal): ResponseEntity<ApiResponse<Any>> {
        return try {
            val response = profileService.getProfile(principal)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @PutMapping
    fun updateProfile(
        @CurrentUser principal: UserPrincipal,
        @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val response = profileService.updateProfile(principal, request)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }
}
