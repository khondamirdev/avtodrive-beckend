package com.example.avtodrivepro

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
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsernameAndDeletedFalse(username)
            ?: throw UserNotFoundException()

        val tokens = jwtTokenRepository.findAllByUserIdAndRevokedFalse(user.id!!)
        tokens.forEach { it.revoked = true }
        jwtTokenRepository.saveAll(tokens)
    }
}

// ─────────────────────────────────────────────
// ADMIN SERVICE
// ─────────────────────────────────────────────
interface AdminService {
    fun addAdmin(body: AdminCreateRequest)
}

@Service
class AdminServiceImpl(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
    private val passwordEncoder: PasswordEncoder,
) : AdminService {

    @Transactional
    override fun addAdmin(body: AdminCreateRequest) {
        if (userRepository.existsByUsernameAndDeletedFalse(body.username))
            throw UserAlreadyExistsException()

        val user = UserEntity(
            username = body.username,
            password = passwordEncoder.encode(body.password),
            role = UserRole.ADMIN,
        )

        userRepository.save(user)
    }
}

// ─────────────────────────────────────────────
// STUDENT SERVICE
// ─────────────────────────────────────────────
interface StudentService {
    fun create(body: StudentCreateRequest, photo: MultipartFile?): StudentResponse
    fun getAll(status: StudentStatus, page: Int, size: Int): Page<StudentResponse>
    fun search(firstName: String, lastName: String, page: Int, size: Int): Page<StudentResponse>
    fun updateStatus(id: Long, body: StudentStatusUpdateRequest)
    fun delete(id: Long)
    fun update(id: Long, body: StudentUpdateRequest, photo: MultipartFile?): StudentResponse
}

@Service
class StudentServiceImpl(
    private val studentRepository: StudentRepository,
    private val userRepository: UserRepository,
    private val studentMapper: StudentMapper,
    private val fileService: FileService,
) : StudentService {

    @Transactional
    override fun create(body: StudentCreateRequest, photo: MultipartFile?): StudentResponse {
        val phone = body.phoneNumber?.ifBlank { null }

        if (phone != null && studentRepository.existsByPhoneNumberAndDeletedFalse(phone))
            throw StudentPhoneAlreadyExistsException()

        val userId = SecurityContextHolder.getContext().authentication.principal as Long
        val currentUser = userRepository.findByIdAndDeletedFalse(userId)
            ?: throw UserNotFoundException()

        val student = studentMapper.toEntity(body, currentUser)
        student.phoneNumber = phone
        photo?.let { student.photoPath = fileService.save(it) }

        return studentMapper.toResponse(studentRepository.save(student))
    }

    override fun getAll(status: StudentStatus, page: Int, size: Int): Page<StudentResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"))
        return studentRepository.findAllByStatusAndDeletedFalse(status, pageable)
            .map { studentMapper.toResponse(it) }
    }

    override fun search(firstName: String, lastName: String, page: Int, size: Int): Page<StudentResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"))
        return studentRepository
            .findAllByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCaseAndDeletedFalse(
                firstName, lastName, pageable
            )
            .map { studentMapper.toResponse(it) }
    }

    @Transactional
    override fun updateStatus(id: Long, body: StudentStatusUpdateRequest) {
        val student = studentRepository.findByIdAndDeletedFalse(id)
            ?: throw StudentNotFoundException()
        student.status = body.status
        studentRepository.save(student)
    }

    override fun delete(id: Long) {
        studentRepository.findByIdAndDeletedFalse(id)
            ?: throw StudentNotFoundException()
        studentRepository.trash(id)
    }

    @Transactional
    override fun update(id: Long, body: StudentUpdateRequest, photo: MultipartFile?): StudentResponse {
        val student = studentRepository.findByIdAndDeletedFalse(id)
            ?: throw StudentNotFoundException()

        body.firstName?.let { if (it.isNotBlank()) student.firstName = it }
        body.lastName?.let { if (it.isNotBlank()) student.lastName = it }
        body.phoneNumber?.let {
            if (it.isNotBlank() && it != student.phoneNumber &&
                studentRepository.existsByPhoneNumberAndDeletedFalse(it))
                throw StudentPhoneAlreadyExistsException()
            student.phoneNumber = it.ifBlank { null }
        }
        photo?.let { student.photoPath = fileService.save(it) }

        return studentMapper.toResponse(studentRepository.save(student))
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
    private val allowedTypes = setOf("image/jpeg", "image/png")

    override fun save(file: MultipartFile): String {
        if (file.size > maxSize)
            throw FileTooLargeException()

        if (file.contentType !in allowedTypes)
            throw FileTypeNotSupportedException()

        val extension = file.originalFilename?.substringAfterLast(".") ?: "jpg"
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

//             Profil service

interface ProfileService {
    fun getProfile(): ProfileResponse
    fun changePassword(body: ChangePasswordRequest)
}

@Service
class ProfileServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : ProfileService {

    override fun getProfile(): ProfileResponse {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long
        val user = userRepository.findByIdAndDeletedFalse(userId)
            ?: throw UserNotFoundException()
        return ProfileResponse(id = user.id!!, username = user.username, role = user.role)
    }

    override fun changePassword(body: ChangePasswordRequest) {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long
        val user = userRepository.findByIdAndDeletedFalse(userId)
            ?: throw UserNotFoundException()
        if (!passwordEncoder.matches(body.currentPassword, user.password))
            throw InvalidCredentialsException()
        user.password = passwordEncoder.encode(body.newPassword)
        userRepository.save(user)
    }
}
