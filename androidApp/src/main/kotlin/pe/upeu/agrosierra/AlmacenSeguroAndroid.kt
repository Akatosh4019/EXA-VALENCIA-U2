package pe.upeu.agrosierra

import pe.upeu.agrosierra.data.local.AlmacenSeguro

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * El archivo de preferencias solo contiene texto cifrado. La clave AES
 * no se exporta y queda custodiada por Android Keystore.
 */
class AlmacenSeguroAndroid(context: Context) : AlmacenSeguro {
    private val preferencias = context.getSharedPreferences("sesion_segura", Context.MODE_PRIVATE)

    override fun guardarToken(token: String) {
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
        cifrador.init(Cipher.ENCRYPT_MODE, clave())
        val iv = Base64.encodeToString(cifrador.iv, Base64.NO_WRAP)
        val datos = Base64.encodeToString(cifrador.doFinal(token.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        preferencias.edit().putString(CLAVE_PREF, "$iv.$datos").apply()
    }

    override fun leerToken(): String? {
        val guardado = preferencias.getString(CLAVE_PREF, null) ?: return null
        return try {
            val partes = guardado.split('.', limit = 2)
            require(partes.size == 2)
            val descifrador = Cipher.getInstance("AES/GCM/NoPadding")
            descifrador.init(
                Cipher.DECRYPT_MODE,
                clave(),
                GCMParameterSpec(128, Base64.decode(partes[0], Base64.NO_WRAP)),
            )
            String(descifrador.doFinal(Base64.decode(partes[1], Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (_: Exception) {
            // Una copia de seguridad restaurada sin la clave original no es una sesión válida.
            borrarToken()
            null
        }
    }

    override fun borrarToken() {
        preferencias.edit().remove(CLAVE_PREF).apply()
    }

    private fun clave(): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }

    private companion object {
        const val ALIAS = "agrocampo_jwt_aes"
        const val CLAVE_PREF = "jwt_cifrado"
    }
}
