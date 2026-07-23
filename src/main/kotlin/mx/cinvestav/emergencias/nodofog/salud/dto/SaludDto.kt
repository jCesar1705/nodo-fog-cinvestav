package mx.cinvestav.emergencias.nodofog.salud.dto

data class RegistrarSaludRequest(
    val victimaId: String,
    val tipoSangre: String = "",
    val alergias: String = "",
    val condicionesMedicas: String = "",
    val medicamentos: String = "",
    val consentimientoLFPDPPP: Boolean = false
)
