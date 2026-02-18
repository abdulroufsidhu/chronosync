package com.chronosync.controller

import com.chronosync.dto.common.ApiResponse
import com.chronosync.dto.organization.*
import com.chronosync.security.CurrentUser
import com.chronosync.security.UserPrincipal
import com.chronosync.service.OrganizationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/organization")
class OrganizationController(
    private val organizationService: OrganizationService
) {

    @GetMapping("/users")
    fun getUsers(@CurrentUser principal: UserPrincipal): ResponseEntity<ApiResponse<List<UserListDto>>> {
        return try {
            val response = organizationService.getOrganizationUsers(principal)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @GetMapping("/users/dropdown")
    fun getUserDropdowns(@CurrentUser principal: UserPrincipal): ResponseEntity<ApiResponse<List<UserDropdownDto>>> {
        return try {
            val response = organizationService.getUserDropdowns(principal)
            ResponseEntity.ok(ApiResponse(success = true, data = response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @PostMapping("/invite")
    fun inviteUser(
        @CurrentUser principal: UserPrincipal,
        @RequestBody request: InviteUserRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            organizationService.inviteUser(principal, request)
            ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse(success = true, message = "Invitation sent successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @DeleteMapping("/users/{id}")
    fun removeUser(
        @CurrentUser principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            organizationService.removeUser(principal, id)
            ResponseEntity.ok(ApiResponse(success = true, message = "User removed successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }

    @PutMapping("/users/{id}/role")
    fun updateUserRole(
        @CurrentUser principal: UserPrincipal,
        @PathVariable id: UUID,
        @RequestBody request: UpdateUserRoleRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            organizationService.updateUserRole(principal, id, request)
            ResponseEntity.ok(ApiResponse(success = true, message = "User role updated successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, message = e.message))
        }
    }
}
