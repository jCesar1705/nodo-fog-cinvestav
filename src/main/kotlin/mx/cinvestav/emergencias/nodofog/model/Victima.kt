package mx.cinvestav.emergencias.nodofog.model

import jakarta.persistence.*
import java.util.UUID

/**
 * Miembro de la IES registrado en este nodo FOG.
 * Los campos médicos se cifrarán con AES en producción (R8 — LFPDPPP).
 * Pueden transferirse entre nodos FOG cuando una víctima se desplace entre edificios (CA-07).
 */
@Entity
@Table(name = "victimas")
data class Victima(
    @Id val id: String = UUID.randomUUID().toString(),
    val nombre: String = "",
    @Column(unique = true) val matricula: String = "",
    // Datos médicos — TODO: cifrar con AES antes de persistir (R8)
    val tipoSangre: String = "",
    val alergias: String = "",
    val condicionesMedicas: String = "",
    val medicamentos: String = "",
    val edificioId: String = "",
    val consentimientoAceptado: Boolean = false,
    val timestampRegistro: Long = System.currentTimeMillis()
)
