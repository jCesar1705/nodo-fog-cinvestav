package mx.cinvestav.emergencias.nodofog.mqtt

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * T3 — Publisher MQTT.
 *
 * Se conecta al broker Mosquitto al arrancar y publica las alertas con
 * QoS=2 (EXACTLY_ONCE) y retain=true, como pide la tarjeta T3.
 *
 * Usa HiveMQ MQTT Client (MQTT 5) para hablar el mismo protocolo que la app víctima.
 */
@Component
class MqttPublisher(
    @Value("\${mqtt.broker.host}") private val host: String,
    @Value("\${mqtt.broker.port}") private val port: Int
) {
    private val log = LoggerFactory.getLogger(MqttPublisher::class.java)
    private lateinit var client: Mqtt5BlockingClient

    @PostConstruct
    fun conectar() {
        client = MqttClient.builder()
            .useMqttVersion5()
            .identifier("nodo-fog-publisher")
            .serverHost(host)
            .serverPort(port)
            .automaticReconnectWithDefaultConfig()
            .buildBlocking()
        client.connect()
        log.info("MqttPublisher conectado al broker en {}:{}", host, port)
    }

    /**
     * Publica un mensaje en el topic indicado.
     * @param topic   ej. cinvestav/edificioA/alertas
     * @param payload JSON de la alerta
     */
    fun publicar(topic: String, payload: String) {
        client.publishWith()
            .topic(topic)
            .payload(payload.toByteArray(Charsets.UTF_8))
            .qos(MqttQos.EXACTLY_ONCE)   // QoS 2 (T3)
            .retain(true)                // retain=true (T3)
            .send()
        log.info("Alerta publicada en {} -> {}", topic, payload)
    }

    @PreDestroy
    fun cerrar() {
        if (::client.isInitialized) client.disconnect()
    }
}
