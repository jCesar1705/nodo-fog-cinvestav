package mx.cinvestav.emergencias.nodofog.model

import jakarta.persistence.*

@Entity
@Table(name = "salud_entry")
data class SaludEntry(
    @Id val victimaId: String = "",
    val tipoSangreCifrado: String = "",
    val alergiasCifrado: String = "",
    val condicionesCifrado: String = "",
    val consentimientoLFPDPPP: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
