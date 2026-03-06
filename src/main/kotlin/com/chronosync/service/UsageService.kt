package com.chronosync.service

import com.chronosync.dto.usage.PlanDto
import com.chronosync.dto.usage.UpgradeResponse
import com.chronosync.dto.usage.UsageCountDto
import com.chronosync.dto.usage.UsageResponse
import com.chronosync.entity.OrganizationUserStatus
import com.chronosync.entity.Plan
import com.chronosync.repository.OrganizationRepository
import com.chronosync.repository.OrganizationUserRepository
import com.chronosync.security.UserPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UsageService(
    private val organizationRepository: OrganizationRepository,
    private val organizationUserRepository: OrganizationUserRepository
) {

    @Transactional(readOnly = true)
    fun getUsage(principal: UserPrincipal): UsageResponse {
        val organization = organizationRepository.findById(principal.organizationId)
            .orElseThrow { IllegalArgumentException("Organization not found") }

        val scheduleLimit = when (organization.plan) {
            Plan.FREE -> 100
            Plan.PRO -> 500
            Plan.ENTERPRISE -> Int.MAX_VALUE
        }

        val teamMemberLimit = when (organization.plan) {
            Plan.FREE -> 5
            Plan.PRO -> 20
            Plan.ENTERPRISE -> Int.MAX_VALUE
        }

        val teamMemberCount = organizationUserRepository.countByOrganizationIdAndStatus(
            principal.organizationId,
            OrganizationUserStatus.ACTIVE
        ).toInt()

        val blocked = organization.plan != Plan.ENTERPRISE && organization.currentUsage >= scheduleLimit

        return UsageResponse(
            plan = organization.plan.name,
            schedules = UsageCountDto(
                used = organization.currentUsage,
                limit = scheduleLimit
            ),
            teamMembers = UsageCountDto(
                used = teamMemberCount,
                limit = teamMemberLimit
            ),
            blocked = blocked,
            nextReset = organization.nextReset
        )
    }

    @Transactional(readOnly = true)
    fun getUpgradePlans(): UpgradeResponse {
        val plans = Plan.entries.map { plan ->
            val features = when (plan) {
                Plan.FREE -> listOf("100 schedules/month", "5 team members", "Basic features")
                Plan.PRO -> listOf("500 schedules/month", "20 team members", "Priority support", "Advanced features")
                Plan.ENTERPRISE -> listOf("Unlimited schedules", "Unlimited team members", "24/7 support", "All features", "Custom integrations")
            }
            PlanDto(
                id = plan.name.lowercase(),
                name = plan.displayName,
                price = plan.priceValue,
                yearlyPrice = if (plan.priceValue > 0) (plan.priceValue * 12 * 0.8).toInt() else null,
                features = features,
                isFeatured = plan == Plan.PRO
            )
        }

        return UpgradeResponse(plans = plans)
    }

    @Transactional
    fun upgradePlan(principal: UserPrincipal, planName: String): UsageResponse {
        if (!isAdmin(principal.role)) {
            throw IllegalArgumentException("Only admins can upgrade plans")
        }

        val organization = organizationRepository.findById(principal.organizationId)
            .orElseThrow { IllegalArgumentException("Organization not found") }

        val newPlan = Plan.entries.find { it.name.equals(planName, ignoreCase = true) }
            ?: throw IllegalArgumentException("Invalid plan")

        val newLimit = when (newPlan) {
            Plan.FREE -> 100
            Plan.PRO -> 500
            Plan.ENTERPRISE -> Int.MAX_VALUE
        }

        val updatedOrg = organization.copy(
            plan = newPlan,
            scheduleLimit = newLimit
        )
        organizationRepository.save(updatedOrg)

        return getUsage(principal)
    }

    private fun isAdmin(role: String): Boolean {
        return role == "OWNER" || role == "MANAGER"
    }
}
