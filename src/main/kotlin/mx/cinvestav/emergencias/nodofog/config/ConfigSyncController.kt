package mx.cinvestav.emergencias.nodofog.config

import mx.cinvestav.emergencias.nodofog.model.Victima
import mx.cinvestav.emergencias.nodofog.repository.VictimaRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

/**
 * Recibe la configuración empujada por el nodo central (CU-10 — Sprint 2).
 *
 * POST /config/sync    — nodo central sincroniza edificios, zonas, usuarios y params
 * GET  /config/status  — verifica que el nodo responde (usado por la app víctima en IpConfigScreen)
 *
 * Sprint 4: los usuarios del nodo central se persisten en H2 (VictimaRepository)
 * para que el login (AuthController) los pueda autenticar con BCrypt.
 * TODO: persistir zonas y parámetros, aplicar cifrado AES en datos médicos.
 */
@RestController
@RequestMapping("/config")
class ConfigSyncController(
    private val victimaRepository: VictimaRepository
) {

    private val log = LoggerFactory.getLogger(ConfigSyncController::class.java)

    @PostMapping("/sync")
    fun sincronizar(
        @RequestHeader("X-Role") rol: String,
        @RequestBody payload: Map<String, Any>
    ): ResponseEntity<Map<String, Any>> {

        if (rol != "ADMINISTRADOR" && rol != "ADMIN") {
            return ResponseEntity.status(403)
                .body(mapOf("error" to "Se requiere X-Role: ADMINISTRADOR"))
        }

        val edificio   = payload["edificioClave"] ?: "?"
        val zonas      = (payload["zonas"]      as? List<*>)?.size ?: 0
        val parametros = (payload["parametros"] as? Map<*, *>)?.size ?: 0

        @Suppress("UNCHECKED_CAST")
        val usuariosPayload = payload["usuarios"] as? List<Map<String, Any>> ?: emptyList()
        var usuariosPersistidos = 0

        usuariosPayload.forEach { u ->
            val identificador = u["identificador"] as? String ?: return@forEach
            val nombre        = u["nombre"]        as? String ?: return@forEach
            val passwordHash  = u["passwordHash"]  as? String ?: ""

            // Buscar si ya existe por matrícula o por id
            val existente = victimaRepository.findByMatricula(identificador)
                ?: victimaRepository.findById(identificador).orElse(null)

            if (existente == null) {
                // Crear nueva Victima con los datos del central
                val nueva = Victima(
                    id           = identificador,
                    nombre       = nombre,
                    matricula    = identificador,
                    passwordHash = passwordHash
                )
                victimaRepository.save(nueva)
            } else {
                // Actualizar nombre y hash si vino un hash válido de BCrypt (data class → copy())
                val actualizado = existente.copy(
                    nombre = nombre,
                    passwordHash = if (passwordHash.startsWith("\$2a\$")) passwordHash else existente.passwordHash
                )
                victimaRepository.save(actualizado)
            }
            usuariosPersistidos++
        }

        log.info("Config sincronizada: edificio={}, zonas={}, usuarios={} persistidos, params={}",
            edificio, zonas, usuariosPersistidos, parametros)

        return ResponseEntity.ok(mapOf(
            "status"     to "aplicado",
            "edificio"   to edificio,
            "zonas"      to zonas,
            "usuarios"   to usuariosPersistidos,
            "parametros" to parametros,
            "timestamp"  to Instant.now().toString()
        ))
    }

    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "nodo"      to "Nodo-Fog",
            "timestamp" to Instant.now().toString()
        ))
    }
}