package com.chronosync.controller

import com.chronosync.config.TestSecurityConfig
import com.chronosync.dto.organization.InviteUserRequest
import com.chronosync.dto.organization.UpdateOrganizationRequest
import com.chronosync.dto.organization.UpdateUserRoleRequest
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
class OrganizationControllerTest {

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
    private lateinit var authTokenRepository: AuthTokenRepository

    @Autowired
    private lateinit var scheduleRepository: ScheduleRepository

    private lateinit var testUser: User
    private lateinit var testOrganization: Organization
    private lateinit var token: String
    private lateinit var organizationId: UUID

    @BeforeEach
    fun setUp() {
        scheduleRepository.deleteAll()
        authTokenRepository.deleteAll()
        organizationUserRepository.deleteAll()
        organizationRepository.deleteAll()
        userRepository.deleteAll()

        testUser = User(
            email = "orgtest@example.com",
            password = "password123",
            firstName = "Org",
            lastName = "Admin",
            status = UserStatus.ACTIVE
        )
        testUser = userRepository.save(testUser)

        testOrganization = Organization(
            name = "Organization Test Org",
            plan = Plan.FREE,
            scheduleLimit = 100,
            currentUsage = 0,
            nextReset = Instant.now().plus(30, ChronoUnit.DAYS),
            timezone = "America/New_York"
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
    fun `get organization users returns user list`() {
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

        val result: MvcResult = mockMvc.perform(
            get("/api/organization/users")
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
    fun `get user dropdowns returns user list`() {
        val memberUser = User(
            email = "dropdown@example.com",
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

        val result: MvcResult = mockMvc.perform(
            get("/api/organization/users/dropdown")
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
    fun `invite new user returns success`() {
        val request = InviteUserRequest(
            email = "invited@example.com",
            role = "MEMBER"
        )

        val result: MvcResult = mockMvc.perform(
            post("/api/organization/invite")
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
        assertEquals("Invitation sent successfully", response.message)
    }

    @Test
    fun `update user role returns success`() {
        val memberUser = User(
            email = "updaterole@example.com",
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
        val savedOrgUser = organizationUserRepository.save(orgUser)

        val request = UpdateUserRoleRequest(
            role = "MANAGER"
        )

        val result: MvcResult = mockMvc.perform(
            put("/api/organization/users/${savedOrgUser.user.id}/role")
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
    fun `remove user returns success`() {
        val memberUser = User(
            email = "remove@example.com",
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
        val savedOrgUser = organizationUserRepository.save(orgUser)

        mockMvc.perform(
            delete("/api/organization/users/${savedOrgUser.user.id}")
                .header("Authorization", "Bearer $token")
        ).andExpect(
            status().isOk
        )

        val deleted = organizationUserRepository.findByUserIdAndOrganizationId(savedMember.id, organizationId)
        assertNull(deleted)
    }

    @Test
    fun `cannot remove owner returns bad request`() {
        mockMvc.perform(
            delete("/api/organization/users/${testUser.id}")
                .header("Authorization", "Bearer $token")
        ).andExpect(
            status().isBadRequest
        )
    }

    @Test
    fun `member cannot invite users`() {
        val memberUser = User(
            email = "memberinvite@example.com",
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

        val request = InviteUserRequest(
            email = "newuser@example.com",
            role = "MEMBER"
        )

        mockMvc.perform(
            post("/api/organization/invite")
                .header("Authorization", "Bearer $memberToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(
            status().isBadRequest
        )
    }

    @Test
    fun `update organization name returns success`() {
        val request = UpdateOrganizationRequest(name = "Updated Org Name")

        val result: MvcResult = mockMvc.perform(
            put("/api/organization/settings")
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

        val saved = organizationRepository.findById(organizationId).get()
        assertEquals("Updated Org Name", saved.name)
    }

    @Test
    fun `update organization coordinates stores lat and lng`() {
        val request = UpdateOrganizationRequest(latitude = 40.7128, longitude = -74.0060)

        val result: MvcResult = mockMvc.perform(
            put("/api/organization/settings")
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

        val saved = organizationRepository.findById(organizationId).get()
        assertEquals(40.7128, saved.latitude)
        assertEquals(-74.0060, saved.longitude)
    }

    @Test
    fun `member cannot update organization settings`() {
        val memberUser = User(
            email = "membersettings@example.com",
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

        val request = UpdateOrganizationRequest(name = "Should Not Update")

        mockMvc.perform(
            put("/api/organization/settings")
                .header("Authorization", "Bearer $memberToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(
            status().isBadRequest
        )
    }
}
