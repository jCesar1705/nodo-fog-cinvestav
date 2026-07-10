package mx.cinvestav.emergencias.nodofog.salud

import mx.cinvestav.emergencias.nodofog.model.SaludEntry
import mx.cinvestav.emergencias.nodofog.repository.SaludRepository
import mx.cinvestav.emergencias.nodofog.salud.dto.RegistrarSaludRequest
import mx.cinvestav.emergencias.nodofog.security.AesService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SaludService(
    private val saludRepo: SaludRepository,
    private val aesService: AesService
) {
    private val log = LoggerFactory.getLogger(SaludService::class.java)

    fun registrar(req: RegistrarSaludRequest): SaludEntry {
        val entry = SaludEntry(
            victimaId              = req.victimaId,
            tipoSangreCifrado      = aesService.cifrar(req.tipoSangre),
            alergiasCifrado        = aesService.cifrar(req.alergias),
            condicionesCifrado     = aesService.cifrar(req.condicionesMedicas),
            consentimientoLFPDPPP  = req.consentimientoLFPDPPP,
            timestamp              = System.currentTimeMillis()
        )
        val guardado = saludRepo.save(entry)
        log.info("Datos médicos registrados — victimaId={} consentimiento={}",
            req.victimaId, req.consentimientoLFPDPPP)
        return guardado
    }

    fun obtener(victimaId: String): Map<String, Any>? {
        val entry = saludRepo.findById(victimaId).orElse(null) ?: return null
        return mapOf(
            "victimaId"             to entry.victimaId,
            "tipoSangre"            to aesService.descifrar(entry.tipoSangreCifrado),
            "alergias"              to aesService.descifrar(entry.alergiasCifrado),
            "condicionesMedicas"    to aesService.descifrar(entry.condicionesCifrado),
            "consentimientoLFPDPPP" to entry.consentimientoLFPDPPP,
            "timestamp"             to entry.timestamp
        )
    }

    fun limpiarTodos() {
        saludRepo.deleteAll()
        // NO limpiar — los datos de salud son permanentes, no se borran al terminar emergencia
        // Solo las ubicaciones e incidentes se limpian (R7 aplica a datos de localización)
    }
}
