package com.example.avtodrivepro

import org.springframework.stereotype.Component

@Component
class UserMapper {
    fun toAdminResponse(entity: UserEntity, studentCount: Long): AdminResponse {
        return AdminResponse(
            id = entity.id!!,
            username = entity.username,
            role = entity.role,
            isActive = entity.isActive,
            studentCount = studentCount,
        )
    }

    fun toProfileResponse(entity: UserEntity): ProfileResponse {
        return ProfileResponse(
            id = entity.id!!,
            username = entity.username,
            role = entity.role,
        )
    }
}

@Component
class StudentMapper {
    fun toEntity(body: StudentCreateRequest, createdByUser: UserEntity): StudentEntity {
        return StudentEntity(
            firstName = body.firstName,
            lastName = body.lastName,
            phoneNumber = body.phoneNumber?.ifBlank { null },
            totalAmount = body.totalAmount,
            createdByUser = createdByUser,
        )
    }

    fun toResponse(entity: StudentEntity, paidAmount: Long): StudentResponse {
        return StudentResponse(
            id = entity.id!!,
            firstName = entity.firstName,
            lastName = entity.lastName,
            phoneNumber = entity.phoneNumber,
            passportPhotoUrl = entity.passportPhotoPath,
            form083Url = entity.form083Path,
            status = entity.status,
            totalAmount = entity.totalAmount,
            paidAmount = paidAmount,
            remainingAmount = entity.totalAmount - paidAmount,
            createdBy = entity.createdBy,
            createdByUsername = entity.createdByUser.username,
            createdDate = entity.createdDate,
        )
    }
}

@Component
class PaymentMapper {
    fun toEntity(body: PaymentCreateRequest, student: StudentEntity): PaymentEntity {
        return PaymentEntity(
            student = student,
            amount = body.amount,
            note = body.note,
        )
    }

    fun toResponse(entity: PaymentEntity): PaymentResponse {
        return PaymentResponse(
            id = entity.id!!,
            amount = entity.amount,
            note = entity.note,
            createdDate = entity.createdDate,
        )
    }
}