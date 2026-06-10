package mx.cinvestav.emergencias.nodofog.repository

import mx.cinvestav.emergencias.nodofog.model.Heartbeat
import mx.cinvestav.emergencias.nodofog.model.PaseListaEntry
import mx.cinvestav.emergencias.nodofog.model.Victima
import org.springframework.data.jpa.repository.JpaRepository

interface VictimaRepository : JpaRepository<Victima, String> {
    /** Buscar por matrícula — usado en el login (AuthController). */
    fun findByMatricula(matricula: String): Victima?
}

interface HeartbeatRepository : JpaRepository<Heartbeat, String> {
    /** Heartbeats con timestamp >= [desde] — usado para la ventana de 20 min (T4). */
    fun findByTimestampGreaterThanEqual(desde: Long): List<Heartbeat>
}

interface PaseListaRepository : JpaRepository<PaseListaEntry, String> {
    fun findByEmergenciaId(emergenciaId: String): List<PaseListaEntry>
    fun findByVictimaIdAndEmergenciaId(victimaId: String, emergenciaId: String): PaseListaEntry?
}
