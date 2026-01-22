package com.chronosync.controller

import com.chronosync.security.JwtUtil
import com.chronosync.entity.OrganizationRole
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TestJwtUtil {

    @Autowired
    private lateinit var jwtUtil: JwtUtil

    fun generateTestToken(
        userId: UUID = UUID.randomUUID(),
        email: String = "test@example.com",
        organizationId: UUID = UUID.randomUUID(),
        role: String = OrganizationRole.OWNER.name
    ): String {
        return jwtUtil.generateToken(userId, email, organizationId, role)
    }
}
