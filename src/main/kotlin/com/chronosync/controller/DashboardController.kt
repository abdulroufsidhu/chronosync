package com.chronosync.controller

import com.chronosync.dto.common.ApiResponse
import com.chronosync.dto.schedule.TodayScheduleResponse
import com.chronosync.security.CurrentUser
import com.chronosync.security.UserPrincipal
import com.chronosync.service.ScheduleService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val scheduleService: ScheduleService
) {

    @GetMapping("/today")
    fun getTodayDashboard(@CurrentUser principal: UserPrincipal): ResponseEntity<ApiResponse<Any>> {
        return try {
            val response = scheduleService.getTodaySchedules(principal)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }
}
