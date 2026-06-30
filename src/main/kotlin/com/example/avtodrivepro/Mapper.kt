package com.example.avtodrivepro

import org.springframework.stereotype.Component

@Component
class UserMapper {
    fun toResponse(entity: UserEntity): AdminResponse {
        return AdminResponse(
            id = entity.id!!,
            username = entity.username,
            role = entity.role,
            isActive = entity.isActive,
        )
    }
}

@Component
class StudentMapper(
    private val userRepository: UserRepository,
) {
    fun toEntity(body: StudentCreateRequest, createdByUser: UserEntity): StudentEntity {
        return StudentEntity(
            firstName = body.firstName,
            lastName = body.lastName,
            phoneNumber = body.phoneNumber?.ifBlank { null },
            createdByUser = createdByUser,
        )
    }

    fun toResponse(entity: StudentEntity): StudentResponse {
        return StudentResponse(
            id = entity.id!!,
            firstName = entity.firstName,
            lastName = entity.lastName,
            phoneNumber = entity.phoneNumber,
            photoUrl = entity.photoPath,
            status = entity.status,
            createdBy = entity.createdBy,
        )
    }
}
