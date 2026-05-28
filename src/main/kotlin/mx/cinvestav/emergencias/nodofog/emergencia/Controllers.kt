package mx.cinvestav.emergencias.nodofog.emergencia

import mx.cinvestav.emergencias.nodofog.emergencia.dto.ActivarEmergenciaRequest
import mx.cinvestav.emergencias.nodofog.emergencia.dto.ActualizarEstadoRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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

    @PostMapping("/desactivar")
    fun desactivar(
        @RequestHeader(value = "X-Role", required = false) role: String?
    ): ResponseEntity<Any> {
        if (role != "ADMIN" && role != "SISTEMA") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "Rol no autorizado."))
        }
        emergenciaService.desactivar()
        return ResponseEntity.ok(mapOf("modo" to "NORMAL", "mensaje" to "Emergencia finalizada"))
    }
}

/**
 * Endpoints del pase de lista.
 * GET  /api/pase-lista        → consultar lista actual (brigadista)
 * PUT  /api/pase-lista/{id}/estado → actualizar estado de una víctima (CU-07)
 */
@RestController
@RequestMapping("/api/pase-lista")
class PaseListaController(
    private val paseListaService: PaseListaService
) {
    @GetMapping
    fun obtener(
        @RequestHeader(value = "X-Role", required = false) role: String?
    ): ResponseEntity<Any> {
        if (role != "BRIGADISTA" && role != "ADMIN") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "Solo brigadistas pueden consultar el pase de lista."))
        }
        return ResponseEntity.ok(paseListaService.obtenerActual())
    }

    @PutMapping("/{victimaId}/estado")
    fun actualizarEstado(
        @PathVariable victimaId: String,
        @RequestHeader(value = "X-Role", required = false) role: String?,
        @RequestBody request: ActualizarEstadoRequest
    ): ResponseEntity<Any> {
        if (role != "BRIGADISTA" && role != "ADMIN") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "Solo brigadistas pueden actualizar el estado."))
        }
        val entrada = paseListaService.actualizarEstado(
            victimaId, request.estado, request.brigadistaId
        ) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(mapOf("error" to "Víctima no encontrada en el pase de lista activo."))

        return ResponseEntity.ok(entrada)
    }
}
