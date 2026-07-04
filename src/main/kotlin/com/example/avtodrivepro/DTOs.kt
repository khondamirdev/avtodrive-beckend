package com.example.avtodrivepro

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.Date

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
// PROFILE
// ─────────────────────────────────────────────
data class ProfileResponse(
    val id: Long,
    val username: String,
    val role: UserRole,
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Current password cannot be blank")
    val currentPassword: String,

    @field:NotBlank(message = "New password cannot be blank")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    val newPassword: String,
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
    val studentCount: Long,
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

    val totalAmount: Long = 0,
)

data class StudentUpdateRequest(
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: String?,
    val totalAmount: Long?,
)

data class StudentResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String?,
    val passportPhotoUrl: String?,
    val form083Url: String?,
    val status: StudentStatus,
    val totalAmount: Long,
    val paidAmount: Long,
    val remainingAmount: Long,
    val createdBy: Long?,
    val createdByUsername: String?,
    val createdDate: Date?,
)

data class StudentStatusUpdateRequest(
    val status: StudentStatus,
)

// ─────────────────────────────────────────────
// PAYMENT
// ─────────────────────────────────────────────
data class PaymentCreateRequest(
    @field:NotNull(message = "Amount cannot be null")
    val amount: Long,

    val note: String? = null,
)

data class PaymentResponse(
    val id: Long,
    val amount: Long,
    val note: String?,
    val createdDate: Date?,
)