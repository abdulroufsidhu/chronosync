package com.chronosync.controller

import com.chronosync.dto.common.ApiResponse
import com.chronosync.dto.schedule.*
import com.chronosync.security.CurrentUser
import com.chronosync.security.UserPrincipal
import com.chronosync.service.ScheduleService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/schedules")
class ScheduleController(
    private val scheduleService: ScheduleService
) {

    @GetMapping("/today")
    fun getTodaySchedules(@CurrentUser principal: UserPrincipal): ResponseEntity<ApiResponse<TodayScheduleResponse>> {
        return try {
            val response = scheduleService.getTodaySchedules(principal)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @GetMapping
    fun getSchedules(
        @CurrentUser principal: UserPrincipal,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @RequestParam(required = false) assignedUserId: UUID?
    ): ResponseEntity<ApiResponse<ScheduleListResponse>> {
        return try {
            val response = scheduleService.getSchedules(principal, from, to, assignedUserId)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @GetMapping("/{id}")
    fun getSchedule(
        @CurrentUser principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<ApiResponse<ScheduleDto>> {
        return try {
            val response = scheduleService.getSchedule(principal, id)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @PostMapping
    fun createSchedule(
        @CurrentUser principal: UserPrincipal,
        @RequestBody request: CreateScheduleRequest
    ): ResponseEntity<ApiResponse<ScheduleDto>> {
        return try {
            val response = scheduleService.createSchedule(principal, request)
            ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @PutMapping("/{id}")
    fun updateSchedule(
        @CurrentUser principal: UserPrincipal,
        @PathVariable id: UUID,
        @RequestBody request: UpdateScheduleRequest
    ): ResponseEntity<ApiResponse<ScheduleDto>> {
        return try {
            val response = scheduleService.updateSchedule(principal, id, request)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteSchedule(
        @CurrentUser principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            scheduleService.deleteSchedule(principal, id)
            ResponseEntity.ok(ApiResponse(success = true, message = "Schedule deleted successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }
}
