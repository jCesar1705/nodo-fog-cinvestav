package mx.cinvestav.emergencias.nodofog.emergencia

import tools.jackson.databind.ObjectMapper
import mx.cinvestav.emergencias.nodofog.emergencia.dto.ActivarEmergenciaRequest
import mx.cinvestav.emergencias.nodofog.emergencia.dto.AlertaMensaje
import mx.cinvestav.emergencias.nodofog.mqtt.MqttPublisher
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EmergenciaService(
    private val mqttPublisher: MqttPublisher,
    private val objectMapper: ObjectMapper,
    @Value("\${fog.node-id}") private val nodeId: String
) {

    fun activar(request: ActivarEmergenciaRequest): AlertaMensaje {
        val alerta = AlertaMensaje(
            id = UUID.randomUUID().toString(),
            tipo = request.tipo,
            severidad = request.severidad,
            mensaje = request.mensaje,
            timestamp = System.currentTimeMillis()
        )
        val topic = "cinvestav/$nodeId/alertas"
        mqttPublisher.publicar(topic, objectMapper.writeValueAsString(alerta))
        return alerta
    }
}