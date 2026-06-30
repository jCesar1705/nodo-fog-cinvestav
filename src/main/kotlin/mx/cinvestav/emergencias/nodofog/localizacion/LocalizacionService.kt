package mx.cinvestav.emergencias.nodofog.localizacion

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import mx.cinvestav.emergencias.nodofog.localizacion.dto.BssidRssi
import mx.cinvestav.emergencias.nodofog.localizacion.dto.FingerprintRequest
import mx.cinvestav.emergencias.nodofog.localizacion.dto.UbicacionResponse
import mx.cinvestav.emergencias.nodofog.model.RadioMapEntry
import mx.cinvestav.emergencias.nodofog.model.UbicacionEntry
import mx.cinvestav.emergencias.nodofog.mqtt.MqttPublisher
import mx.cinvestav.emergencias.nodofog.repository.RadioMapRepository
import mx.cinvestav.emergencias.nodofog.repository.UbicacionRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.math.pow
import kotlin.math.sqrt

@Service
class LocalizacionService(
    private val radioMapRepo  : RadioMapRepository,
    private val ubicacionRepo : UbicacionRepository,
    private val mqttPublisher : MqttPublisher,
    @Value("\${fog.node-id}") private val nodeId: String
) {
    private val log  = LoggerFactory.getLogger(LocalizacionService::class.java)
    private val gson = Gson()

    companion object {
        const val K = 3                // vecinos más cercanos
        const val PENALTY_RSSI = 100f  // penalización si el AP no está en la referencia
    }

    /**
     * Recibe el fingerprint de una víctima, estima su zona con k-NN
     * y publica la posición por MQTT para que la app brigadista actualice el plano.
     */
    fun procesarUbicacion(req: FingerprintRequest): UbicacionResponse {

        log.info("Fingerprint recibido: victimaId={} APs={} datos={}",
            req.victimaId, req.fingerprint.size, req.fingerprint)

        val entradasRadioMap = radioMapRepo.findAll()

        val zonaEstimada = if (entradasRadioMap.isEmpty()) {
            // Sin radio map — zona desconocida (calibrar primero)
            ZonaKnn("DESCONOCIDA", "Zona desconocida", 0f)
        } else {
            estimarConKnn(req.fingerprint, entradasRadioMap)
        }

        // Guardar o actualizar la posición de esta víctima
        val entrada = UbicacionEntry(
            victimaId  = req.victimaId,
            zonaId     = zonaEstimada.id,
            zonaNombre = zonaEstimada.nombre,
            piso       = req.piso,
            confianza  = zonaEstimada.confianza,
            timestamp  = System.currentTimeMillis()
        )
        ubicacionRepo.save(entrada)

        // Publicar MQTT para que la app brigadista actualice el plano (QA-02, ≤3s)
        val payload = gson.toJson(mapOf(
            "victimaId"  to req.victimaId,
            "zona"       to zonaEstimada.nombre,
            "piso"       to req.piso,
            "confianza"  to zonaEstimada.confianza,
            "timestamp"  to entrada.timestamp
        ))
        mqttPublisher.publicar("cinvestav/$nodeId/ubicaciones", payload)

        log.info("Ubicacion estimada: victima={} zona='{}' piso={} confianza={}",
            req.victimaId, zonaEstimada.nombre, req.piso, zonaEstimada.confianza)

        return UbicacionResponse(
            victimaId    = req.victimaId,
            zonaEstimada = zonaEstimada.id,
            zonaNombre   = zonaEstimada.nombre,
            piso         = req.piso,
            confianza    = zonaEstimada.confianza,
            timestamp    = entrada.timestamp
        )
    }

    /**
     * Algoritmo k-NN en espacio de RSSI.
     *
     * Para cada entrada del radio map calcula la distancia Euclidiana
     * al fingerprint recibido. Toma los K más cercanos y vota por zona.
     *
     * Distancia entre dos fingerprints:
     *   d = √ Σ (rssi_medido - rssi_referencia)²
     * Si un BSSID no está en la referencia: penalización de 100 (AP muy distante).
     */
    private fun estimarConKnn(
        fingerprint: List<BssidRssi>,
        radioMap   : List<RadioMapEntry>
    ): ZonaKnn {

        val distancias = radioMap.map { entrada ->
            val ref = parsearFingerprint(entrada.fingerprint)
            val dist = distanciaEuclidiana(fingerprint, ref)
            Triple(entrada.zonaId, entrada.zonaNombre, dist)
        }.sortedBy { it.third }

        // Tomar los K vecinos más cercanos
        val vecinos = distancias.take(K)

        // Votar por zona (mayoría simple)
        val votos = vecinos.groupBy { it.first }
        val ganador = votos.maxByOrNull { it.value.size }!!
        val zonaId     = ganador.key
        val zonaNombre = ganador.value.first().second
        val confianza  = ganador.value.size.toFloat() / vecinos.size.toFloat()

        return ZonaKnn(zonaId, zonaNombre, confianza)
    }

    private fun distanciaEuclidiana(
        medido   : List<BssidRssi>,
        referencia: List<BssidRssi>
    ): Float {
        var suma = 0f
        medido.forEach { m ->
            val ref = referencia.find { it.bssid == m.bssid }
            suma += if (ref != null) {
                (m.rssi - ref.rssi).toFloat().pow(2)
            } else {
                PENALTY_RSSI.pow(2)   // AP no en el radio map → penalizar
            }
        }
        return sqrt(suma)
    }

    private fun parsearFingerprint(json: String): List<BssidRssi> {
        val tipo = object : TypeToken<List<BssidRssi>>() {}.type
        return try { gson.fromJson(json, tipo) } catch (e: Exception) { emptyList() }
    }

    /** Elimina todas las posiciones activas al terminar la emergencia (R7). */
    fun limpiarUbicaciones() {
        ubicacionRepo.deleteAll()
        log.info("Ubicaciones de víctimas eliminadas al finalizar la emergencia (R7)")
    }

    private data class ZonaKnn(val id: String, val nombre: String, val confianza: Float)
}
