package com.chronosync.controller

import com.chronosync.config.TestSecurityConfig
import com.chronosync.dto.schedule.CreateScheduleRequest
import com.chronosync.dto.schedule.UpdateScheduleRequest
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
class ScheduleControllerTest {

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
    private lateinit var testOrgUser: OrganizationUser
    private lateinit var token: String
    private lateinit var organizationId: UUID

    @BeforeEach
    fun setUp() {
        scheduleRepository.deleteAll()
        organizationUserRepository.deleteAll()
        organizationRepository.deleteAll()
        userRepository.deleteAll()

        testUser = User(
            email = "scheduletest@example.com",
            password = "password123",
            firstName = "Test",
            lastName = "User",
            status = UserStatus.ACTIVE
        )
        testUser = userRepository.save(testUser)

        testOrganization = Organization(
            name = "Schedule Test Org",
            plan = Plan.FREE,
            scheduleLimit = 100,
            currentUsage = 0,
            nextReset = Instant.now().plus(30, ChronoUnit.DAYS)
        )
        testOrganization = organizationRepository.save(testOrganization)
        organizationId = testOrganization.id

        testOrgUser = OrganizationUser(
            user = testUser,
            organization = testOrganization,
            role = OrganizationRole.OWNER,
            status = OrganizationUserStatus.ACTIVE
        )
        organizationUserRepository.save(testOrgUser)

        token = jwtUtil.generateToken(
            testUser.id,
            testUser.email,
            organizationId,
            OrganizationRole.OWNER.name
        )
    }

    @Test
    fun `get today schedules returns success`() {
        val schedule = Schedule(
            title = "Today's Appointment",
            startDateTime = Instant.now(),
            endDateTime = Instant.now().plus(30, ChronoUnit.MINUTES),
            organization = testOrganization,
            assignedUser = testUser
        )
        scheduleRepository.save(schedule)

        val result: MvcResult = mockMvc.perform(
            get("/api/schedules/today")
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
    }

    @Test
    fun `get schedules with date range returns success`() {
        val schedule = Schedule(
            title = "Weekly Appointment",
            startDateTime = Instant.now().plus(1, ChronoUnit.DAYS),
            endDateTime = Instant.now().plus(1, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
            organization = testOrganization
        )
        scheduleRepository.save(schedule)

        val today = java.time.LocalDate.now()
        val from = today.toString()
        val to = today.plusDays(7).toString()

        val result: MvcResult = mockMvc.perform(
            get("/api/schedules")
                .header("Authorization", "Bearer $token")
                .param("from", from)
                .param("to", to)
        ).andExpect(
            status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        assertTrue(response.success)
    }

    @Test
    fun `create schedule returns created schedule`() {
        val request = CreateScheduleRequest(
            title = "New Appointment",
            startDateTime = Instant.now().plus(1, ChronoUnit.DAYS),
            endDateTime = Instant.now().plus(1, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
            assignedUserId = testUser.id.toString(),
            clientName = "John Doe",
            clientEmail = "john@example.com",
            clientPhone = "+1234567890",
            clientAddress = "123 Test St",
            notes = "VIP client"
        )

        val result: MvcResult = mockMvc.perform(
            post("/api/schedules")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(
            status().isCreated
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        assertTrue(response.success)
        assertNotNull(response.data)
    }

    @Test
    fun `get schedule by id returns success`() {
        val schedule = Schedule(
            title = "Get Test Appointment",
            startDateTime = Instant.now().plus(2, ChronoUnit.DAYS),
            endDateTime = Instant.now().plus(2, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
            organization = testOrganization,
            assignedUser = testUser
        )
        val savedSchedule = scheduleRepository.save(schedule)

        val result: MvcResult = mockMvc.perform(
            get("/api/schedules/${savedSchedule.id}")
                .header("Authorization", "Bearer $token")
        ).andExpect(
            status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        assertTrue(response.success)
    }

    @Test
    fun `update schedule returns updated schedule`() {
        val schedule = Schedule(
            title = "Original Title",
            startDateTime = Instant.now().plus(3, ChronoUnit.DAYS),
            endDateTime = Instant.now().plus(3, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
            organization = testOrganization
        )
        val savedSchedule = scheduleRepository.save(schedule)

        val request = UpdateScheduleRequest(
            title = "Updated Title",
            startDateTime = savedSchedule.startDateTime,
            endDateTime = savedSchedule.endDateTime,
            assignedUserId = testUser.id.toString(),
            notes = "Updated notes"
        )

        val result: MvcResult = mockMvc.perform(
            put("/api/schedules/${savedSchedule.id}")
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
    }

    @Test
    fun `delete schedule returns success`() {
        val schedule = Schedule(
            title = "Delete Test",
            startDateTime = Instant.now().plus(4, ChronoUnit.DAYS),
            endDateTime = Instant.now().plus(4, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
            organization = testOrganization
        )
        val savedSchedule = scheduleRepository.save(schedule)

        mockMvc.perform(
            delete("/api/schedules/${savedSchedule.id}")
                .header("Authorization", "Bearer $token")
        ).andExpect(
            status().isOk
        )

        val deletedSchedule = scheduleRepository.findById(savedSchedule.id)
        assertTrue(deletedSchedule.isEmpty)
    }

    @Test
    fun `unauthorized access returns 403`() {
        mockMvc.perform(
            get("/api/schedules/today")
        ).andExpect(
            status().isForbidden
        )
    }

    @Test
    fun `member role cannot create schedule`() {
        val memberUser = User(
            email = "member@example.com",
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

        val request = CreateScheduleRequest(
            title = "Member Schedule",
            startDateTime = Instant.now().plus(5, ChronoUnit.DAYS),
            endDateTime = Instant.now().plus(5, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES)
        )

        mockMvc.perform(
            post("/api/schedules")
                .header("Authorization", "Bearer $memberToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(
            status().isBadRequest
        )
    }
}
