package com.example.avtodrivepro

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.util.Optional

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableJpaRepositories(repositoryBaseClass = BaseRepositoryImpl::class)
class AuditingConfig {

    @Bean
    fun auditorAware(): AuditorAware<Long> = AuditorAware {
        val authentication = SecurityContextHolder.getContext()?.authentication
        if (authentication == null || !authentication.isAuthenticated) {
            return@AuditorAware Optional.empty()
        }
        val principal = authentication.principal
        if (principal is Long) {
            return@AuditorAware Optional.of(principal)
        }
        Optional.empty()
    }
}

@Component
class DataInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        val superAdminUsername = "superadmin"
        if (!userRepository.existsByUsernameAndDeletedFalse(superAdminUsername)) {
            val superAdmin = UserEntity(
                username = superAdminUsername,
                password = passwordEncoder.encode("superadmin123"),
                role = UserRole.SUPER_ADMIN,
            )
            userRepository.save(superAdmin)
            println("✅ SUPER_ADMIN yaratildi → username: $superAdminUsername | password: superadmin123")
        }
    }
}