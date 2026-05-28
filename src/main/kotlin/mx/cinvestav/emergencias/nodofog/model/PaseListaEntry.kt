package mx.cinvestav.emergencias.nodofog.model

import jakarta.persistence.*
import java.util.UUID

/**
 * Entrada del pase de lista generado al activar la emergencia (T4).
 *
 * El [estado] puede ser modificado manualmente por un brigadista (CU-07):
 *   PRESENTE → EN_ZONA_SEGURA | AUSENTE | FUERA_DE_LA_IES
 *
 * Se registra quién cambió el estado y cuándo (trazabilidad).
 */
@Entity
@Table(name = "pase_lista")
data class PaseListaEntry(
    @Id val id: String = UUID.randomUUID().toString(),
    @Column(nullable = false) val emergenciaId: String = "",
    @Column(nullable = false) val victimaId: String = "",
    val nombre: String = "",
    val matricula: String = "",
    val edificioId: String = "",
    val ultimoHeartbeat: Long = 0L,
    // Modificable por brigadista (CU-07)
    var estado: String = "PRESENTE",
    var brigadistaIdCambio: String? = null,
    var timestampCambioEstado: Long? = null
)
