package com.chronosync.dto.notification

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class NotificationDto(
    @Schema(description = "Notification UUID")
    val id: String,

    @Schema(description = "Notification type")
    val type: String,

    @Schema(description = "Notification title")
    val title: String,

    @Schema(description = "Notification message")
    val message: String,

    @Schema(description = "Whether notification is read")
    val isRead: Boolean,

    @Schema(description = "Action URL")
    val actionUrl: String?,

    @Schema(description = "When notification was created")
    val time: Instant
)

data class NotificationListResponse(
    @Schema(description = "List of notifications")
    val notifications: List<NotificationDto>,

    @Schema(description = "Current page number")
    val page: Int,

    @Schema(description = "Page size")
    val size: Int,

    @Schema(description = "Total elements")
    val totalElements: Long,

    @Schema(description = "Total pages")
    val totalPages: Int,

    @Schema(description = "Unread count")
    val unreadCount: Long
)
