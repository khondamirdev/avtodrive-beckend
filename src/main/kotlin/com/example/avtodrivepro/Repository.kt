package com.example.avtodrivepro

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface BaseRepository<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
    fun findByIdAndDeletedFalse(id: Long): T?
    fun trash(id: Long): T?
    fun trashList(ids: List<Long>): List<T?>
    fun findAllNotDeleted(): List<T>
    fun findAllNotDeletedForPageable(pageable: Pageable): Page<T>
    fun saveAndRefresh(t: T): T
}

class BaseRepositoryImpl<T : BaseEntity>(
    entityInformation: JpaEntityInformation<T, Long>, private val entityManager: EntityManager
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {

    val isNotDeletedSpecification = Specification<T> { root, _, cb -> cb.equal(root.get<Boolean>("deleted"), false) }

    override fun findByIdAndDeletedFalse(id: Long) = findByIdOrNull(id)?.run { if (deleted) null else this }

    @Transactional
    override fun trash(id: Long): T? = findByIdOrNull(id)?.takeIf { !it.deleted }?.run {
        deleted = true
        save(this)
    }

    override fun findAllNotDeleted(): List<T> = findAll(isNotDeletedSpecification)
    override fun findAllNotDeletedForPageable(pageable: Pageable): Page<T> =
        findAll(isNotDeletedSpecification, pageable)

    override fun trashList(ids: List<Long>): List<T?> = ids.map { trash(it) }

    @Transactional
    override fun saveAndRefresh(t: T): T {
        return save(t).apply { entityManager.refresh(this) }
    }
}

// ─────────────────────────────────────────────
// USER
// ─────────────────────────────────────────────
@Repository
interface UserRepository : BaseRepository<UserEntity> {
    fun findByUsernameAndDeletedFalse(username: String): UserEntity?
    fun existsByUsernameAndDeletedFalse(username: String): Boolean
}

// ─────────────────────────────────────────────
// STUDENT
// ─────────────────────────────────────────────
@Repository
interface StudentRepository : BaseRepository<StudentEntity> {
    fun existsByPhoneNumberAndDeletedFalse(phoneNumber: String): Boolean
    fun findAllByStatusAndDeletedFalse(status: StudentStatus, pageable: Pageable): Page<StudentEntity>
    fun findAllByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCaseAndDeletedFalse(
        firstName: String,
        lastName: String,
        pageable: Pageable
    ): Page<StudentEntity>
}

// ─────────────────────────────────────────────
// JWT TOKEN
// ─────────────────────────────────────────────
@Repository
interface JwtTokenRepository : BaseRepository<JwtTokenEntity> {
    fun findByTokenValueAndRevokedFalse(tokenValue: String): JwtTokenEntity?
    fun findAllByUserIdAndRevokedFalse(userId: Long): List<JwtTokenEntity>
}
