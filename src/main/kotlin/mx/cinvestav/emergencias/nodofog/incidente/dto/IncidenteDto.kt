package mx.cinvestav.emergencias.nodofog.incidente.dto

data class CrearIncidenteRequest(
    val tipo: String,
    val descripcion: String = "",
    val reportadoPorId: String,
    val zonaId: String = "",
    val zonaNombre: String = "",
    val piso: Int = 0,
    val confianzaUbicacion: Float = 0f
)

data class CambiarEstadoRequest(
    val estado: String
)