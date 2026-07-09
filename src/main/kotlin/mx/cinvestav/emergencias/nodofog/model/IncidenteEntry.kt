package mx.cinvestav.emergencias.nodofog.model

import jakarta.persistence.*
import java.util.UUID

/**
 * Incidente reportado durante una emergencia activa (CU-04).
 * La descripción se almacena cifrada con AES (ver AesService).
 */
@Entity
@Table(name = "incidente_entry")
data class IncidenteEntry(
    @Id val id: String = UUID.randomUUID().toString(),
    val tipo: String = "",
    @Column(columnDefinition = "TEXT") val descripcionCifrada: String = "",
    val reportadoPorId: String = "",
    val zonaId: String = "",
    val zonaNombre: String = "",
    val piso: Int = 0,
    val confianzaUbicacion: Float = 0f,
    val estado: String = "NUEVO",
    val emergenciaId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)