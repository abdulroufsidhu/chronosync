package com.chronosync.controller

import com.chronosync.entity.*
import com.chronosync.repository.*
import com.chronosync.security.JwtUtil
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class UsageControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jwtUtil: JwtUtil

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var organizationUserRepository: OrganizationUserRepository

    private lateinit var testUser: User
    private lateinit var testOrganization: Organization
    private lateinit var token: String
    private lateinit var organizationId: UUID

    @BeforeEach
    fun setUp() {
        organizationUserRepository.deleteAll()
        organizationRepository.deleteAll()
        userRepository.deleteAll()

        testUser = User(
            email = "usagetest@example.com",
            password = "password123",
            status = UserStatus.ACTIVE
        )
        testUser = userRepository.save(testUser)

        testOrganization = Organization(
            name = "Usage Test Org",
            plan = Plan.FREE,
            scheduleLimit = 100,
            currentUsage = 45,
            nextReset = Instant.now().plus(30, ChronoUnit.DAYS)
        )
        testOrganization = organizationRepository.save(testOrganization)
        organizationId = testOrganization.id

        val orgUser = OrganizationUser(
            user = testUser,
            organization = testOrganization,
            role = OrganizationRole.OWNER,
            status = OrganizationUserStatus.ACTIVE
        )
        organizationUserRepository.save(orgUser)

        token = jwtUtil.generateToken(
            testUser.id,
            testUser.email,
            organizationId,
            OrganizationRole.OWNER.name
        )
    }

    @Test
    fun `get usage returns current usage data`() {
        val result: MvcResult = mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/usage")
                .header("Authorization", "Bearer $token")
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        org.junit.jupiter.api.Assertions.assertTrue(response.success)
        org.junit.jupiter.api.Assertions.assertNotNull(response.data)

        val dataJson = objectMapper.writeValueAsString(response.data)
        val usageResponse = objectMapper.readValue(dataJson, com.chronosync.dto.usage.UsageResponse::class.java)

        org.junit.jupiter.api.Assertions.assertEquals("FREE", usageResponse.plan)
        org.junit.jupiter.api.Assertions.assertEquals(45, usageResponse.used)
        org.junit.jupiter.api.Assertions.assertEquals(100, usageResponse.limit)
        org.junit.jupiter.api.Assertions.assertFalse(usageResponse.blocked)
        org.junit.jupiter.api.Assertions.assertNotNull(usageResponse.nextReset)
    }

    @Test
    fun `get usage returns blocked when limit reached`() {
        testOrganization = testOrganization.copy(currentUsage = 100)
        organizationRepository.save(testOrganization)

        val result: MvcResult = mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/usage")
                .header("Authorization", "Bearer $token")
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        org.junit.jupiter.api.Assertions.assertTrue(response.success)

        val dataJson = objectMapper.writeValueAsString(response.data)
        val usageResponse = objectMapper.readValue(dataJson, com.chronosync.dto.usage.UsageResponse::class.java)

        org.junit.jupiter.api.Assertions.assertTrue(usageResponse.blocked)
    }

    @Test
    fun `get billing plans returns available plans`() {
        val result: MvcResult = mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/billing/plans")
                .header("Authorization", "Bearer $token")
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        org.junit.jupiter.api.Assertions.assertTrue(response.success)
        org.junit.jupiter.api.Assertions.assertNotNull(response.data)

        val dataJson = objectMapper.writeValueAsString(response.data)
        val upgradeResponse = objectMapper.readValue(dataJson, com.chronosync.dto.usage.UpgradeResponse::class.java)

        org.junit.jupiter.api.Assertions.assertEquals(3, upgradeResponse.plans.size)
        org.junit.jupiter.api.Assertions.assertEquals("FREE", upgradeResponse.plans[0].name)
        org.junit.jupiter.api.Assertions.assertEquals("PRO", upgradeResponse.plans[1].name)
        org.junit.jupiter.api.Assertions.assertEquals("ENTERPRISE", upgradeResponse.plans[2].name)
    }

    @Test
    fun `subscribe to new plan returns updated usage`() {
        val request = mapOf("plan" to "PRO")

        val result: MvcResult = mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/billing/subscribe")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        org.junit.jupiter.api.Assertions.assertTrue(response.success)

        val dataJson = objectMapper.writeValueAsString(response.data)
        val usageResponse = objectMapper.readValue(dataJson, com.chronosync.dto.usage.UsageResponse::class.java)

        org.junit.jupiter.api.Assertions.assertEquals("PRO", usageResponse.plan)
        org.junit.jupiter.api.Assertions.assertEquals(500, usageResponse.limit)
    }

    @Test
    fun `member cannot upgrade plan`() {
        val memberUser = User(
            email = "memberusage@example.com",
            password = "password123",
            status = UserStatus.ACTIVE
        )
        val savedMember = userRepository.save(memberUser)

        val orgUser = OrganizationUser(
            user = savedMember,
            organization = testOrganization,
            role = OrganizationRole.MEMBER,
            status = OrganizationUserStatus.ACTIVE
        )
        organizationUserRepository.save(orgUser)

        val memberToken = jwtUtil.generateToken(
            savedMember.id,
            savedMember.email,
            organizationId,
            OrganizationRole.MEMBER.name
        )

        val request = mapOf("plan" to "PRO")

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/billing/subscribe")
                .header("Authorization", "Bearer $memberToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest
        )
    }

    @Test
    fun `unauthorized access to usage returns 403`() {
        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/usage")
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden
        )
    }
}
