package mx.cinvestav.emergencias.nodofog.config

import mx.cinvestav.emergencias.nodofog.model.Victima
import mx.cinvestav.emergencias.nodofog.repository.VictimaRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * Carga datos de prueba al iniciar el sistema.
 * Los IDs deben coincidir con los VICTIMA_ID configurados en las apps víctima.
 *
 * En producción esto se reemplaza por un módulo de registro de usuarios.
 */
@Component
class DataInitializer(
    private val victimaRepository: VictimaRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(DataInitializer::class.java)

    private val rolPorMatricula = mapOf(
        "A07654321" to "USUARIO",
        "A01234567" to "BRIGADISTA",
        "EXT-001" to "USUARIO"
    )

    override fun run(args: ApplicationArguments) {
        if (victimaRepository.count() == 0L) {
            val hash = passwordEncoder.encode("1234")!!
            val victimas = listOf(
                Victima(
                    id = "victima-001",
                    nombre = "Diego Aguilar Hurtado",
                    matricula = "A01234567",
                    passwordHash = hash,
                    tipoSangre = "O+",
                    alergias = "Penicilina",
                    condicionesMedicas = "Ninguna",
                    medicamentos = "Ninguno",
                    edificioId = "edificioA",
                    consentimientoAceptado = true,
                    rol = rolPorMatricula["A01234567"]!!
                ),
                Victima(
                    id = "victima-002",
                    nombre = "Julio César Rocha Hernández",
                    matricula = "A07654321",
                    passwordHash = hash,
                    tipoSangre = "A+",
                    alergias = "Ninguna",
                    condicionesMedicas = "Ninguna",
                    medicamentos = "Ninguno",
                    edificioId = "edificioA",
                    consentimientoAceptado = true,
                    rol = rolPorMatricula["A07654321"]!!
                ),
                Victima(
                    id = "victima-003",
                    nombre = "Investigador Externo",
                    matricula = "EXT-001",
                    passwordHash = hash,
                    tipoSangre = "B+",
                    alergias = "Ninguna",
                    condicionesMedicas = "Hipertensión",
                    medicamentos = "Enalapril 10mg",
                    edificioId = "edificioA",
                    consentimientoAceptado = true,
                    rol = rolPorMatricula["EXT-001"]!!
                )
            )

            victimaRepository.saveAll(victimas)
            log.info("Datos de prueba cargados: {} víctimas registradas", victimas.size)
            return
        }

        // Si los usuarios ya existían de un arranque previo sin passwordHash, hashearla ahora.
        val sinPassword = victimaRepository.findAll().filter { it.passwordHash.isNullOrBlank() }
        if (sinPassword.isNotEmpty()) {
            val actualizados = sinPassword.map { it.copy(passwordHash = passwordEncoder.encode("1234")!!) }
            victimaRepository.saveAll(actualizados)
            log.info("passwordHash generado para {} víctimas existentes", actualizados.size)
        }

        // Si los usuarios ya existían de un arranque previo sin rol, asignarlo ahora.
        val sinRol = victimaRepository.findAll().filter { it.rol.isNullOrBlank() }
        if (sinRol.isNotEmpty()) {
            val actualizados = sinRol.map { it.copy(rol = rolPorMatricula[it.matricula] ?: "USUARIO") }
            victimaRepository.saveAll(actualizados)
            log.info("rol asignado para {} víctimas existentes", actualizados.size)
        }
    }
}
