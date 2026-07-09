package mx.cinvestav.emergencias.nodofog.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cifrado AES-256-GCM para datos sensibles (R8 — LFPDPPP).
 * Si no hay llave configurada (fog.aes.key vacío) opera en modo passthrough,
 * para no bloquear ambientes de desarrollo sin la variable de entorno.
 */
@Service
class AesService(@Value("\${fog.aes.key:}") private val keyB64: String) {

    private val key: SecretKey? by lazy {
        if (keyB64.isBlank()) null
        else SecretKeySpec(Base64.getDecoder().decode(keyB64), "AES")
    }

    fun cifrar(texto: String): String {
        if (key == null || texto.isBlank()) return texto
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val cifrado = cipher.doFinal(texto.toByteArray())
        return Base64.getEncoder().encodeToString(iv + cifrado)
    }

    fun descifrar(b64: String): String {
        if (key == null || b64.isBlank()) return b64
        return try {
            val combinado = Base64.getDecoder().decode(b64)
            val iv = combinado.sliceArray(0..11)
            val cifrado = combinado.sliceArray(12 until combinado.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cifrado))
        } catch (e: Exception) { b64 }
    }
}