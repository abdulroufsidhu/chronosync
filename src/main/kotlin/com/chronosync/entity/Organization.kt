package com.chronosync.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "organizations")
data class Organization(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val name: String,

    @Column(name = "organization_type")
    val type: String? = null,

    @Column(name = "services", nullable = false)
    @Convert(converter = StringListConverter::class)
    val services: List<String> = emptyList(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val plan: Plan = Plan.FREE,

    @Column(name = "schedule_limit")
    val scheduleLimit: Int = 100,

    @Column(name = "current_usage")
    val currentUsage: Int = 0,

    @Column(name = "next_reset")
    val nextReset: Instant? = null,

    @Column
    val latitude: Double? = null,

    @Column
    val longitude: Double? = null,

    @Column(nullable = false)
    val timezone: String = "UTC",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    val updatedAt: Instant? = null
)

enum class Plan(val displayName: String, val price: String, val priceValue: Int, val limit: Int) {
    FREE("Free", "$0/month", 0, 100),
    PRO("Pro", "$5/month", 5, 500),
    ENTERPRISE("Enterprise", "$20/month", 20, -1)
}
