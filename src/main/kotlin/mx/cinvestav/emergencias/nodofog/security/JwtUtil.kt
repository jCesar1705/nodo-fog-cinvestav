package mx.cinvestav.emergencias.nodofog.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

/**
 * Generación y validación de JWT firmados con HMAC (Sprint 4 — seguridad).
 */
@Component
class JwtUtil(
    @Value("\${fog.jwt.secret}") private val secret: String,
    @Value("\${fog.jwt.expiration-days}") private val expirationDays: Long
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    fun generarToken(victimaId: String, nombre: String, rol: String): String {
        val ahora = Date()
        val expira = Date(ahora.time + expirationDays * 24 * 3600 * 1000)
        return Jwts.builder()
            .subject(victimaId)
            .claim("nombre", nombre)
            .claim("rol", rol)
            .issuer("saces-fog")
            .issuedAt(ahora)
            .expiration(expira)
            .signWith(key)
            .compact()
    }

    fun validar(token: String): Claims? = try {
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload
    } catch (e: Exception) { null }

    fun getRol(token: String): String? = validar(token)?.get("rol", String::class.java)
    fun getSubject(token: String): String? = validar(token)?.subject
}