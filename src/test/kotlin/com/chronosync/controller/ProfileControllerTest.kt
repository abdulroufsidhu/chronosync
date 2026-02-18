package com.chronosync.controller

import com.chronosync.config.TestSecurityConfig
import com.chronosync.dto.profile.UpdateProfileRequest
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
class ProfileControllerTest {

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
    private lateinit var secondOrganization: Organization
    private lateinit var token: String
    private lateinit var organizationId: UUID

    @BeforeEach
    fun setUp() {
        organizationUserRepository.deleteAll()
        organizationRepository.deleteAll()
        userRepository.deleteAll()

        testUser = User(
            email = "profiletest@example.com",
            password = "password123",
            firstName = "Profile",
            lastName = "User",
            status = UserStatus.ACTIVE
        )
        testUser = userRepository.save(testUser)

        testOrganization = Organization(
            name = "Profile Test Org 1",
            plan = Plan.FREE,
            scheduleLimit = 100,
            currentUsage = 0,
            nextReset = Instant.now().plus(30, ChronoUnit.DAYS)
        )
        testOrganization = organizationRepository.save(testOrganization)

        secondOrganization = Organization(
            name = "Profile Test Org 2",
            plan = Plan.PRO,
            scheduleLimit = 500,
            currentUsage = 10,
            nextReset = Instant.now().plus(15, ChronoUnit.DAYS)
        )
        secondOrganization = organizationRepository.save(secondOrganization)

        val orgUser1 = OrganizationUser(
            user = testUser,
            organization = testOrganization,
            role = OrganizationRole.OWNER,
            status = OrganizationUserStatus.ACTIVE
        )
        organizationUserRepository.save(orgUser1)

        val orgUser2 = OrganizationUser(
            user = testUser,
            organization = secondOrganization,
            role = OrganizationRole.MEMBER,
            status = OrganizationUserStatus.ACTIVE
        )
        organizationUserRepository.save(orgUser2)

        token = jwtUtil.generateToken(
            testUser.id,
            testUser.email,
            testOrganization.id,
            OrganizationRole.OWNER.name
        )
    }

    @Test
    fun `get profile returns user profile with organizations`() {
        val result: MvcResult = mockMvc.perform(
            get("/api/profile")
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
        val profileResponse = objectMapper.readValue(dataJson, com.chronosync.dto.profile.ProfileResponse::class.java)

        assertEquals("Profile User", profileResponse.name)
        assertEquals("profiletest@example.com", profileResponse.email)
        assertEquals("OWNER", profileResponse.role)
        assertEquals(2, profileResponse.organizations.size)
    }

    @Test
    fun `profile shows correct organization roles`() {
        val result: MvcResult = mockMvc.perform(
            get("/api/profile")
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
        val profileResponse = objectMapper.readValue(dataJson, com.chronosync.dto.profile.ProfileResponse::class.java)

        val org1 = profileResponse.organizations.find { it.name == "Profile Test Org 1" }
        val org2 = profileResponse.organizations.find { it.name == "Profile Test Org 2" }

        assertNotNull(org1)
        assertNotNull(org2)
        assertEquals("OWNER", org1?.role)
        assertEquals("MEMBER", org2?.role)
        assertEquals("FREE", org1?.plan)
        assertEquals("PRO", org2?.plan)
    }

    @Test
    fun `profile returns email as name when no first or last name`() {
        val userWithoutName = User(
            email = "noname@example.com",
            password = "password123",
            status = UserStatus.ACTIVE
        )
        val savedUser = userRepository.save(userWithoutName)

        val orgUser = OrganizationUser(
            user = savedUser,
            organization = testOrganization,
            role = OrganizationRole.MEMBER,
            status = OrganizationUserStatus.ACTIVE
        )
        organizationUserRepository.save(orgUser)

        val tokenWithoutName = jwtUtil.generateToken(
            savedUser.id,
            savedUser.email,
            testOrganization.id,
            OrganizationRole.MEMBER.name
        )

        val result: MvcResult = mockMvc.perform(
            get("/api/profile")
                .header("Authorization", "Bearer $tokenWithoutName")
        ).andExpect(
            status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        assertTrue(response.success)

        val dataJson = objectMapper.writeValueAsString(response.data)
        val profileResponse = objectMapper.readValue(dataJson, com.chronosync.dto.profile.ProfileResponse::class.java)

        assertEquals("noname@example.com", profileResponse.name)
        assertEquals("MEMBER", profileResponse.role)
    }

    @Test
    fun `unauthorized access to profile returns 403`() {
        mockMvc.perform(
            get("/api/profile")
        ).andExpect(
            status().isForbidden
        )
    }

    @Test
    fun `update profile updates user information`() {
        val request = UpdateProfileRequest(
            firstName = "Updated",
            lastName = "User",
            phoneNumber = "+1234567890"
        )

        val result: MvcResult = mockMvc.perform(
            put("/api/profile")
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
        val profileResponse = objectMapper.readValue(dataJson, com.chronosync.dto.profile.ProfileResponse::class.java)

        assertEquals("Updated User", profileResponse.name)

        val updatedUser = userRepository.findById(testUser.id).orElse(null)
        assertNotNull(updatedUser)
        assertEquals("Updated", updatedUser?.firstName)
        assertEquals("User", updatedUser?.lastName)
        assertEquals("+1234567890", updatedUser?.phoneNumber)
    }

    @Test
    fun `update profile with partial data updates only provided fields`() {
        val request = UpdateProfileRequest(firstName = "NewFirstName")

        val result: MvcResult = mockMvc.perform(
            put("/api/profile")
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

        val updatedUser = userRepository.findById(testUser.id).orElse(null)
        assertNotNull(updatedUser)
        assertEquals("NewFirstName", updatedUser?.firstName)
        assertEquals("User", updatedUser?.lastName)
    }

    @Test
    fun `user has different roles across organizations`() {
        val thirdOrganization = Organization(
            name = "Third Org",
            plan = Plan.ENTERPRISE,
            scheduleLimit = Int.MAX_VALUE,
            currentUsage = 0,
            nextReset = Instant.now().plus(30, ChronoUnit.DAYS)
        )
        val savedThirdOrg = organizationRepository.save(thirdOrganization)

        val orgUser3 = OrganizationUser(
            user = testUser,
            organization = savedThirdOrg,
            role = OrganizationRole.OWNER,
            status = OrganizationUserStatus.ACTIVE
        )
        organizationUserRepository.save(orgUser3)

        val multiOrgToken = jwtUtil.generateToken(
            testUser.id,
            testUser.email,
            secondOrganization.id,
            OrganizationRole.MEMBER.name
        )

        val result: MvcResult = mockMvc.perform(
            get("/api/profile")
                .header("Authorization", "Bearer $multiOrgToken")
        ).andExpect(
            status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        assertTrue(response.success)

        val dataJson = objectMapper.writeValueAsString(response.data)
        val profileResponse = objectMapper.readValue(dataJson, com.chronosync.dto.profile.ProfileResponse::class.java)

        assertEquals("MEMBER", profileResponse.role)
        assertEquals(3, profileResponse.organizations.size)

        val org1Role = profileResponse.organizations.find { it.name == "Profile Test Org 1" }?.role
        val org2Role = profileResponse.organizations.find { it.name == "Profile Test Org 2" }?.role
        val org3Role = profileResponse.organizations.find { it.name == "Third Org" }?.role

        assertEquals("OWNER", org1Role)
        assertEquals("MEMBER", org2Role)
        assertEquals("OWNER", org3Role)
    }
}
