package mx.cinvestav.emergencias.nodofog.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Extrae y valida el JWT del header Authorization en cada request,
 * poblando el SecurityContext cuando el token es válido (Sprint 4).
 */
@Component
class JwtFilter(private val jwtUtil: JwtUtil) : OncePerRequestFilter() {

    override fun doFilterInternal(req: HttpServletRequest,
                                  res: HttpServletResponse,
                                  chain: FilterChain) {
        val header = req.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val token = header.removePrefix("Bearer ").trim()
            val claims = jwtUtil.validar(token)
            if (claims != null) {
                val rol = claims["rol", String::class.java] ?: "USUARIO"
                val auth = UsernamePasswordAuthenticationToken(
                    claims.subject, null,
                    listOf(SimpleGrantedAuthority("ROLE_$rol"))
                )
                SecurityContextHolder.getContext().authentication = auth
                req.setAttribute("jwt_rol", rol)
                req.setAttribute("jwt_subject", claims.subject)
            }
        }
        chain.doFilter(req, res)
    }
}