package com.example.avtodrivepro

import com.example.avtodrivepro.service.*
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    fun login(@RequestBody body: LoginRequest) = authService.login(body)

    @PostMapping("/logout")
    fun logout() = authService.logout()
}

@RestController
@RequestMapping("/api/profile")
class ProfileController(private val profileService: ProfileService) {

    @GetMapping
    fun getProfile() = profileService.getProfile()

    @PatchMapping("/password")
    fun changePassword(@RequestBody body: ChangePasswordRequest) =
        profileService.changePassword(body)
}

@RestController
@RequestMapping("/api/admin")
class AdminController(private val adminService: AdminService) {

    @PostMapping("/add")
    fun addAdmin(@RequestBody body: AdminCreateRequest) = adminService.addAdmin(body)

    @GetMapping
    fun getAllAdmins() = adminService.getAllAdmins()

    @GetMapping("/{id}/students")
    fun getAdminStudents(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "24") size: Int,
    ) = adminService.getAdminStudents(id, page, size)
}

@RestController
@RequestMapping("/api/students")
class StudentController(private val studentService: StudentService) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun create(
        @RequestPart("data") body: StudentCreateRequest,
        @RequestPart("passportPhoto", required = false) passportPhoto: MultipartFile?,
        @RequestPart("form083", required = false) form083: MultipartFile?,
    ) = studentService.create(body, passportPhoto, form083)

    @GetMapping
    fun getAll(
        @RequestParam status: StudentStatus,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "24") size: Int,
    ) = studentService.getAll(status, page, size)

    @GetMapping("/{id}")
    fun getOne(@PathVariable id: Long) = studentService.getOne(id)

    @GetMapping("/search")
    fun search(
        @RequestParam(defaultValue = "") firstName: String,
        @RequestParam(defaultValue = "") lastName: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "24") size: Int,
    ) = studentService.search(firstName, lastName, page, size)

    @PutMapping("/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun update(
        @PathVariable id: Long,
        @RequestPart("data") body: StudentUpdateRequest,
        @RequestPart("passportPhoto", required = false) passportPhoto: MultipartFile?,
        @RequestPart("form083", required = false) form083: MultipartFile?,
    ) = studentService.update(id, body, passportPhoto, form083)

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestBody body: StudentStatusUpdateRequest,
    ) = studentService.updateStatus(id, body)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) = studentService.delete(id)
}

@RestController
@RequestMapping("/api/students/{studentId}/payments")
class PaymentController(private val paymentService: PaymentService) {

    @PostMapping
    fun addPayment(
        @PathVariable studentId: Long,
        @RequestBody body: PaymentCreateRequest,
    ) = paymentService.addPayment(studentId, body)

    @GetMapping
    fun getPayments(@PathVariable studentId: Long) =
        paymentService.getPayments(studentId)

    @DeleteMapping("/{paymentId}")
    fun deletePayment(
        @PathVariable studentId: Long,
        @PathVariable paymentId: Long,
    ) = paymentService.deletePayment(paymentId)
}