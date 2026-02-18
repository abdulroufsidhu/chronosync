package com.chronosync.controller

import com.chronosync.config.TestSecurityConfig
import com.chronosync.entity.*
import com.chronosync.repository.*
import com.chronosync.security.JwtUtil
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
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
            get("/api/usage")
                .header("Authorization", "Bearer $token")
        ).andExpect(
            status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        assertTrue(response.success)
        assertNotNull(response.data)

        val dataJson = objectMapper.writeValueAsString(response.data)
        val usageResponse = objectMapper.readValue(dataJson, com.chronosync.dto.usage.UsageResponse::class.java)

        assertEquals("FREE", usageResponse.plan)
        assertEquals(45, usageResponse.used)
        assertEquals(100, usageResponse.limit)
        assertFalse(usageResponse.blocked)
        assertNotNull(usageResponse.nextReset)
    }

    @Test
    fun `get usage returns blocked when limit reached`() {
        testOrganization = testOrganization.copy(currentUsage = 100)
        organizationRepository.save(testOrganization)

        val result: MvcResult = mockMvc.perform(
            get("/api/usage")
                .header("Authorization", "Bearer $token")
        ).andExpect(
            status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        assertTrue(response.success)

        val dataJson = objectMapper.writeValueAsString(response.data)
        val usageResponse = objectMapper.readValue(dataJson, com.chronosync.dto.usage.UsageResponse::class.java)

        assertTrue(usageResponse.blocked)
    }

    @Test
    fun `get billing plans returns available plans`() {
        val result: MvcResult = mockMvc.perform(
            get("/api/billing/plans")
                .header("Authorization", "Bearer $token")
        ).andExpect(
            status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        assertTrue(response.success)
        assertNotNull(response.data)

        val dataJson = objectMapper.writeValueAsString(response.data)
        val upgradeResponse = objectMapper.readValue(dataJson, com.chronosync.dto.usage.UpgradeResponse::class.java)

        assertEquals(3, upgradeResponse.plans.size)
        assertEquals("FREE", upgradeResponse.plans[0].name)
        assertEquals("PRO", upgradeResponse.plans[1].name)
        assertEquals("ENTERPRISE", upgradeResponse.plans[2].name)
    }

    @Test
    fun `subscribe to new plan returns updated usage`() {
        val request = mapOf("plan" to "PRO")

        val result: MvcResult = mockMvc.perform(
            post("/api/billing/subscribe")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(
            status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        assertTrue(response.success)

        val dataJson = objectMapper.writeValueAsString(response.data)
        val usageResponse = objectMapper.readValue(dataJson, com.chronosync.dto.usage.UsageResponse::class.java)

        assertEquals("PRO", usageResponse.plan)
        assertEquals(500, usageResponse.limit)
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
            post("/api/billing/subscribe")
                .header("Authorization", "Bearer $memberToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(
            status().isBadRequest
        )
    }

    @Test
    fun `unauthorized access to usage returns 403`() {
        mockMvc.perform(
            get("/api/usage")
        ).andExpect(
            status().isForbidden
        )
    }
}
