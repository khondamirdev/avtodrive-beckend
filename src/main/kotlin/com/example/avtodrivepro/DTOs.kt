package com.example.avtodrivepro

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class BaseMessage(
    val code: Long? = null,
    val message: String? = null
)

// ─────────────────────────────────────────────
// AUTH
// ─────────────────────────────────────────────
data class LoginRequest(
    @field:NotBlank(message = "Username cannot be blank")
    val username: String,

    @field:NotBlank(message = "Password cannot be blank")
    val password: String,
)

data class LoginResponse(
    val token: String,
    val role: UserRole,
)

// ─────────────────────────────────────────────
// ADMIN
// ─────────────────────────────────────────────
data class AdminCreateRequest(
    @field:NotBlank(message = "Username cannot be blank")
    val username: String,

    @field:NotBlank(message = "Password cannot be blank")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    val password: String,
)

data class AdminResponse(
    val id: Long,
    val username: String,
    val role: UserRole,
    val isActive: Boolean,
)

// ─────────────────────────────────────────────
// STUDENT
// ─────────────────────────────────────────────
data class StudentCreateRequest(
    @field:NotBlank(message = "First name cannot be blank")
    val firstName: String,

    @field:NotBlank(message = "Last name cannot be blank")
    val lastName: String,

    @field:Pattern(
        regexp = "^\\+998\\d{9}$",
        message = "Phone number must be in +998XXXXXXXXX format"
    )
    val phoneNumber: String? = null,
)

data class StudentResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String?,
    val photoUrl: String?,
    val status: StudentStatus,
    val createdBy: Long?,
)

data class StudentStatusUpdateRequest(
    val status: StudentStatus,
)

data class StudentUpdateRequest(
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: String?,
)