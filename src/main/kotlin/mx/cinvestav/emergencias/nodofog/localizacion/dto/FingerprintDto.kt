package mx.cinvestav.emergencias.nodofog.localizacion.dto

/**
 * Payload que envía la app víctima al FOG con su escaneo WiFi.
 * La app mide el RSSI de todos los APs visibles y los manda junto
 * con el piso estimado por el sensor barométrico.
 */
data class FingerprintRequest(
    val victimaId : String,
    val piso      : Int = 0,
    val fingerprint: List<BssidRssi>
)

data class BssidRssi(
    val bssid: String,   // MAC del AP, ej. "AA:BB:CC:DD:EE:FF"
    val rssi : Int       // señal recibida, ej. -65 dBm
)

/**
 * Payload para guardar un punto de calibración del radio map.
 * El admin (o cualquier usuario con un modo calibración) camina el edificio
 * y registra el fingerprint de cada zona conocida.
 */
data class CalibracionRequest(
    val zonaId     : String,
    val zonaNombre : String,
    val piso       : Int = 0,
    val fingerprint: List<BssidRssi>
)

/** Respuesta al POST /api/ubicacion — posición estimada de la víctima. */
data class UbicacionResponse(
    val victimaId   : String,
    val zonaEstimada: String,
    val zonaNombre  : String,
    val piso        : Int,
    val confianza   : Float,   // 0.0-1.0 (1.0 = k vecinos todos en la misma zona)
    val timestamp   : Long
)
