package com.example.avtodrivepro

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
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
@RequestMapping("/api/admin")
class AdminController(private val adminService: AdminService) {

    @PostMapping("/add")
    fun addAdmin(@RequestBody body: AdminCreateRequest) = adminService.addAdmin(body)
}

@RestController
@RequestMapping("/api/students")
class StudentController(private val studentService: StudentService) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun create(
        @RequestPart("data") body: StudentCreateRequest,
        @RequestPart("photo", required = false) photo: MultipartFile?,
    ) = studentService.create(body, photo)

    @GetMapping
    fun getAll(
        @RequestParam status: StudentStatus,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "24") size: Int,
    ) = studentService.getAll(status, page, size)

    @GetMapping("/search")
    fun search(
        @RequestParam(defaultValue = "") firstName: String,
        @RequestParam(defaultValue = "") lastName: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "24") size: Int,
    ) = studentService.search(firstName, lastName, page, size)

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestBody body: StudentStatusUpdateRequest,
    ) = studentService.updateStatus(id, body)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) = studentService.delete(id)

    @PutMapping("/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun update(
        @PathVariable id: Long,
        @RequestPart("data") body: StudentUpdateRequest,
        @RequestPart("photo", required = false) photo: MultipartFile?,
    ) = studentService.update(id, body, photo)
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
