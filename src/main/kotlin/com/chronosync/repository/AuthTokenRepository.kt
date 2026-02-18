package com.chronosync.repository

import com.chronosync.entity.AuthToken
import com.chronosync.entity.AuthTokenType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AuthTokenRepository : JpaRepository<AuthToken, UUID> {
    fun findByToken(token: String): AuthToken?
    fun findByTokenAndType(token: String, type: AuthTokenType): AuthToken?
    fun deleteByUserIdAndType(userId: UUID, type: AuthTokenType)
}
