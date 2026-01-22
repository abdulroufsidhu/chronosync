package com.chronosync.repository

import com.chronosync.entity.User
import com.chronosync.entity.UserStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
    fun findByEmailAndStatus(email: String, status: UserStatus): User?
}
