package mx.cinvestav.emergencias.nodofog.config

import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Estado en memoria de la emergencia activa.
 * Singleton gestionado por Spring — un solo estado por nodo FOG.
 * En producción se persistiría en BD para sobrevivir reinicios del proceso.
 */
@Component
class EmergenciaState {

    final var activa: Boolean = false
        private set

    final var emergenciaId: String? = null
        private set

    /** Activa la emergencia y devuelve el ID generado. */
    fun activar(): String {
        val id = UUID.randomUUID().toString()
        emergenciaId = id
        activa = true
        return id
    }

    /** Desactiva la emergencia. El [emergenciaId] se conserva para consultas históricas. */
    fun desactivar() {
        activa = false
    }
}
