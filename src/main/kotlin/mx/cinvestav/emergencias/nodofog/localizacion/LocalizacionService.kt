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
        const val PENALTY_RSSI = 40f   // penalización si un AP calibrado no aparece en el escaneo actual
    }

    fun procesarUbicacion(req: FingerprintRequest): UbicacionResponse {

        log.info("Fingerprint recibido: victimaId={} APs={}", req.victimaId, req.fingerprint.size)

        val entradasRadioMap = radioMapRepo.findAll()

        val zonaEstimada = if (entradasRadioMap.isEmpty()) {
            ZonaKnn("DESCONOCIDA", "Zona desconocida", 0f)
        } else {
            estimarConKnn(req.fingerprint, entradasRadioMap)
        }

        val entrada = UbicacionEntry(
            victimaId  = req.victimaId,
            zonaId     = zonaEstimada.id,
            zonaNombre = zonaEstimada.nombre,
            piso       = req.piso,
            confianza  = zonaEstimada.confianza,
            timestamp  = System.currentTimeMillis()
        )
        ubicacionRepo.save(entrada)

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
     * FIX: se itera sobre los BSSIDs de la REFERENCIA (radio map, ~5 APs),
     * no sobre los APs del escaneo medido (que puede tener 40-70 APs en
     * entornos densos como CINVESTAV). Iterar sobre el escaneo completo
     * diluye la señal real con decenas de penalizaciones de APs que
     * ninguna zona tiene calibrados, haciendo que la clasificación
     * dependa más de "cuántos AP coincidieron" que de la similitud real
     * de RSSI entre las zonas candidatas.
     */
    private fun estimarConKnn(
        fingerprint: List<BssidRssi>,
        radioMap   : List<RadioMapEntry>
    ): ZonaKnn {

        val medidoMap = fingerprint.associate { it.bssid to it.rssi }

        val distancias = radioMap.map { entrada ->
            val ref = parsearFingerprint(entrada.fingerprint)
            val dist = distanciaEuclidiana(ref, medidoMap)
            Triple(entrada.zonaId, entrada.zonaNombre, dist)
        }.sortedBy { it.third }

        val vecinos = distancias.take(K)

        // Voto ponderado por distancia inversa: un vecino muy cercano (dist baja)
        // pesa mucho más que uno lejano, en vez de contar cada voto por igual.
        // Esto evita que una zona con más puntos de calibración "le gane" a la
        // zona con el match real más fuerte solo por tener más entradas.
        val pesoPorZona = vecinos
            .groupBy { it.first }
            .mapValues { (_, items) -> items.sumOf { (1.0 / (it.third + 1f)).toDouble() } }

        val zonaIdGanadora = pesoPorZona.maxByOrNull { it.value }!!.key
        val zonaNombre = vecinos.first { it.first == zonaIdGanadora }.second
        val pesoTotal = pesoPorZona.values.sum()
        val confianza = (pesoPorZona[zonaIdGanadora]!! / pesoTotal).toFloat()

        log.info("k-NN distancias: {}", distancias.map { "${it.second}=${"%.1f".format(it.third)}" })

        return ZonaKnn(zonaIdGanadora, zonaNombre, confianza)
    }

    /**
     * Distancia Euclidiana iterando sobre los APs de REFERENCIA (calibración).
     * Para cada AP calibrado: si aparece en el escaneo medido, diferencia real;
     * si no aparece, penalización fija (el AP de esa zona ya no se ve).
     */
    private fun distanciaEuclidiana(
        referencia: List<BssidRssi>,
        medido    : Map<String, Int>
    ): Float {
        var suma = 0f
        referencia.forEach { ref ->
            val rssiMedido = medido[ref.bssid]
            suma += if (rssiMedido != null) {
                (rssiMedido - ref.rssi).toFloat().pow(2)
            } else {
                PENALTY_RSSI.pow(2)
            }
        }
        return sqrt(suma)
    }

    private fun parsearFingerprint(json: String): List<BssidRssi> {
        val tipo = object : TypeToken<List<BssidRssi>>() {}.type
        return try { gson.fromJson(json, tipo) } catch (e: Exception) { emptyList() }
    }

    fun limpiarUbicaciones() {
        ubicacionRepo.deleteAll()
        log.info("Ubicaciones de víctimas eliminadas al finalizar la emergencia (R7)")
    }

    private data class ZonaKnn(val id: String, val nombre: String, val confianza: Float)
}