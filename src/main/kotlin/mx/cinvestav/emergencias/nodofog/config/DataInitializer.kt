package mx.cinvestav.emergencias.nodofog.config

import mx.cinvestav.emergencias.nodofog.model.Victima
import mx.cinvestav.emergencias.nodofog.repository.VictimaRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * Carga datos de prueba al iniciar el sistema.
 * Los IDs deben coincidir con los VICTIMA_ID configurados en las apps víctima.
 *
 * En producción esto se reemplaza por un módulo de registro de usuarios.
 */
@Component
class DataInitializer(
    private val victimaRepository: VictimaRepository
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(DataInitializer::class.java)

    override fun run(args: ApplicationArguments) {
        if (victimaRepository.count() > 0L) return

        val victimas = listOf(
            Victima(
                id = "victima-001",
                nombre = "Diego Aguilar Hurtado",
                matricula = "A01234567",
                tipoSangre = "O+",
                alergias = "Penicilina",
                condicionesMedicas = "Ninguna",
                medicamentos = "Ninguno",
                edificioId = "edificioA",
                consentimientoAceptado = true
            ),
            Victima(
                id = "victima-002",
                nombre = "Julio César Rocha Hernández",
                matricula = "A07654321",
                tipoSangre = "A+",
                alergias = "Ninguna",
                condicionesMedicas = "Ninguna",
                medicamentos = "Ninguno",
                edificioId = "edificioA",
                consentimientoAceptado = true
            ),
            Victima(
                id = "victima-003",
                nombre = "Investigador Externo",
                matricula = "EXT-001",
                tipoSangre = "B+",
                alergias = "Ninguna",
                condicionesMedicas = "Hipertensión",
                medicamentos = "Enalapril 10mg",
                edificioId = "edificioA",
                consentimientoAceptado = true
            )
        )

        victimaRepository.saveAll(victimas)
        log.info("Datos de prueba cargados: {} víctimas registradas", victimas.size)
    }
}
