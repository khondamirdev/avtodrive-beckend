package com.example.avtodrivepro

import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler(private val messageSource: MessageSource) {
    @ExceptionHandler(AvtoDriveException::class)
    fun handleAvtoDriveException(exception: AvtoDriveException): ResponseEntity<BaseMessage> {
        return ResponseEntity.badRequest().body(exception.getErrorMessage(messageSource))
    }
}

sealed class AvtoDriveException : RuntimeException() {
    abstract fun errorCode(): ErrorCode
    open fun getArguments(): Array<Any?>? = null

    fun getErrorMessage(messageSource: MessageSource): BaseMessage {
        val message = try {
            messageSource.getMessage(errorCode().name, getArguments(), LocaleContextHolder.getLocale())
        } catch (e: Exception) {
            e.message ?: errorCode().name
        }
        return BaseMessage(errorCode().code, message)
    }
}

class InvalidCredentialsException : AvtoDriveException() {
    override fun errorCode() = ErrorCode.INVALID_CREDENTIALS
}

class TokenExpiredException : AvtoDriveException() {
    override fun errorCode() = ErrorCode.TOKEN_EXPIRED
}

class TokenRevokedException : AvtoDriveException() {
    override fun errorCode() = ErrorCode.TOKEN_REVOKED
}

class AccessDeniedException : AvtoDriveException() {
    override fun errorCode() = ErrorCode.ACCESS_DENIED
}

class UserNotFoundException : AvtoDriveException() {
    override fun errorCode() = ErrorCode.USER_NOT_FOUND
}

class UserAlreadyExistsException : AvtoDriveException() {
    override fun errorCode() = ErrorCode.USER_ALREADY_EXISTS
}

class StudentNotFoundException : AvtoDriveException() {
    override fun errorCode() = ErrorCode.STUDENT_NOT_FOUND
}

class StudentPhoneAlreadyExistsException : AvtoDriveException() {
    override fun errorCode() = ErrorCode.STUDENT_PHONE_ALREADY_EXISTS
}

class PaymentNotFoundException : AvtoDriveException() {
    override fun errorCode() = ErrorCode.PAYMENT_NOT_FOUND
}

class InvalidPaymentAmountException : AvtoDriveException() {
    override fun errorCode() = ErrorCode.INVALID_PAYMENT_AMOUNT
}

class FileTooLargeException : AvtoDriveException() {
    override fun errorCode() = ErrorCode.FILE_TOO_LARGE
}

class FileTypeNotSupportedException : AvtoDriveException() {
    override fun errorCode() = ErrorCode.FILE_TYPE_NOT_SUPPORTED
}

class FileUploadFailedException : AvtoDriveException() {
    override fun errorCode() = ErrorCode.FILE_UPLOAD_FAILED
}