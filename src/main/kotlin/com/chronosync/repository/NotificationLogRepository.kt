package com.chronosync.repository

import com.chronosync.entity.NotificationLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NotificationLogRepository : JpaRepository<NotificationLog, UUID> {
    fun findByScheduleId(scheduleId: UUID): List<NotificationLog>
    fun findByRecipientEmail(recipientEmail: String): List<NotificationLog>
}
