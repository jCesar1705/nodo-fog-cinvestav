package mx.cinvestav.emergencias.nodofog.emergencia

import mx.cinvestav.emergencias.nodofog.config.EmergenciaState
import mx.cinvestav.emergencias.nodofog.model.PaseListaEntry
import mx.cinvestav.emergencias.nodofog.repository.HeartbeatRepository
import mx.cinvestav.emergencias.nodofog.repository.PaseListaRepository
import mx.cinvestav.emergencias.nodofog.repository.VictimaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PaseListaService(
    private val heartbeatRepository: HeartbeatRepository,
    private val paseListaRepository: PaseListaRepository,
    private val victimaRepository: VictimaRepository,
    private val emergenciaState: EmergenciaState
) {
    private val log = LoggerFactory.getLogger(PaseListaService::class.java)

    /**
     * T4 — Genera el pase de lista al activar la emergencia.
     * Consulta heartbeats con timestamp >= now()-20min, agrupa por víctima
     * y toma el más reciente de cada una.
     */
    fun generar(emergenciaId: String): List<PaseListaEntry> {
        val hace20min = System.currentTimeMillis() - (20 * 60 * 1000)
        val recientes = heartbeatRepository.findByTimestampGreaterThanEqual(hace20min)

        val porVictima = recientes
            .groupBy { it.victimaId }
            .map { (_, hbs) -> hbs.maxByOrNull { it.timestamp }!! }

        log.info("Generando pase de lista — {} usuarios con señal en últimos 20 min", porVictima.size)

        val entradas = porVictima.map { hb ->
            val victima = victimaRepository.findById(hb.victimaId).orElse(null)
            PaseListaEntry(
                emergenciaId = emergenciaId,
                victimaId = hb.victimaId,
                nombre = victima?.nombre ?: "Usuario desconocido",
                matricula = victima?.matricula ?: "",
                edificioId = hb.edificioId,
                ultimoHeartbeat = hb.timestamp,
                estado = "PRESENTE"
            )
        }

        return paseListaRepository.saveAll(entradas)
    }

    /** Devuelve el pase de lista de la emergencia activa. */
    fun obtenerActual(): List<PaseListaEntry> {
        val id = emergenciaState.emergenciaId ?: return emptyList()
        return paseListaRepository.findByEmergenciaId(id)
    }

    /**
     * CU-07 — El brigadista actualiza el estado de una víctima manualmente.
     * Registra quién hizo el cambio y cuándo.
     */
    fun actualizarEstado(
        victimaId: String,
        nuevoEstado: String,
        brigadistaId: String
    ): PaseListaEntry? {
        val emergenciaId = emergenciaState.emergenciaId ?: return null
        val entrada = paseListaRepository
            .findByVictimaIdAndEmergenciaId(victimaId, emergenciaId) ?: return null

        entrada.estado = nuevoEstado
        entrada.brigadistaIdCambio = brigadistaId
        entrada.timestampCambioEstado = System.currentTimeMillis()

        return paseListaRepository.save(entrada)
    }
}
