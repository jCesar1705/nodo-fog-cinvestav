package mx.cinvestav.emergencias.nodofog.localizacion

import mx.cinvestav.emergencias.nodofog.config.EmergenciaState
import mx.cinvestav.emergencias.nodofog.localizacion.dto.FingerprintRequest
import mx.cinvestav.emergencias.nodofog.repository.UbicacionRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Endpoints REST del módulo de localización (CU-02, CU-05).
 *
 * POST /api/ubicacion          — app víctima envía fingerprint WiFi
 * GET  /api/ubicacion          — app brigadista consulta posiciones activas
 * DELETE /api/ubicacion        — limpia al terminar emergencia (llamado por EmergenciaService)
 */
@RestController
@RequestMapping("/api/ubicacion")
class UbicacionController(
    private val service        : LocalizacionService,
    private val ubicacionRepo  : UbicacionRepository,
    private val emergenciaState: EmergenciaState
) {

    /**
     * La app víctima llama este endpoint cada 30s durante la emergencia.
     * El FOG estima la zona con k-NN y publica la posición por MQTT.
     *
     * Solo acepta datos cuando hay emergencia activa (QA-10, R7).
     */
    @PostMapping
    fun recibirUbicacion(@RequestBody req: FingerprintRequest): ResponseEntity<Any> {
        if (!emergenciaState.isActiva()) {
            return ResponseEntity.status(403)
                .body(mapOf("error" to "Solo se registran ubicaciones durante emergencia activa (R7)"))
        }
        if (req.victimaId.isBlank() || req.fingerprint.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Se requiere victimaId y al menos un AP en el fingerprint"))
        }
        val respuesta = service.procesarUbicacion(req)
        return ResponseEntity.ok(respuesta)
    }

    /**
     * La app brigadista consulta las posiciones activas durante la emergencia (CU-05).
     * Requiere rol BRIGADISTA o ADMINISTRADOR.
     */
    @GetMapping
    fun listarUbicaciones(
        @RequestHeader("X-Role") rol: String
    ): ResponseEntity<Any> {
        if (rol != "BRIGADISTA" && rol != "ADMINISTRADOR" && rol != "ADMIN") {
            return ResponseEntity.status(403)
                .body(mapOf("error" to "Se requiere rol BRIGADISTA"))
        }
        if (!emergenciaState.isActiva()) {
            return ResponseEntity.status(403)
                .body(mapOf("error" to "Las ubicaciones solo son visibles durante emergencia activa (R7)"))
        }
        return ResponseEntity.ok(ubicacionRepo.findAll())
    }
}
