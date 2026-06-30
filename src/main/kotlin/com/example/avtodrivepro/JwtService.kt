package com.example.avtodrivepro

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date

@Service
class JwtService {

    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @Value("\${jwt.expiration}")
    private var expiration: Long = 3600000

    private val signingKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateToken(user: UserEntity): String {
        val now = Date()
        return Jwts.builder()
            .setSubject(user.username)
            .claim("role", user.role.name)
            .setIssuedAt(now)
            .setExpiration(Date(now.time + expiration))
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact()
    }

    fun extractUsername(token: String): String =
        extractAllClaims(token).subject

    fun extractExpiration(token: String): Date =
        extractAllClaims(token).expiration

    fun isTokenValid(token: String): Boolean =
        runCatching { extractExpiration(token).after(Date()) }.getOrDefault(false)

    private fun extractAllClaims(token: String): Claims =
        Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .body
}
