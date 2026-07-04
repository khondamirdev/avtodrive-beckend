package com.example.avtodrivepro.service

import com.example.avtodrivepro.*
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

// ─────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────
fun currentUserId(): Long =
    SecurityContextHolder.getContext().authentication.principal as Long

fun currentUserRole(userRepository: UserRepository): UserRole {
    val user = userRepository.findByIdAndDeletedFalse(currentUserId())
        ?: throw UserNotFoundException()
    return user.role
}

// ─────────────────────────────────────────────
// AUTH SERVICE
// ─────────────────────────────────────────────
interface AuthService {
    fun login(body: LoginRequest): LoginResponse
    fun logout()
}

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val jwtTokenRepository: JwtTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) : AuthService {

    override fun login(body: LoginRequest): LoginResponse {
        val user = userRepository.findByUsernameAndDeletedFalse(body.username)
            ?: throw InvalidCredentialsException()
        if (!passwordEncoder.matches(body.password, user.password))
            throw InvalidCredentialsException()

        val token = jwtService.generateToken(user)
        jwtTokenRepository.save(
            JwtTokenEntity(
                user = user,
                tokenValue = token,
                expiresAt = jwtService.extractExpiration(token),
            )
        )
        return LoginResponse(token = token, role = user.role)
    }

    @Transactional
    override fun logout() {
        val user = userRepository.findByIdAndDeletedFalse(currentUserId())
            ?: throw UserNotFoundException()
        val tokens = jwtTokenRepository.findAllByUserIdAndRevokedFalse(user.id!!)
        tokens.forEach { it.revoked = true }
        jwtTokenRepository.saveAll(tokens)
    }
}

// ─────────────────────────────────────────────
// PROFILE SERVICE
// ─────────────────────────────────────────────
interface ProfileService {
    fun getProfile(): ProfileResponse
    fun changePassword(body: ChangePasswordRequest)
}

@Service
class ProfileServiceImpl(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
    private val passwordEncoder: PasswordEncoder,
) : ProfileService {

    override fun getProfile(): ProfileResponse {
        val user = userRepository.findByIdAndDeletedFalse(currentUserId())
            ?: throw UserNotFoundException()
        return userMapper.toProfileResponse(user)
    }

    @Transactional
    override fun changePassword(body: ChangePasswordRequest) {
        val user = userRepository.findByIdAndDeletedFalse(currentUserId())
            ?: throw UserNotFoundException()
        if (!passwordEncoder.matches(body.currentPassword, user.password))
            throw InvalidCredentialsException()
        user.password = passwordEncoder.encode(body.newPassword)
        userRepository.save(user)
    }
}

// ─────────────────────────────────────────────
// ADMIN SERVICE
// ─────────────────────────────────────────────
interface AdminService {
    fun addAdmin(body: AdminCreateRequest)
    fun getAllAdmins(): List<AdminResponse>
    fun getAdminStudents(adminId: Long, page: Int, size: Int): Page<StudentResponse>
}

@Service
class AdminServiceImpl(
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository,
    private val paymentRepository: PaymentRepository,
    private val userMapper: UserMapper,
    private val studentMapper: StudentMapper,
    private val passwordEncoder: PasswordEncoder,
) : AdminService {

    @Transactional
    override fun addAdmin(body: AdminCreateRequest) {
        if (userRepository.existsByUsernameAndDeletedFalse(body.username))
            throw UserAlreadyExistsException()
        userRepository.save(
            UserEntity(
                username = body.username,
                password = passwordEncoder.encode(body.password),
                role = UserRole.ADMIN,
            )
        )
    }

    override fun getAllAdmins(): List<AdminResponse> {
        return userRepository.findAllByRoleAndDeletedFalse(UserRole.ADMIN).map { admin ->
            val count = studentRepository.countByCreatedByUserIdAndDeletedFalse(admin.id!!)
            userMapper.toAdminResponse(admin, count)
        }
    }

    override fun getAdminStudents(adminId: Long, page: Int, size: Int): Page<StudentResponse> {
        userRepository.findByIdAndDeletedFalse(adminId) ?: throw UserNotFoundException()
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"))
        return studentRepository.findAllByCreatedByUserIdAndDeletedFalse(adminId, pageable)
            .map { student ->
                val paid = paymentRepository.sumAmountByStudentId(student.id!!)
                studentMapper.toResponse(student, paid)
            }
    }
}

// ─────────────────────────────────────────────
// STUDENT SERVICE
// ─────────────────────────────────────────────
interface StudentService {
    fun create(body: StudentCreateRequest, passportPhoto: MultipartFile?, form083: MultipartFile?): StudentResponse
    fun getAll(status: StudentStatus, page: Int, size: Int): Page<StudentResponse>
    fun getOne(id: Long): StudentResponse
    fun search(firstName: String, lastName: String, page: Int, size: Int): Page<StudentResponse>
    fun update(id: Long, body: StudentUpdateRequest, passportPhoto: MultipartFile?, form083: MultipartFile?): StudentResponse
    fun updateStatus(id: Long, body: StudentStatusUpdateRequest)
    fun delete(id: Long)
}

@Service
class StudentServiceImpl(
    private val studentRepository: StudentRepository,
    private val userRepository: UserRepository,
    private val paymentRepository: PaymentRepository,
    private val studentMapper: StudentMapper,
    private val fileService: FileService,
) : StudentService {

    @Transactional
    override fun create(
        body: StudentCreateRequest,
        passportPhoto: MultipartFile?,
        form083: MultipartFile?,
    ): StudentResponse {
        val phone = body.phoneNumber?.ifBlank { null }
        if (phone != null && studentRepository.existsByPhoneNumberAndDeletedFalse(phone))
            throw StudentPhoneAlreadyExistsException()

        val currentUser = userRepository.findByIdAndDeletedFalse(currentUserId())
            ?: throw UserNotFoundException()

        val student = studentMapper.toEntity(body, currentUser)
        passportPhoto?.let { student.passportPhotoPath = fileService.save(it) }
        form083?.let { student.form083Path = fileService.save(it) }

        val saved = studentRepository.save(student)
        return studentMapper.toResponse(saved, 0L)
    }

    override fun getAll(status: StudentStatus, page: Int, size: Int): Page<StudentResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"))
        val userId = currentUserId()
        val user = userRepository.findByIdAndDeletedFalse(userId) ?: throw UserNotFoundException()

        val students = if (user.role == UserRole.SUPER_ADMIN) {
            studentRepository.findAllByStatusAndDeletedFalse(status, pageable)
        } else {
            studentRepository.findAllByStatusAndCreatedByUserIdAndDeletedFalse(status, userId, pageable)
        }

        return students.map { student ->
            val paid = paymentRepository.sumAmountByStudentId(student.id!!)
            studentMapper.toResponse(student, paid)
        }
    }

    override fun getOne(id: Long): StudentResponse {
        val student = studentRepository.findByIdAndDeletedFalse(id)
            ?: throw StudentNotFoundException()
        checkAccess(student)
        val paid = paymentRepository.sumAmountByStudentId(student.id!!)
        return studentMapper.toResponse(student, paid)
    }

    override fun search(firstName: String, lastName: String, page: Int, size: Int): Page<StudentResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"))
        val userId = currentUserId()
        val user = userRepository.findByIdAndDeletedFalse(userId) ?: throw UserNotFoundException()

        val students = if (user.role == UserRole.SUPER_ADMIN) {
            studentRepository
                .findAllByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCaseAndDeletedFalse(
                    firstName, lastName, pageable
                )
        } else {
            studentRepository
                .findAllByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCaseAndCreatedByUserIdAndDeletedFalse(
                    firstName, lastName, userId, pageable
                )
        }

        return students.map { student ->
            val paid = paymentRepository.sumAmountByStudentId(student.id!!)
            studentMapper.toResponse(student, paid)
        }
    }

    @Transactional
    override fun update(
        id: Long,
        body: StudentUpdateRequest,
        passportPhoto: MultipartFile?,
        form083: MultipartFile?,
    ): StudentResponse {
        val student = studentRepository.findByIdAndDeletedFalse(id)
            ?: throw StudentNotFoundException()
        checkAccess(student)

        body.firstName?.let { if (it.isNotBlank()) student.firstName = it }
        body.lastName?.let { if (it.isNotBlank()) student.lastName = it }
        body.phoneNumber?.let {
            val phone = it.ifBlank { null }
            if (phone != null && phone != student.phoneNumber &&
                studentRepository.existsByPhoneNumberAndDeletedFalse(phone)
            ) throw StudentPhoneAlreadyExistsException()
            student.phoneNumber = phone
        }
        body.totalAmount?.let { student.totalAmount = it }
        passportPhoto?.let { student.passportPhotoPath = fileService.save(it) }
        form083?.let { student.form083Path = fileService.save(it) }

        val saved = studentRepository.save(student)
        val paid = paymentRepository.sumAmountByStudentId(saved.id!!)
        return studentMapper.toResponse(saved, paid)
    }

    @Transactional
    override fun updateStatus(id: Long, body: StudentStatusUpdateRequest) {
        val user = userRepository.findByIdAndDeletedFalse(currentUserId())
            ?: throw UserNotFoundException()
        if (user.role != UserRole.SUPER_ADMIN) throw AccessDeniedException()

        val student = studentRepository.findByIdAndDeletedFalse(id)
            ?: throw StudentNotFoundException()
        student.status = body.status
        studentRepository.save(student)
    }

    override fun delete(id: Long) {
        val student = studentRepository.findByIdAndDeletedFalse(id)
            ?: throw StudentNotFoundException()
        checkAccess(student)
        studentRepository.trash(id)
    }

    private fun checkAccess(student: StudentEntity) {
        val userId = currentUserId()
        val user = userRepository.findByIdAndDeletedFalse(userId) ?: throw UserNotFoundException()
        if (user.role != UserRole.SUPER_ADMIN && student.createdByUser.id != userId)
            throw AccessDeniedException()
    }
}

// ─────────────────────────────────────────────
// PAYMENT SERVICE
// ─────────────────────────────────────────────
interface PaymentService {
    fun addPayment(studentId: Long, body: PaymentCreateRequest): PaymentResponse
    fun getPayments(studentId: Long): List<PaymentResponse>
    fun deletePayment(paymentId: Long)
}

@Service
class PaymentServiceImpl(
    private val paymentRepository: PaymentRepository,
    private val studentRepository: StudentRepository,
    private val userRepository: UserRepository,
    private val paymentMapper: PaymentMapper,
) : PaymentService {

    @Transactional
    override fun addPayment(studentId: Long, body: PaymentCreateRequest): PaymentResponse {
        if (body.amount <= 0) throw InvalidPaymentAmountException()

        val student = studentRepository.findByIdAndDeletedFalse(studentId)
            ?: throw StudentNotFoundException()
        checkAccess(student)

        val payment = paymentMapper.toEntity(body, student)
        return paymentMapper.toResponse(paymentRepository.save(payment))
    }

    override fun getPayments(studentId: Long): List<PaymentResponse> {
        val student = studentRepository.findByIdAndDeletedFalse(studentId)
            ?: throw StudentNotFoundException()
        checkAccess(student)
        return paymentRepository.findAllByStudentIdAndDeletedFalse(studentId)
            .map { paymentMapper.toResponse(it) }
    }

    @Transactional
    override fun deletePayment(paymentId: Long) {
        val payment = paymentRepository.findByIdAndDeletedFalse(paymentId)
            ?: throw PaymentNotFoundException()
        checkAccess(payment.student)
        paymentRepository.trash(paymentId)
    }

    private fun checkAccess(student: StudentEntity) {
        val userId = currentUserId()
        val user = userRepository.findByIdAndDeletedFalse(userId) ?: throw UserNotFoundException()
        if (user.role != UserRole.SUPER_ADMIN && student.createdByUser.id != userId)
            throw AccessDeniedException()
    }
}

// ─────────────────────────────────────────────
// FILE SERVICE
// ─────────────────────────────────────────────
interface FileService {
    fun save(file: MultipartFile): String
}

@Service
class FileServiceImpl : FileService {
    private val uploadDir = "uploads/"
    private val maxSize = 5 * 1024 * 1024L
    private val allowedTypes = setOf("image/jpeg", "image/png", "application/pdf")

    override fun save(file: MultipartFile): String {
        if (file.size > maxSize) throw FileTooLargeException()
        if (file.contentType !in allowedTypes) throw FileTypeNotSupportedException()

        val extension = file.originalFilename?.substringAfterLast(".") ?: "bin"
        val fileName = "${UUID.randomUUID()}.$extension"
        val targetPath = Paths.get(uploadDir, fileName)

        try {
            Files.createDirectories(targetPath.parent)
            Files.copy(file.inputStream, targetPath)
        } catch (e: Exception) {
            throw FileUploadFailedException()
        }

        return "$uploadDir$fileName"
    }
}