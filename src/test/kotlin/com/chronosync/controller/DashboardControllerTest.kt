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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {

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

    @Autowired
    private lateinit var scheduleRepository: ScheduleRepository

    private lateinit var testUser: User
    private lateinit var testOrganization: Organization
    private lateinit var token: String
    private lateinit var organizationId: UUID

    @BeforeEach
    fun setUp() {
        scheduleRepository.deleteAll()
        organizationUserRepository.deleteAll()
        organizationRepository.deleteAll()
        userRepository.deleteAll()

        testUser = User(
            email = "dashboardtest@example.com",
            password = "password123",
            firstName = "Dashboard",
            lastName = "User",
            status = UserStatus.ACTIVE
        )
        testUser = userRepository.save(testUser)

        testOrganization = Organization(
            name = "Dashboard Test Org",
            plan = Plan.FREE,
            scheduleLimit = 100,
            currentUsage = 5,
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
    fun `get today dashboard returns success with schedules`() {
        val schedule1 = Schedule(
            title = "Morning Appointment",
            startDateTime = Instant.now().plus(1, ChronoUnit.HOURS),
            endDateTime = Instant.now().plus(1, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES),
            organization = testOrganization,
            assignedUser = testUser
        )
        val schedule2 = Schedule(
            title = "Afternoon Appointment",
            startDateTime = Instant.now().plus(5, ChronoUnit.HOURS),
            endDateTime = Instant.now().plus(5, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES),
            organization = testOrganization,
            assignedUser = testUser
        )
        scheduleRepository.save(schedule1)
        scheduleRepository.save(schedule2)

        val result: MvcResult = mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/dashboard/today")
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
        val todayResponse = objectMapper.readValue(dataJson, com.chronosync.dto.schedule.TodayScheduleResponse::class.java)

        org.junit.jupiter.api.Assertions.assertEquals(java.time.LocalDate.now().toString(), todayResponse.date)
        org.junit.jupiter.api.Assertions.assertEquals("OWNER", todayResponse.userRole)
        org.junit.jupiter.api.Assertions.assertEquals(5, todayResponse.usage.used)
        org.junit.jupiter.api.Assertions.assertEquals(100, todayResponse.usage.limit)
    }

    @Test
    fun `get today dashboard returns empty schedules when none exist`() {
        val result: MvcResult = mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/dashboard/today")
                .header("Authorization", "Bearer $token")
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        org.junit.jupiter.api.Assertions.assertTrue(response.success)
    }

    @Test
    fun `get today dashboard with member role shows only assigned schedules`() {
        val memberUser = User(
            email = "memberdashboard@example.com",
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

        val ownSchedule = Schedule(
            title = "Member's Own Schedule",
            startDateTime = Instant.now().plus(2, ChronoUnit.HOURS),
            endDateTime = Instant.now().plus(2, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES),
            organization = testOrganization,
            assignedUser = savedMember
        )
        val otherSchedule = Schedule(
            title = "Other User Schedule",
            startDateTime = Instant.now().plus(3, ChronoUnit.HOURS),
            endDateTime = Instant.now().plus(3, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES),
            organization = testOrganization,
            assignedUser = testUser
        )
        scheduleRepository.save(ownSchedule)
        scheduleRepository.save(otherSchedule)

        val result: MvcResult = mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/dashboard/today")
                .header("Authorization", "Bearer $memberToken")
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        org.junit.jupiter.api.Assertions.assertTrue(response.success)

        val dataJson = objectMapper.writeValueAsString(response.data)
        val todayResponse = objectMapper.readValue(dataJson, com.chronosync.dto.schedule.TodayScheduleResponse::class.java)

        org.junit.jupiter.api.Assertions.assertEquals("MEMBER", todayResponse.userRole)
    }

    @Test
    fun `unauthorized access to dashboard returns 403`() {
        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/dashboard/today")
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden
        )
    }
}
