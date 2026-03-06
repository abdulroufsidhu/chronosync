package com.chronosync.dto.usage

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class SubscribeRequest(
    @Schema(
        description = "Current subscription plan and is required",
        examples = ["FREE", "PRO", "ENTERPRISE"]
    )
    val plan: String?,
    @Schema(
        description = "Payment varification token to varify payment ( planned for future ) ",
        example = "123adsf234afds2"
    )
    val paymentId: String?,
    @Schema(
        description = "Payment Method ( planned for future )",
        examples = ["Google", "Apple", "MyFatoorah", "Alflalah", "Easypesa", "JazzCash", "Nayapay"]
    )
    val paymentMethod: String?,
)

data class UsageResponse(
    @Schema(description = "Current subscription plan", example = "FREE")
    val plan: String,

    @Schema(description = "Schedule usage")
    val schedules: UsageCountDto,

    @Schema(description = "Team member usage")
    val teamMembers: UsageCountDto,

    @Schema(description = "Whether new schedules are blocked due to limit reached")
    val blocked: Boolean,

    @Schema(description = "Date when usage resets (for monthly plans)")
    val nextReset: Instant?
)

data class UsageCountDto(
    @Schema(description = "Number used")
    val used: Int,

    @Schema(description = "Maximum allowed")
    val limit: Int
)

data class PlanDto(
    @Schema(description = "Plan ID", example = "free")
    val id: String,

    @Schema(description = "Plan name", example = "Free")
    val name: String,

    @Schema(description = "Plan price", example = "0")
    val price: Int,

    @Schema(description = "Yearly price", example = "0")
    val yearlyPrice: Int?,

    @Schema(description = "List of features included in the plan")
    val features: List<String>,

    @Schema(description = "Whether this plan is featured")
    val isFeatured: Boolean
)

data class UpgradeResponse(
    @Schema(description = "List of available subscription plans")
    val plans: List<PlanDto>
)
