package com.chronosync.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "schedules")
data class Schedule(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val title: String,

    @Column(name = "start_date_time", nullable = false)
    val startDateTime: Instant,

    @Column(name = "end_date_time", nullable = false)
    val endDateTime: Instant,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    val assignedUser: User? = null,

    @Column(name = "client_name")
    val clientName: String? = null,

    @Column(name = "client_email")
    val clientEmail: String? = null,

    @Column(name = "client_phone")
    val clientPhone: String? = null,

    @Column(name = "client_address")
    val clientAddress: String? = null,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    val updatedAt: Instant? = null
)
