package mx.cinvestav.emergencias.nodofog.auth

import mx.cinvestav.emergencias.nodofog.repository.VictimaRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Autenticación de usuarios contra el registro local del nodo FOG.
 *
 * POST /api/auth/login              — matrícula + password → datos del usuario
 * PUT  /api/auth/cambiar-password   — cambio de contraseña (Sprint-seguridad)
 *
 * Sprint 2: la contraseña no se verifica (seguridad diferida al sprint de seguridad).
 * El identificador que llega es la MATRÍCULA (ej. "A07654321").
 * La respuesta devuelve el ID interno de la víctima (ej. "victima-002") como
 * identificador, para que los heartbeats usen el ID correcto.
 *
 * TODO Sprint-seguridad: agregar campo password a Victima y verificar con AES.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val victimaRepository: VictimaRepository
) {

    @PostMapping("/login")
    fun login(@RequestBody body: Map<String, String>): ResponseEntity<Any> {
        val identificador = body["identificador"]?.trim()
        val password      = body["password"]

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

        // Sprint 2: contraseña no verificada (TODO Sprint-seguridad)
        // Devolver el id interno como identificador para que el heartbeat lo use correctamente
        return ResponseEntity.ok(mapOf(
            "id"            to victima.id,
            "identificador" to victima.id,       // ID interno, no la matrícula
            "nombre"        to victima.nombre,
            "rol"           to "USUARIO"
        ))
    }

    @PutMapping("/cambiar-password")
    fun cambiarPassword(@RequestBody body: Map<String, String>): ResponseEntity<Any> {
        // TODO Sprint-seguridad: implementar cambio de contraseña con AES
        return ResponseEntity.ok(mapOf("mensaje" to "Funcionalidad disponible en Sprint de seguridad"))
    }
}
