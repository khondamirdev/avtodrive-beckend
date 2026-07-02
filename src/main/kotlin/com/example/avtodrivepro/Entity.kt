package com.example.avtodrivepro

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.Date

// ─────────────────────────────────────────────
// BASE ENTITY
// ─────────────────────────────────────────────
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdDate: Date? = null,
    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var modifiedDate: Date? = null,
    @CreatedBy var createdBy: Long? = null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false
)

// ─────────────────────────────────────────────
// USER — Tizim foydalanuvchisi (Admin / SuperAdmin)
// ─────────────────────────────────────────────
@Entity
@Table(name = "users")
class UserEntity(

    @Column(nullable = false, unique = true)
    var username: String,

    @Column(nullable = false)
    var password: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole,

    @Column(nullable = false)
    @ColumnDefault("true")
    var isActive: Boolean = true,

) : BaseEntity()

// ─────────────────────────────────────────────
// STUDENT — O'quvchi
// ─────────────────────────────────────────────
@Entity
@Table(name = "students")
class StudentEntity(

    @Column(nullable = false)
    var firstName: String,

    @Column(nullable = false)
    var lastName: String,

    var phoneNumber: String? = null,

    @Column
    var photoPath: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: StudentStatus = StudentStatus.REGISTERED,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    var createdByUser: UserEntity,

) : BaseEntity()

// ─────────────────────────────────────────────
// JWT TOKEN — Token boshqaruvi
// ─────────────────────────────────────────────
@Entity
@Table(name = "jwt_tokens")
class JwtTokenEntity(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @Column(nullable = false, unique = true, length = 512)
    var tokenValue: String,

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    var expiresAt: Date,

    @Column(nullable = false)
    @ColumnDefault("false")
    var revoked: Boolean = false,

) : BaseEntity()
