package com.chronosync.controller

import com.chronosync.config.TestSecurityConfig
import com.chronosync.dto.auth.*
import com.chronosync.entity.*
import com.chronosync.repository.AuthTokenRepository
import com.chronosync.repository.OrganizationRepository
import com.chronosync.repository.OrganizationUserRepository
import com.chronosync.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
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

    @Autowired
    private lateinit var authTokenRepository: AuthTokenRepository

    @BeforeEach
    fun setUp() {
        authTokenRepository.deleteAll()
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
            post("/api/auth/login")
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
        assertNotNull(response.data)
    }

    @Test
    fun `login with invalid credentials returns bad request`() {
        val request = LoginRequest(
            email = "nonexistent@example.com",
            password = "wrongpassword"
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(
            status().isBadRequest
        )
    }

    @Test
    fun `register creates new organization and user`() {
        val request = RegisterRequest(
            email = "newuser@example.com",
            password = "password123",
            organization = OrganizationInfoRegisterDto(
                name = "New Organization",
                services = listOf("cutting", "beard"),
                type = "salon",
                role = "OWNER"
            )
        )

        val result: MvcResult = mockMvc.perform(
            post("/api/auth/register")
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
        assertNotNull(response.data)

        val data = objectMapper.writeValueAsString(response.data)
        val authResponse = objectMapper.readValue(data, AuthResponse::class.java)
        assertEquals("newuser@example.com", authResponse.user.email)
        assertEquals("New Organization", authResponse.organization.name)
        assertEquals("OWNER", authResponse.organization.role)
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
            organization = OrganizationInfoRegisterDto(
                name = "Organization",
                services = emptyList(),
                type = "salon",
                role = "OWNER"
            )
        )

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(
            status().isBadRequest
        )
    }

    @Test
    fun `forgot password returns success for existing email`() {
        val user = User(
            email = "forgot@example.com",
            password = "password123",
            status = UserStatus.ACTIVE
        )
        userRepository.save(user)

        val request = ForgotPasswordRequest(email = "forgot@example.com")

        val result: MvcResult = mockMvc.perform(
            post("/api/auth/forgot-password")
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

        // Verify token was created
        val tokens = authTokenRepository.findAll()
        assertEquals(1, tokens.size)
        assertEquals(AuthTokenType.PASSWORD_RESET, tokens[0].type)
    }

    @Test
    fun `forgot password returns success for non-existent email`() {
        val request = ForgotPasswordRequest(email = "nonexistent@example.com")

        val result: MvcResult = mockMvc.perform(
            post("/api/auth/forgot-password")
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

        // Verify no token was created
        val tokens = authTokenRepository.findAll()
        assertEquals(0, tokens.size)
    }

    @Test
    fun `reset password with valid token returns success`() {
        val user = User(
            email = "reset@example.com",
            password = BCryptPasswordEncoder().encode("oldpassword"),
            status = UserStatus.ACTIVE
        )
        val savedUser = userRepository.save(user)

        val organization = Organization(
            name = "Reset Test Org",
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

        val token = UUID.randomUUID().toString()
        val authToken = AuthToken(
            user = savedUser,
            token = token,
            type = AuthTokenType.PASSWORD_RESET,
            expiresAt = Instant.now().plus(1, ChronoUnit.HOURS)
        )
        authTokenRepository.save(authToken)

        val request = ResetPasswordRequest(
            token = token,
            newPassword = "newpassword123"
        )

        val result: MvcResult = mockMvc.perform(
            post("/api/auth/reset-password")
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
        assertNotNull(response.data)

        // Verify token is marked as used
        val usedToken = authTokenRepository.findByToken(token)
        assertNotNull(usedToken?.usedAt)
    }

    @Test
    fun `reset password with invalid token returns bad request`() {
        val request = ResetPasswordRequest(
            token = "invalid-token",
            newPassword = "newpassword123"
        )

        mockMvc.perform(
            post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(
            status().isBadRequest
        )
    }

    @Test
    fun `request magic link returns success for existing email`() {
        val user = User(
            email = "magic@example.com",
            password = "password123",
            status = UserStatus.ACTIVE
        )
        userRepository.save(user)

        val request = MagicLinkRequest(email = "magic@example.com")

        val result: MvcResult = mockMvc.perform(
            post("/api/auth/magic-link")
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

        // Verify magic link token was created
        val tokens = authTokenRepository.findAll()
        assertEquals(1, tokens.size)
        assertEquals(AuthTokenType.MAGIC_LINK, tokens[0].type)
    }

    @Test
    fun `request magic link returns success for non-existent email`() {
        val request = MagicLinkRequest(email = "nonexistent@example.com")

        val result: MvcResult = mockMvc.perform(
            post("/api/auth/magic-link")
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

        // Verify no token was created
        val tokens = authTokenRepository.findAll()
        assertEquals(0, tokens.size)
    }

    @Test
    fun `verify magic link with valid token returns auth response`() {
        val user = User(
            email = "verifymagic@example.com",
            password = "password123",
            status = UserStatus.ACTIVE
        )
        val savedUser = userRepository.save(user)

        val organization = Organization(
            name = "Magic Test Org",
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

        val token = UUID.randomUUID().toString()
        val authToken = AuthToken(
            user = savedUser,
            token = token,
            type = AuthTokenType.MAGIC_LINK,
            expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES)
        )
        authTokenRepository.save(authToken)

        val result: MvcResult = mockMvc.perform(
            get("/api/auth/magic-link/verify")
                .param("token", token)
        ).andExpect(
            status().isOk
        ).andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            com.chronosync.dto.common.ApiResponse::class.java
        )

        assertTrue(response.success)
        assertNotNull(response.data)

        val data = objectMapper.writeValueAsString(response.data)
        val authResponse = objectMapper.readValue(data, AuthResponse::class.java)
        assertEquals("verifymagic@example.com", authResponse.user.email)
        assertNotNull(authResponse.accessToken)

        // Verify token is marked as used
        val usedToken = authTokenRepository.findByToken(token)
        assertNotNull(usedToken?.usedAt)
    }

    @Test
    fun `verify magic link with invalid token returns bad request`() {
        mockMvc.perform(
            get("/api/auth/magic-link/verify")
                .param("token", "invalid-token")
        ).andExpect(
            status().isBadRequest
        )
    }

    @Test
    fun `verify magic link with expired token returns bad request`() {
        val user = User(
            email = "expired@example.com",
            password = "password123",
            status = UserStatus.ACTIVE
        )
        val savedUser = userRepository.save(user)

        val token = UUID.randomUUID().toString()
        val authToken = AuthToken(
            user = savedUser,
            token = token,
            type = AuthTokenType.MAGIC_LINK,
            expiresAt = Instant.now().minus(1, ChronoUnit.MINUTES) // Expired
        )
        authTokenRepository.save(authToken)

        mockMvc.perform(
            get("/api/auth/magic-link/verify")
                .param("token", token)
        ).andExpect(
            status().isBadRequest
        )
    }

    @Test
    fun `verify magic link with used token returns bad request`() {
        val user = User(
            email = "used@example.com",
            password = "password123",
            status = UserStatus.ACTIVE
        )
        val savedUser = userRepository.save(user)

        val token = UUID.randomUUID().toString()
        val authToken = AuthToken(
            user = savedUser,
            token = token,
            type = AuthTokenType.MAGIC_LINK,
            expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES),
            usedAt = Instant.now() // Already used
        )
        authTokenRepository.save(authToken)

        mockMvc.perform(
            get("/api/auth/magic-link/verify")
                .param("token", token)
        ).andExpect(
            status().isBadRequest
        )
    }

    @Test
    fun `register with timezone creates organization with timezone`() {
        val request = RegisterRequest(
            email = "timezoneuser@example.com",
            password = "password123",
            organization = OrganizationInfoRegisterDto(
                name = "Timezone Organization",
                services = listOf("haircut"),
                type = "salon",
                role = "OWNER",
                timezone = "America/New_York"
            )
        )

        val result: MvcResult = mockMvc.perform(
            post("/api/auth/register")
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

        val data = objectMapper.writeValueAsString(response.data)
        val authResponse = objectMapper.readValue(data, AuthResponse::class.java)
        assertEquals("America/New_York", authResponse.organization.timezone)

        // Verify in database
        val org = organizationRepository.findAll().first()
        assertEquals("America/New_York", org.timezone)
    }

    @Test
    fun `register without timezone defaults to UTC`() {
        val request = RegisterRequest(
            email = "utcuser@example.com",
            password = "password123",
            organization = OrganizationInfoRegisterDto(
                name = "UTC Organization",
                services = listOf("haircut"),
                type = "salon",
                role = "OWNER"
            )
        )

        val result: MvcResult = mockMvc.perform(
            post("/api/auth/register")
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

        val data = objectMapper.writeValueAsString(response.data)
        val authResponse = objectMapper.readValue(data, AuthResponse::class.java)
        assertEquals("UTC", authResponse.organization.timezone)
    }
}
