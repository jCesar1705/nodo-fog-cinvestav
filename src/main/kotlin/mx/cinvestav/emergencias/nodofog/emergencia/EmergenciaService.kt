package mx.cinvestav.emergencias.nodofog.emergencia

import com.google.gson.Gson
import mx.cinvestav.emergencias.nodofog.config.EmergenciaState
import mx.cinvestav.emergencias.nodofog.emergencia.dto.ActivarEmergenciaRequest
import mx.cinvestav.emergencias.nodofog.emergencia.dto.AlertaMensaje
import mx.cinvestav.emergencias.nodofog.mqtt.MqttPublisher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class EmergenciaService(
    private val mqttPublisher: MqttPublisher,
    @Value("\${fog.node-id}") private val nodeId: String,
    private val emergenciaState: EmergenciaState,
    private val paseListaService: PaseListaService
) {
    private val log = LoggerFactory.getLogger(EmergenciaService::class.java)
    private val gson = Gson()

    fun activar(request: ActivarEmergenciaRequest): AlertaMensaje {
        val emergenciaId = emergenciaState.activar()

        val alerta = AlertaMensaje(
            id = emergenciaId,
            tipo = request.tipo,
            severidad = request.severidad,
            mensaje = request.mensaje,
            timestamp = System.currentTimeMillis()
        )

        // T3 — publicar alerta con QoS 2 y retain
        mqttPublisher.publicar("cinvestav/$nodeId/alertas", gson.toJson(alerta))

        // T4 — generar pase de lista con presentes en ultimos 20 min
        val lista = paseListaService.generar(emergenciaId)
        log.info("Pase de lista generado: {} personas", lista.size)

        return alerta
    }

    fun desactivar() {
        emergenciaState.desactivar()
        // Publicar NORMAL con retain → bloqueo automatico en app victima (QA-10, R7)
        mqttPublisher.publicar(
            "cinvestav/$nodeId/estado",
            gson.toJson(mapOf("modo" to "NORMAL", "timestamp" to System.currentTimeMillis()))
        )
    }
}
