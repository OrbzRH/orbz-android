package app.orbz.android.store

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
 * The session cookie and the signed-in address, encrypted with a key that lives in the Android Keystore and
 * never leaves the device's secure hardware. The file itself is app-private; the encryption is for a backup,
 * a rooted phone or a stray copy of the file, none of which can read it without the key.
 */
class SecureStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("orbz", Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    private fun seal(plain: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key())
        val ct = c.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(c.iv + ct, Base64.NO_WRAP)
    }

    private fun open(sealed: String): String? = runCatching {
        val raw = Base64.decode(sealed, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw, 0, 12))
        String(c.doFinal(raw, 12, raw.size - 12), Charsets.UTF_8)
    }.getOrNull()

    private fun put(k: String, v: String?) = prefs.edit().apply { if (v == null) remove(k) else putString(k, seal(v)) }.apply()
    private fun read(k: String): String? = prefs.getString(k, null)?.let { open(it) }

    fun cookie(): String? = read(COOKIE)
    fun saveCookie(v: String) = put(COOKIE, v)
    fun clearCookie() = put(COOKIE, null)

    fun address(): String? = read(ADDRESS)
    fun saveAddress(v: String) = put(ADDRESS, v)

    /** An API key from the console's Keys page, the other way in. Same encryption as the cookie. */
    fun apiKey(): String? = read(KEY)
    fun saveApiKey(v: String) = put(KEY, v)
    fun clearApiKey() = put(KEY, null)

    fun clearAll() = prefs.edit().clear().apply()

    private companion object {
        const val ALIAS = "orbz.session"
        const val COOKIE = "cookie"
        const val ADDRESS = "address"
        const val KEY = "apikey"
    }
}
