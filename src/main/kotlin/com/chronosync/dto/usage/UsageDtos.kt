package com.chronosync.dto.usage

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class UsageResponse(
    @Schema(description = "Current subscription plan", example = "FREE")
    val plan: String,

    @Schema(description = "Number of schedules used this period")
    val used: Int,

    @Schema(description = "Maximum number of schedules allowed")
    val limit: Int,

    @Schema(description = "Whether new schedules are blocked due to limit reached")
    val blocked: Boolean,

    @Schema(description = "Date when usage resets (for monthly plans)")
    val nextReset: Instant?
)

data class PlanDto(
    @Schema(description = "Plan name", example = "PRO")
    val name: String,

    @Schema(description = "Plan price", example = "$5/month")
    val price: String,

    @Schema(description = "List of features included in the plan")
    val features: List<String>
)

data class UpgradeResponse(
    @Schema(description = "List of available subscription plans")
    val plans: List<PlanDto>
)
