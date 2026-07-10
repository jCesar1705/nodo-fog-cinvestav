package mx.cinvestav.emergencias.nodofog.auth

import mx.cinvestav.emergencias.nodofog.repository.VictimaRepository
import mx.cinvestav.emergencias.nodofog.security.JwtUtil
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

/**
 * Autenticación de usuarios contra el registro local del nodo FOG.
 *
 * POST /api/auth/login              — matrícula + password → JWT + datos del usuario
 * PUT  /api/auth/cambiar-password   — cambio de contraseña (Sprint-seguridad)
 *
 * Sprint 4: la contraseña se verifica con BCrypt y el login devuelve un JWT firmado.
 * El identificador que llega es la MATRÍCULA (ej. "A07654321").
 * La respuesta devuelve el ID interno de la víctima (ej. "victima-002") como
 * identificador, para que los heartbeats usen el ID correcto.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val victimaRepository: VictimaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil
) {

    @PostMapping("/login")
    fun login(@RequestBody body: Map<String, String>): ResponseEntity<Any> {
        val identificador = body["identificador"]?.trim()
        val password      = body["password"] ?: ""

        if (identificador.isNullOrBlank()) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Se requiere identificador"))
        }

        // Buscar por matrícula (identificador externo)
        val victima = victimaRepository.findByMatricula(identificador)
        // Si no encontró por matrícula, intentar buscar por id interno
            ?: victimaRepository.findById(identificador).orElse(null)

        if (victima == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Usuario no encontrado"))
        }

        // Verificación real con BCrypt
        if (!victima.passwordHash.isNullOrBlank() &&
            !passwordEncoder.matches(password, victima.passwordHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Contraseña incorrecta"))
        }

        val rol = victima.rol.ifBlank { "VICTIMA" }
        val token = jwtUtil.generarToken(victima.id, victima.nombre, rol)

        // Devolver el id interno como identificador para que el heartbeat lo use correctamente
        return ResponseEntity.ok(mapOf(
            "id"            to victima.id,
            "identificador" to victima.id,       // ID interno, no la matrícula
            "nombre"        to victima.nombre,
            "rol"           to rol,
            "token"         to token
        ))
    }

    @PutMapping("/cambiar-password")
    fun cambiarPassword(@RequestBody body: Map<String, String>): ResponseEntity<Any> {
        val victimaId = SecurityContextHolder.getContext()
            .authentication?.name ?: return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

        val passwordActual = body["passwordActual"] ?: ""
        val passwordNueva  = body["passwordNueva"] ?: ""

        val victima = victimaRepository.findByMatricula(victimaId)
            ?: victimaRepository.findById(victimaId).orElse(null)

        if (victima == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Usuario no encontrado"))
        }

        if (!passwordEncoder.matches(passwordActual, victima.passwordHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Contraseña actual incorrecta"))
        }

        if (passwordNueva.length < 4) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "La contraseña debe tener al menos 4 caracteres"))
        }

        val actualizado = victima.copy(passwordHash = passwordEncoder.encode(passwordNueva)!!)
        victimaRepository.save(actualizado)

        return ResponseEntity.ok(mapOf("mensaje" to "Contraseña actualizada correctamente"))
    }
}