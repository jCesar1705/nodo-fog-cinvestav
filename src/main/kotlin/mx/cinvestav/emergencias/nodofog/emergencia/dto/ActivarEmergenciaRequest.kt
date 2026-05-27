package mx.cinvestav.emergencias.nodofog.emergencia.dto

/**
 * Cuerpo de la petición POST /api/emergencia/activar.
 * Todos los campos tienen valor por defecto: una activación mínima
 * (solo con el header de rol) genera una alerta sísmica estándar.
 */
data class ActivarEmergenciaRequest(
    val tipo: String = "SISMICA",
    val severidad: String = "ALTA",
    val mensaje: String = "Alerta sísmica. Diríjase a la zona segura más cercana."
)
