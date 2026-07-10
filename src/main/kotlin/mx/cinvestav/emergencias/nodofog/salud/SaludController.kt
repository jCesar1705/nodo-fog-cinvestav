package mx.cinvestav.emergencias.nodofog.salud

import mx.cinvestav.emergencias.nodofog.config.EmergenciaState
import mx.cinvestav.emergencias.nodofog.salud.dto.RegistrarSaludRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/salud")
class SaludController(
    private val service: SaludService,
    private val emergenciaState: EmergenciaState
) {
    private val log = LoggerFactory.getLogger(SaludController::class.java)

    // POST público con JWT de víctima — registra o actualiza datos médicos
    @PostMapping
    fun registrar(@RequestBody req: RegistrarSaludRequest): ResponseEntity<Any> {
        log.info("POST /api/salud recibido — victimaId={}", req.victimaId)
        if (!req.consentimientoLFPDPPP) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Se requiere consentimiento LFPDPPP (R8)"))
        }
        val entry = service.registrar(req)
        return ResponseEntity.ok(mapOf(
            "mensaje"   to "Datos médicos registrados correctamente",
            "victimaId" to entry.victimaId,
            "timestamp" to entry.timestamp
        ))
    }

    // GET solo para brigadistas durante emergencia activa (CU-09, QA-10)
    @GetMapping("/{victimaId}")
    fun obtener(@PathVariable victimaId: String): ResponseEntity<Any> {
        if (!emergenciaState.isActiva()) {
            return ResponseEntity.status(403)
                .body(mapOf("error" to "Datos médicos solo visibles durante emergencia activa (QA-10, R7)"))
        }
        val datos = service.obtener(victimaId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(datos)
    }
}
