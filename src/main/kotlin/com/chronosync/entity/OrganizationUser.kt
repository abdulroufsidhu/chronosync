package com.chronosync.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "organization_users",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "organization_id"])
    ]
)
data class OrganizationUser(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: OrganizationRole = OrganizationRole.MEMBER,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: OrganizationUserStatus = OrganizationUserStatus.ACTIVE,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    val updatedAt: Instant? = null
)

enum class OrganizationRole {
    OWNER,
    MANAGER,
    MEMBER
}

enum class OrganizationUserStatus {
    ACTIVE,
    INACTIVE,
    PENDING_INVITE
}
