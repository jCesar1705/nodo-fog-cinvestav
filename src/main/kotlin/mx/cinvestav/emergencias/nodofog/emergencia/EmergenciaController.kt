package mx.cinvestav.emergencias.nodofog.emergencia

import mx.cinvestav.emergencias.nodofog.emergencia.dto.ActivarEmergenciaRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * T2 — Endpoint POST /api/emergencia/activar.
 *
 * Recibe la señal de activación, valida que el rol sea ADMIN o SISTEMA y
 * delega a EmergenciaService.activar() (que publica la alerta vía MQTT — T3).
 *
 * NOTA: la validación de rol aquí es simplificada para el prototipo (header X-Role).
 * En producción se usaría Spring Security + JWT, como contempla la arquitectura (QA-12).
 */
@RestController
@RequestMapping("/api/emergencia")
class EmergenciaController(
    private val emergenciaService: EmergenciaService
) {
    @PostMapping("/activar")
    fun activar(
        @RequestHeader(value = "X-Role", required = false) role: String?,
        @RequestBody(required = false) request: ActivarEmergenciaRequest?
    ): ResponseEntity<Any> {
        if (role != "ADMIN" && role != "SISTEMA") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "Rol no autorizado. Se requiere ADMIN o SISTEMA."))
        }

        val alerta = emergenciaService.activar(request ?: ActivarEmergenciaRequest())
        return ResponseEntity.ok(alerta)
    }
}
