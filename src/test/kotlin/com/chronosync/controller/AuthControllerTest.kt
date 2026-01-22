package com.chronosync.controller

import com.chronosync.dto.auth.LoginRequest
import com.chronosync.dto.auth.RegisterRequest
import com.chronosync.dto.auth.OrganizationInfo
import com.chronosync.entity.Organization
import com.chronosync.entity.OrganizationRole
import com.chronosync.entity.OrganizationUser
import com.chronosync.entity.OrganizationUserStatus
import com.chronosync.entity.Plan
import com.chronosync.entity.User
import com.chronosync.entity.UserStatus
import com.chronosync.repository.OrganizationRepository
import com.chronosync.repository.OrganizationUserRepository
import com.chronosync.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var organizationUserRepository: OrganizationUserRepository

    @BeforeEach
    fun setUp() {
        organizationUserRepository.deleteAll()
        organizationRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `login with valid credentials returns success`() {
        val password = "password123"
        val user = User(
            email = "test@example.com",
            password = BCryptPasswordEncoder().encode(password),
            status = UserStatus.ACTIVE
        )
        val savedUser = userRepository.save(user)

        val organization = Organization(
            name = "Test Organization",
            plan = Plan.FREE,
            scheduleLimit = 100,
            currentUsage = 0,
            nextReset = Instant.now().plus(30, ChronoUnit.DAYS)
        )
        val savedOrg = organizationRepository.save(organization)

        val orgUser = OrganizationUser(
            user = savedUser,
            organization = savedOrg,
            role = OrganizationRole.OWNER,
            status = OrganizationUserStatus.ACTIVE
        )
        organizationUserRepository.save(orgUser)

        val request = LoginRequest(
            email = "test@example.com",
            password = password
        )

        val result: MvcResult = mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
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
        org.junit.jupiter.api.Assertions.assertNotNull(response.data)
    }

    @Test
    fun `login with invalid credentials returns bad request`() {
        val request = LoginRequest(
            email = "nonexistent@example.com",
            password = "wrongpassword"
        )

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest
        )
    }

    @Test
    fun `register creates new organization and user`() {
        val request = RegisterRequest(
            email = "newuser@example.com",
            password = "password123",
            organization = OrganizationInfo(
                name = "New Organization",
                services = listOf("cutting", "beard"),
                type = "salon",
                role = "OWNER"
            )
        )

        val result: MvcResult = mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/register")
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
        org.junit.jupiter.api.Assertions.assertNotNull(response.data)

        val data = objectMapper.writeValueAsString(response.data)
        val authResponse = objectMapper.readValue(data, com.chronosync.dto.auth.AuthResponse::class.java)
        org.junit.jupiter.api.Assertions.assertEquals("newuser@example.com", authResponse.user.email)
        org.junit.jupiter.api.Assertions.assertEquals("New Organization", authResponse.organization.name)
        org.junit.jupiter.api.Assertions.assertEquals("OWNER", authResponse.organization.role)
    }

    @Test
    fun `register with existing email returns bad request`() {
        val existingUser = User(
            email = "existing@example.com",
            password = "password123",
            status = UserStatus.ACTIVE
        )
        userRepository.save(existingUser)

        val request = RegisterRequest(
            email = "existing@example.com",
            password = "password123",
            organization = OrganizationInfo(
                name = "Organization",
                services = emptyList(),
                type = "salon",
                role = "OWNER"
            )
        )

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest
        )
    }
}
