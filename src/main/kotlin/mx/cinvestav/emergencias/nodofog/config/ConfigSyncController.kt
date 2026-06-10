package mx.cinvestav.emergencias.nodofog.config

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
 * Sprint 2: se recibe y registra la config en logs.
 * Los usuarios del nodo central se almacenan en UsuarioConfig para el login.
 * TODO Sprint 3: persistir zonas y parámetros, aplicar cifrado AES en datos médicos.
 */
@RestController
@RequestMapping("/config")
class ConfigSyncController {

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
        val usuarios   = (payload["usuarios"]   as? List<*>)?.size ?: 0
        val parametros = (payload["parametros"] as? Map<*, *>)?.size ?: 0

        log.info("Config sincronizada desde nodo central — edificio={}, zonas={}, usuarios={}, params={}",
            edificio, zonas, usuarios, parametros)

        return ResponseEntity.ok(mapOf(
            "status"     to "aplicado",
            "edificio"   to edificio,
            "zonas"      to zonas,
            "usuarios"   to usuarios,
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
