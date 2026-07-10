package mx.cinvestav.emergencias.nodofog.incidente

import com.google.gson.Gson
import mx.cinvestav.emergencias.nodofog.config.EmergenciaState
import mx.cinvestav.emergencias.nodofog.incidente.dto.CrearIncidenteRequest
import mx.cinvestav.emergencias.nodofog.model.IncidenteEntry
import mx.cinvestav.emergencias.nodofog.mqtt.MqttPublisher
import mx.cinvestav.emergencias.nodofog.repository.IncidenteRepository
import mx.cinvestav.emergencias.nodofog.security.AesService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Alta y consulta de incidentes reportados durante una emergencia (CU-04).
 * La descripción se cifra con AES antes de persistirse y se descifra al listar.
 */
@Service
class IncidenteService(
    private val incidenteRepo: IncidenteRepository,
    private val mqttPublisher: MqttPublisher,
    private val emergenciaState: EmergenciaState,
    private val aesService: AesService,
    @Value("\${fog.node-id}") private val nodeId: String
) {
    private val log = LoggerFactory.getLogger(IncidenteService::class.java)
    private val gson = Gson()

    fun crear(req: CrearIncidenteRequest): IncidenteEntry {
        val entry = IncidenteEntry(
            tipo               = req.tipo,
            descripcionCifrada = aesService.cifrar(req.descripcion),
            reportadoPorId     = req.reportadoPorId,
            zonaId             = req.zonaId,
            zonaNombre         = req.zonaNombre,
            piso               = req.piso,
            confianzaUbicacion = req.confianzaUbicacion,
            emergenciaId       = emergenciaState.emergenciaId ?: ""
        )
        val guardado = incidenteRepo.save(entry)
        log.info("Incidente registrado — id={} tipo={} zona='{}' reportadoPor={}",
            guardado.id, guardado.tipo, guardado.zonaNombre, guardado.reportadoPorId)

        // Publicar MQTT solo para brigadistas
        val payload = gson.toJson(mapOf(
            "id"                 to guardado.id,
            "tipo"               to guardado.tipo,
            "descripcion"        to req.descripcion,
            "reportadoPorId"     to guardado.reportadoPorId,
            "zonaId"             to guardado.zonaId,
            "zonaNombre"         to guardado.zonaNombre,
            "piso"               to guardado.piso,
            "confianzaUbicacion" to guardado.confianzaUbicacion,
            "estado"             to guardado.estado,
            "timestamp"          to guardado.timestamp
        ))
        mqttPublisher.publicar("cinvestav/$nodeId/incidentes", payload)
        return guardado
    }

    fun listar(): List<Map<String, Any>> = incidenteRepo.findAll().map {
        mapOf(
            "id"                 to it.id,
            "tipo"               to it.tipo,
            "descripcion"        to aesService.descifrar(it.descripcionCifrada),
            "reportadoPorId"     to it.reportadoPorId,
            "zonaId"             to it.zonaId,
            "zonaNombre"         to it.zonaNombre,
            "piso"               to it.piso,
            "confianzaUbicacion" to it.confianzaUbicacion,
            "estado"             to it.estado,
            "timestamp"          to it.timestamp
        )
    }

    fun cambiarEstado(id: String, nuevoEstado: String): IncidenteEntry? {
        val entry = incidenteRepo.findById(id).orElse(null) ?: return null
        return incidenteRepo.save(entry.copy(estado = nuevoEstado))
    }

    fun limpiarIncidentes() {
        val total = incidenteRepo.count()
        incidenteRepo.deleteAll()
        log.info("Incidentes eliminados al finalizar emergencia — {} registros borrados (R7)", total)
    }
}
