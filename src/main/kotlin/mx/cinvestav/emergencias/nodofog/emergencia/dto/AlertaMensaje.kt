package mx.cinvestav.emergencias.nodofog.emergencia.dto

/**
 * Mensaje de alerta que se publica al topic cinvestav/{nodeId}/alertas.
 *
 * IMPORTANTE: la estructura debe coincidir con el AlertaDto que parsea la app víctima
 * (campos: id, tipo, severidad, mensaje, timestamp). Si cambia aquí, cambia allá.
 */
data class AlertaMensaje(
    val id: String,          // emergenciaId (T3)
    val tipo: String,        // SISMICA | SIMULACRO
    val severidad: String,   // BAJA | MEDIA | ALTA
    val mensaje: String,
    val timestamp: Long      // epoch millis (T3)
)
