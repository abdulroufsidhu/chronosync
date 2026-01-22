package com.chronosync.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "plans")
data class PlanEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val name: String,

    @Column(nullable = false)
    val price: String,

    @Column(name = "schedule_limit")
    val scheduleLimit: Int,

    @Column(columnDefinition = "TEXT")
    val features: String? = null
)
