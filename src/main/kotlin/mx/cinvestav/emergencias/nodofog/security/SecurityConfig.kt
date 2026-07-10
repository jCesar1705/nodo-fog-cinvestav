package mx.cinvestav.emergencias.nodofog.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Configuración de seguridad stateless con JWT (Sprint 4).
 * Las rutas públicas se mantienen sin cambios de comportamiento respecto
 * a sprints anteriores; el resto exige un JWT válido.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(private val jwtFilter: JwtFilter) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
            .authorizeHttpRequests { auth ->
                auth
                    // Rutas públicas — no requieren JWT
                    .requestMatchers("/api/auth/login").permitAll()
                    .requestMatchers("/api/emergencia/activar").permitAll()
                    .requestMatchers("/api/emergencia/desactivar").permitAll()
                    .requestMatchers("/api/alertas/**").permitAll()
                    .requestMatchers("/config/**").permitAll()
                    .requestMatchers("/h2/**").permitAll()
                    // Heartbeat público (spec Sprint 4)
                    .requestMatchers("/api/victimas/heartbeat").permitAll()
                    // La víctima registra sus datos médicos con su propio JWT (CU-03)
                    .requestMatchers(HttpMethod.POST, "/api/salud").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/radiomap").permitAll()
                    // Todo lo demás requiere JWT válido
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
            .headers { it.frameOptions { fo -> fo.disable() } }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
