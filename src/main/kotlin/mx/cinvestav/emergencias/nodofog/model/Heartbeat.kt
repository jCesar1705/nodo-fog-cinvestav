package mx.cinvestav.emergencias.nodofog.model

import jakarta.persistence.*
import java.util.UUID

/**
 * Señal de presencia ("estoy aquí") publicada por la app víctima vía MQTT
 * al topic cinvestav/{edificioId}/heartbeats cada 30 segundos (demo) / 5 min (producción).
 *
 * Al activarse una emergencia, el nodo consulta los heartbeats de los últimos
 * 20 minutos para generar el pase de lista (T4).
 */
@Entity
@Table(name = "heartbeats")
data class Heartbeat(
    @Id val id: String = UUID.randomUUID().toString(),
    @Column(nullable = false) val victimaId: String = "",
    @Column(nullable = false) val edificioId: String = "",
    @Column(nullable = false) val timestamp: Long = 0L
)
