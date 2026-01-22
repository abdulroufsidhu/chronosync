package com.chronosync.controller

import com.chronosync.dto.common.ApiResponse
import com.chronosync.security.CurrentUser
import com.chronosync.security.UserPrincipal
import com.chronosync.service.UsageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class UsageController(
    private val usageService: UsageService
) {

    @GetMapping("/usage")
    fun getUsage(@CurrentUser principal: UserPrincipal): ResponseEntity<ApiResponse<Any>> {
        return try {
            val response = usageService.getUsage(principal)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @GetMapping("/billing/plans")
    fun getUpgradePlans(): ResponseEntity<ApiResponse<Any>> {
        return try {
            val response = usageService.getUpgradePlans()
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @PostMapping("/billing/subscribe")
    fun upgradePlan(
        @CurrentUser principal: UserPrincipal,
        @RequestBody request: Map<String, String>
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val planName = request["plan"] ?: throw IllegalArgumentException("Plan is required")
            val response = usageService.upgradePlan(principal, planName)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }
}
