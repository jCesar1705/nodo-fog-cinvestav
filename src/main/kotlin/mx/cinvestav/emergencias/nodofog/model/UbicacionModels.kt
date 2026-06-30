package mx.cinvestav.emergencias.nodofog.model

import jakarta.persistence.*

/**
 * Posición estimada de una víctima durante una emergencia activa.
 * Se crea/actualiza cada vez que la app envía un nuevo fingerprint.
 * Se borra al finalizar la emergencia (CA-07, R7).
 */
@Entity
@Table(name = "ubicacion_entry")
data class UbicacionEntry(
    @Id
    val victimaId   : String = "",

    /** ID de la zona estimada por k-NN (ej. "zona-1"). */
    val zonaId      : String = "",

    val zonaNombre  : String = "",

    val piso        : Int = 0,

    /** Confianza del k-NN: proporción de los k vecinos que coinciden (0.0-1.0). */
    val confianza   : Float = 0f,

    val timestamp   : Long = System.currentTimeMillis()
)

/**
 * Punto de calibración del radio map.
 * Cada entrada almacena el fingerprint promedio de una zona del edificio.
 * El fingerprint se guarda como JSON: [{"bssid":"AA:BB:..","rssi":-65}, ...]
 */
@Entity
@Table(name = "radio_map_entry")
data class RadioMapEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val zonaId    : String = "",

    @Column(nullable = false)
    val zonaNombre: String = "",

    val piso      : Int = 0,

    /** JSON con la lista de {bssid, rssi} de referencia para esta zona. */
    @Column(columnDefinition = "TEXT", nullable = false)
    val fingerprint: String = "[]"
)
