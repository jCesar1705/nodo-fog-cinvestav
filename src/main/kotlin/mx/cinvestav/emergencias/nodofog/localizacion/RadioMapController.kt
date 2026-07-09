package mx.cinvestav.emergencias.nodofog.localizacion

import com.google.gson.Gson
import mx.cinvestav.emergencias.nodofog.localizacion.dto.CalibracionRequest
import mx.cinvestav.emergencias.nodofog.model.RadioMapEntry
import mx.cinvestav.emergencias.nodofog.repository.RadioMapRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

/**
 * Gestión del radio map de calibración (Sprint 3).
 *
 * POST /api/radiomap/calibrar  — guarda un punto de calibración para una zona
 * GET  /api/radiomap           — lista todos los puntos de calibración
 * DELETE /api/radiomap/{id}    — elimina un punto de calibración
 *
 * Flujo de calibración:
 *   1. El admin camina al centro de la zona a calibrar
 *   2. La app víctima (o una app separada) escanea WiFi y manda el fingerprint
 *   3. El FOG guarda el fingerprint asociado a esa zona
 *   4. Al activar una emergencia el k-NN compara contra estos puntos
 */
@RestController
@RequestMapping("/api/radiomap")
class RadioMapController(
    private val repo: RadioMapRepository
) {
    private val gson = Gson()

    /** Obtiene el rol desde el JWT validado por [mx.cinvestav.emergencias.nodofog.security.JwtFilter]. */
    private fun rolActual(): String {
        val auth = SecurityContextHolder.getContext().authentication
        return auth?.authorities?.firstOrNull()
            ?.authority?.removePrefix("ROLE_") ?: "USUARIO"
    }

    /** Guarda un punto de calibración. Se puede llamar varias veces para promediar. */
    @PostMapping("/calibrar")
    fun calibrar(
        @RequestBody req: CalibracionRequest
    ): ResponseEntity<Any> {
        val rol = rolActual()
        if (rol != "ADMINISTRADOR" && rol != "ADMIN" && rol != "BRIGADISTA") {
            return ResponseEntity.status(403)
                .body(mapOf("error" to "Se requiere rol ADMINISTRADOR o BRIGADISTA para calibrar"))
        }
        if (req.zonaId.isBlank() || req.fingerprint.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Se requiere zonaId y al menos un AP"))
        }

        val entrada = RadioMapEntry(
            zonaId     = req.zonaId,
            zonaNombre = req.zonaNombre,
            piso       = req.piso,
            fingerprint = gson.toJson(req.fingerprint)
        )
        val guardado = repo.save(entrada)
        return ResponseEntity.ok(mapOf(
            "mensaje"   to "Punto de calibración guardado",
            "id"        to guardado.id,
            "zona"      to guardado.zonaNombre,
            "piso"      to guardado.piso,
            "aps"       to req.fingerprint.size
        ))
    }

    /** Lista todos los puntos de calibración. */
    @GetMapping
    fun listar(): ResponseEntity<List<RadioMapEntry>> =
        ResponseEntity.ok(repo.findAll())

    /** Elimina un punto de calibración. */
    @DeleteMapping("/{id}")
    fun eliminar(
        @PathVariable id: Long
    ): ResponseEntity<Any> {
        val rol = rolActual()
        if (rol != "ADMINISTRADOR" && rol != "ADMIN") {
            return ResponseEntity.status(403).body(mapOf("error" to "Se requiere ADMIN"))
        }
        if (!repo.existsById(id)) return ResponseEntity.notFound().build()
        repo.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
