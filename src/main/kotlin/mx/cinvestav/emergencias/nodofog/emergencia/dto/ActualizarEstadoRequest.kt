package mx.cinvestav.emergencias.nodofog.emergencia.dto

data class ActualizarEstadoRequest(
    val estado: String,                                       // PRESENTE | EN_ZONA_SEGURA | AUSENTE | FUERA_DE_LA_IES
    val brigadistaId: String = "brigadista-desconocido"
)
