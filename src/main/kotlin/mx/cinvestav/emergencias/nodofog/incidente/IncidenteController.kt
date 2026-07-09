package mx.cinvestav.emergencias.nodofog.incidente

import mx.cinvestav.emergencias.nodofog.incidente.dto.CambiarEstadoRequest
import mx.cinvestav.emergencias.nodofog.incidente.dto.CrearIncidenteRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Endpoints de incidentes (CU-04). Protegidos por JWT (ver SecurityConfig).
 *
 * POST  /api/incidentes           — crear incidente
 * GET   /api/incidentes           — listar incidentes
 * PATCH /api/incidentes/{id}/estado — cambiar estado (NUEVO, EN_REVISION, RESUELTO)
 */
@RestController
@RequestMapping("/api/incidentes")
class IncidenteController(private val service: IncidenteService) {

    @PostMapping
    fun crear(@RequestBody req: CrearIncidenteRequest): ResponseEntity<Any> =
        ResponseEntity.status(201).body(service.crear(req))

    @GetMapping
    fun listar(): ResponseEntity<Any> = ResponseEntity.ok(service.listar())

    @PatchMapping("/{id}/estado")
    fun cambiarEstado(@PathVariable id: String,
                      @RequestBody req: CambiarEstadoRequest): ResponseEntity<Any> {
        val estados = setOf("NUEVO", "EN_REVISION", "RESUELTO")
        if (req.estado !in estados)
            return ResponseEntity.badRequest().body(mapOf("error" to "Estado inválido"))
        val updated = service.cambiarEstado(id, req.estado)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }
}
