package com.example.avtodrivepro

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtFilter: JwtAuthFilter,
    private val userRepository: UserRepository,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(): UserDetailsService = UserDetailsService { username ->
        val user = userRepository.findByUsernameAndDeletedFalse(username)
            ?: throw UsernameNotFoundException("User not found: $username")

        org.springframework.security.core.userdetails.User(
            user.username,
            user.password,
            listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
        )
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it
                    .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                    .requestMatchers("/uploads/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                    .requestMatchers("/api/profile/**").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/admin/add").hasRole("SUPER_ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/admin/**").hasRole("SUPER_ADMIN")
                    .requestMatchers("/api/students/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOrigins = listOf(
            "https://avtodrivepro.uz",
            "https://avtodrive-frontend.onrender.com" // frontend hali rendarda tursa
        )
        config.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val jwtTokenRepository: JwtTokenRepository,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(7)

        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response)
            return
        }

        val tokenEntity = jwtTokenRepository.findByTokenValueAndRevokedFalse(token)
        if (tokenEntity == null) {
            filterChain.doFilter(request, response)
            return
        }

        val username = jwtService.extractUsername(token)
        val user = userRepository.findByUsernameAndDeletedFalse(username)

        if (user != null && SecurityContextHolder.getContext().authentication == null) {
            val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
            val auth = UsernamePasswordAuthenticationToken(user.id, null, authorities)
            SecurityContextHolder.getContext().authentication = auth
        }

        filterChain.doFilter(request, response)
    }
}
