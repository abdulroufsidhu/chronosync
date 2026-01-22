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
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/profile")
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
        val profileResponse = objectMapper.readValue(dataJson, com.chronosync.dto.profile.ProfileResponse::class.java)

        org.junit.jupiter.api.Assertions.assertEquals("Profile User", profileResponse.name)
        org.junit.jupiter.api.Assertions.assertEquals("profiletest@example.com", profileResponse.email)
        org.junit.jupiter.api.Assertions.assertEquals("OWNER", profileResponse.role)
        org.junit.jupiter.api.Assertions.assertEquals(2, profileResponse.organizations.size)
    }

    @Test
    fun `profile shows correct organization roles`() {
        val result: MvcResult = mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/profile")
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
        val profileResponse = objectMapper.readValue(dataJson, com.chronosync.dto.profile.ProfileResponse::class.java)

        val org1 = profileResponse.organizations.find { it.name == "Profile Test Org 1" }
        val org2 = profileResponse.organizations.find { it.name == "Profile Test Org 2" }

        org.junit.jupiter.api.Assertions.assertNotNull(org1)
        org.junit.jupiter.api.Assertions.assertNotNull(org2)
        org.junit.jupiter.api.Assertions.assertEquals("OWNER", org1?.role)
        org.junit.jupiter.api.Assertions.assertEquals("MEMBER", org2?.role)
        org.junit.jupiter.api.Assertions.assertEquals("FREE", org1?.plan)
        org.junit.jupiter.api.Assertions.assertEquals("PRO", org2?.plan)
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
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/profile")
                .header("Authorization", "Bearer $tokenWithoutName")
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        org.junit.jupiter.api.Assertions.assertTrue(response.success)

        val dataJson = objectMapper.writeValueAsString(response.data)
        val profileResponse = objectMapper.readValue(dataJson, com.chronosync.dto.profile.ProfileResponse::class.java)

        org.junit.jupiter.api.Assertions.assertEquals("noname@example.com", profileResponse.name)
        org.junit.jupiter.api.Assertions.assertEquals("MEMBER", profileResponse.role)
    }

    @Test
    fun `unauthorized access to profile returns 403`() {
        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/profile")
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden
        )
    }

    @Test
    fun `update profile updates user information`() {
        val request = mapOf(
            "firstName" to "Updated",
            "lastName" to "User",
            "phoneNumber" to "+1234567890"
        )

        val result: MvcResult = mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/profile")
                .header("Authorization", "Bearer $token")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
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
        val profileResponse = objectMapper.readValue(dataJson, com.chronosync.dto.profile.ProfileResponse::class.java)

        org.junit.jupiter.api.Assertions.assertEquals("Updated User", profileResponse.name)

        val updatedUser = userRepository.findById(testUser.id).orElse(null)
        org.junit.jupiter.api.Assertions.assertNotNull(updatedUser)
        org.junit.jupiter.api.Assertions.assertEquals("Updated", updatedUser?.firstName)
        org.junit.jupiter.api.Assertions.assertEquals("User", updatedUser?.lastName)
        org.junit.jupiter.api.Assertions.assertEquals("+1234567890", updatedUser?.phoneNumber)
    }

    @Test
    fun `update profile with partial data updates only provided fields`() {
        val request = mapOf("firstName" to "NewFirstName")

        val result: MvcResult = mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/profile")
                .header("Authorization", "Bearer $token")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        org.junit.jupiter.api.Assertions.assertTrue(response.success)

        val updatedUser = userRepository.findById(testUser.id).orElse(null)
        org.junit.jupiter.api.Assertions.assertNotNull(updatedUser)
        org.junit.jupiter.api.Assertions.assertEquals("NewFirstName", updatedUser?.firstName)
        org.junit.jupiter.api.Assertions.assertEquals("User", updatedUser?.lastName)
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
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/profile")
                .header("Authorization", "Bearer $multiOrgToken")
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        org.junit.jupiter.api.Assertions.assertTrue(response.success)

        val dataJson = objectMapper.writeValueAsString(response.data)
        val profileResponse = objectMapper.readValue(dataJson, com.chronosync.dto.profile.ProfileResponse::class.java)

        org.junit.jupiter.api.Assertions.assertEquals("MEMBER", profileResponse.role)
        org.junit.jupiter.api.Assertions.assertEquals(3, profileResponse.organizations.size)

        val org1Role = profileResponse.organizations.find { it.name == "Profile Test Org 1" }?.role
        val org2Role = profileResponse.organizations.find { it.name == "Profile Test Org 2" }?.role
        val org3Role = profileResponse.organizations.find { it.name == "Third Org" }?.role

        org.junit.jupiter.api.Assertions.assertEquals("OWNER", org1Role)
        org.junit.jupiter.api.Assertions.assertEquals("MEMBER", org2Role)
        org.junit.jupiter.api.Assertions.assertEquals("OWNER", org3Role)
    }
}
