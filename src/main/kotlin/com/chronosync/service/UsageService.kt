package com.chronosync.service

import com.chronosync.dto.usage.PlanDto
import com.chronosync.dto.usage.UpgradeResponse
import com.chronosync.dto.usage.UsageResponse
import com.chronosync.entity.Plan
import com.chronosync.repository.OrganizationRepository
import com.chronosync.security.UserPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UsageService(
    private val organizationRepository: OrganizationRepository
) {

    @Transactional(readOnly = true)
    fun getUsage(principal: UserPrincipal): UsageResponse {
        val organization = organizationRepository.findById(principal.organizationId)
            .orElseThrow { IllegalArgumentException("Organization not found") }

        val planLimit = when (organization.plan) {
            Plan.FREE -> 100
            Plan.PRO -> 500
            Plan.ENTERPRISE -> Int.MAX_VALUE
        }

        val blocked = organization.plan != Plan.ENTERPRISE && organization.currentUsage >= planLimit

        return UsageResponse(
            plan = organization.plan.name,
            used = organization.currentUsage,
            limit = planLimit,
            blocked = blocked,
            nextReset = organization.nextReset
        )
    }

    @Transactional(readOnly = true)
    fun getUpgradePlans(): UpgradeResponse {
        val plans = Plan.entries.map { plan ->
            val features = when (plan) {
                Plan.FREE -> listOf("Up to 100 schedules/month", "Basic features")
                Plan.PRO -> listOf("Up to 500 schedules/month", "Priority support", "Advanced features")
                Plan.ENTERPRISE -> listOf("Unlimited schedules", "24/7 support", "All features", "Custom integrations")
            }
            PlanDto(
                name = plan.name,
                price = plan.price,
                features = features
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
