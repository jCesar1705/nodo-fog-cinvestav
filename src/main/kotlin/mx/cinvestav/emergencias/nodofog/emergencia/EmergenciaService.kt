package mx.cinvestav.emergencias.nodofog.emergencia

import com.google.gson.Gson
import mx.cinvestav.emergencias.nodofog.config.EmergenciaState
import mx.cinvestav.emergencias.nodofog.emergencia.dto.ActivarEmergenciaRequest
import mx.cinvestav.emergencias.nodofog.emergencia.dto.AlertaMensaje
import mx.cinvestav.emergencias.nodofog.localizacion.LocalizacionService
import mx.cinvestav.emergencias.nodofog.mqtt.MqttPublisher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EmergenciaService(
    private val mqttPublisher    : MqttPublisher,
    @Value("\${fog.node-id}") private val nodeId: String,
    private val emergenciaState  : EmergenciaState,
    private val paseListaService : PaseListaService,
    private val localizacionService: LocalizacionService   // Sprint 3
) {
    private val log  = LoggerFactory.getLogger(EmergenciaService::class.java)
    private val gson = Gson()

    fun activar(request: ActivarEmergenciaRequest): AlertaMensaje {
        val emergenciaId = emergenciaState.activar()
        val alerta = AlertaMensaje(
            id        = emergenciaId,
            tipo      = request.tipo,
            severidad = request.severidad,
            mensaje   = request.mensaje,
            timestamp = System.currentTimeMillis()
        )
        mqttPublisher.publicar("cinvestav/$nodeId/alertas", gson.toJson(alerta))
        val lista = paseListaService.generar(emergenciaId)
        log.info("Pase de lista generado: {} personas", lista.size)
        return alerta
    }

    fun desactivar() {
        emergenciaState.desactivar()
        val timestamp = System.currentTimeMillis()

        // Sprint 3: eliminar ubicaciones de víctimas al terminar emergencia (R7, CA-07)
        localizacionService.limpiarUbicaciones()

        // Publicar NORMAL al estado con retain (bloqueo automático QA-10, R7)
        mqttPublisher.publicar(
            "cinvestav/$nodeId/estado",
            gson.toJson(mapOf("modo" to "NORMAL", "timestamp" to timestamp))
        )

        // Limpiar mensaje retenido de alertas — publicar FIN_EMERGENCIA
        mqttPublisher.publicar(
            "cinvestav/$nodeId/alertas",
            gson.toJson(mapOf(
                "id"        to UUID.randomUUID().toString(),
                "tipo"      to "FIN_EMERGENCIA",
                "severidad" to "BAJA",
                "mensaje"   to "La emergencia ha concluido.",
                "timestamp" to timestamp
            ))
        )

        log.info("Emergencia desactivada — ubicaciones eliminadas, FIN_EMERGENCIA publicado")
    }
}
