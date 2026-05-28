package mx.cinvestav.emergencias.nodofog.heartbeat

import com.google.gson.Gson
import mx.cinvestav.emergencias.nodofog.model.Heartbeat
import mx.cinvestav.emergencias.nodofog.repository.HeartbeatRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Procesa los heartbeats recibidos desde las apps víctima vía MQTT
 * y los persiste en la BD para la generación del pase de lista (T4).
 */
@Service
class HeartbeatService(
    private val heartbeatRepository: HeartbeatRepository
) {
    private val log = LoggerFactory.getLogger(HeartbeatService::class.java)
    private val gson = Gson()

    fun procesar(payload: String) {
        runCatching {
            val dto = gson.fromJson(payload, HeartbeatDto::class.java)
            if (dto.victimaId.isBlank()) {
                log.warn("Heartbeat sin victimaId ignorado: {}", payload)
                return
            }
            val heartbeat = Heartbeat(
                victimaId = dto.victimaId,
                edificioId = dto.edificioId.ifBlank { "desconocido" },
                timestamp = if (dto.timestamp > 0) dto.timestamp else System.currentTimeMillis()
            )
            heartbeatRepository.save(heartbeat)
            log.info("Heartbeat guardado — victimaId: {}, edificio: {}", dto.victimaId, dto.edificioId)
        }.onFailure {
            log.error("Error al procesar heartbeat: {}", payload, it)
        }
    }

    private data class HeartbeatDto(
        val victimaId: String = "",
        val edificioId: String = "",
        val timestamp: Long = 0L
    )
}
