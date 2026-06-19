package preq.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import preq.model.User
import java.util.Date

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access-token-expiration-ms}") private val accessTokenExpMs: Long,
    @Value("\${jwt.refresh-token-expiration-ms}") private val refreshTokenExpMs: Long,
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateAccessToken(user: User): String = buildToken(user.email, accessTokenExpMs, mapOf("role" to user.role.name))

    fun generateRefreshToken(email: String): String = buildToken(email, refreshTokenExpMs, emptyMap())

    private fun buildToken(
        subject: String,
        expirationMs: Long,
        extraClaims: Map<String, Any> = emptyMap(),
    ): String =
        Jwts
            .builder()
            .subject(subject)
            .claims(extraClaims)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

    fun extractEmail(token: String): String = extractClaims(token).subject

    fun isTokenValid(token: String): Boolean =
        runCatching {
            extractClaims(token).expiration.after(Date())
        }.getOrDefault(false)

    private fun extractClaims(token: String): Claims =
        Jwts
            .parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
