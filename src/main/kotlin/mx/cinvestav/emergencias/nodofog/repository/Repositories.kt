package mx.cinvestav.emergencias.nodofog.repository

import mx.cinvestav.emergencias.nodofog.model.*
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

// ── Sprint 3: localización indoor (CU-02, CU-05) ─────────────────────────────

/** Posiciones activas de víctimas durante la emergencia. */
interface UbicacionRepository : JpaRepository<UbicacionEntry, String>

/** Radio map de calibración para k-NN. */
interface RadioMapRepository : JpaRepository<RadioMapEntry, Long> {
    fun findByZonaId(zonaId: String): List<RadioMapEntry>
}

// ── Sprint 4: incidentes (CU-04) ─────────────────────────────────────────────

interface IncidenteRepository : JpaRepository<IncidenteEntry, String>
