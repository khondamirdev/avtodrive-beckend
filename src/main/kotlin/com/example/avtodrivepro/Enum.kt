package com.example.avtodrivepro

enum class UserRole {
    SUPER_ADMIN, ADMIN
}

enum class StudentStatus {
    REGISTERED, STUDYING
}

enum class ErrorCode(val code: Long) {
    // Auth
    INVALID_CREDENTIALS(1000),
    TOKEN_EXPIRED(1001),
    TOKEN_REVOKED(1002),
    ACCESS_DENIED(1003),

    // User / Admin
    USER_NOT_FOUND(2000),
    USER_ALREADY_EXISTS(2001),

    // Student
    STUDENT_NOT_FOUND(3000),
    STUDENT_PHONE_ALREADY_EXISTS(3001),

    // File
    FILE_TOO_LARGE(4000),
    FILE_TYPE_NOT_SUPPORTED(4001),
    FILE_UPLOAD_FAILED(4002),
}
