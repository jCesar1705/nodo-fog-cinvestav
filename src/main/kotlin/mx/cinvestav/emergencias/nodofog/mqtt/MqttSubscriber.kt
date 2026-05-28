package mx.cinvestav.emergencias.nodofog.mqtt

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import mx.cinvestav.emergencias.nodofog.heartbeat.HeartbeatService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Suscriptor MQTT independiente del [MqttPublisher].
 * Se suscribe al topic de heartbeats y delega el procesamiento a [HeartbeatService].
 *
 * Usa un cliente MQTT5 async con reconexión automática.
 */
@Component
class MqttSubscriber(
    @Value("\${mqtt.broker.host}") private val host: String,
    @Value("\${mqtt.broker.port}") private val port: Int,
    @Value("\${fog.node-id}") private val nodeId: String,
    private val heartbeatService: HeartbeatService
) {
    private val log = LoggerFactory.getLogger(MqttSubscriber::class.java)
    private lateinit var client: Mqtt5AsyncClient

    @PostConstruct
    fun conectar() {
        client = MqttClient.builder()
            .useMqttVersion5()
            .identifier("nodo-fog-subscriber")
            .serverHost(host)
            .serverPort(port)
            .automaticReconnectWithDefaultConfig()
            .buildAsync()

        client.connect().whenComplete { _, error ->
            if (error != null) {
                log.error("Error al conectar MqttSubscriber", error)
            } else {
                log.info("MqttSubscriber conectado al broker en {}:{}", host, port)
                suscribirHeartbeats()
            }
        }
    }

    private fun suscribirHeartbeats() {
        val topic = "cinvestav/$nodeId/heartbeats"
        client.subscribeWith()
            .topicFilter(topic)
            .qos(MqttQos.AT_LEAST_ONCE)
            .callback { publish ->
                val payload = String(publish.payloadAsBytes, Charsets.UTF_8)
                heartbeatService.procesar(payload)
            }
            .send()
            .whenComplete { _, error ->
                if (error != null) log.error("Error al suscribir a heartbeats", error)
                else log.info("Suscrito a topic: {}", topic)
            }
    }

    @PreDestroy
    fun cerrar() {
        if (::client.isInitialized) client.disconnect()
    }
}
