package com.chronosync.controller

import com.chronosync.dto.common.ApiResponse
import com.chronosync.dto.notification.NotificationListResponse
import com.chronosync.security.CurrentUser
import com.chronosync.security.UserPrincipal
import com.chronosync.service.NotificationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping
    fun getNotifications(
        @CurrentUser principal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<NotificationListResponse>> {
        return try {
            val response = notificationService.getNotifications(principal, page, size)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @PutMapping("/{id}/read")
    fun markAsRead(
        @CurrentUser principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            notificationService.markAsRead(principal, id)
            ResponseEntity.ok(ApiResponse(success = true, message = "Notification marked as read"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @PutMapping("/read-all")
    fun markAllAsRead(
        @CurrentUser principal: UserPrincipal
    ): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            notificationService.markAllAsRead(principal)
            ResponseEntity.ok(ApiResponse(success = true, message = "All notifications marked as read"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }
}
