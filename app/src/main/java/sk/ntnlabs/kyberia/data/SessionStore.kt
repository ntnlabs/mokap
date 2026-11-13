package sk.ntnlabs.kyberia.data

import android.content.Context
import android.util.Base64
import androidx.core.content.edit

/** Token session z kyberie (`anticsrf`) – kým platí session; null = nie je. */
interface SessionTokenStore {
    fun readToken(): String?
    fun writeToken(value: String?)
}

/**
 * Session cookies a token session (`anticsrf`, jeden pre celú session) šifrované kľúčom z Android Keystore,
 * uložené v SharedPreferences. Token zaniká so session: koniec cookies ho zmaže tiež.
 */
class SessionStore(context: Context) : CookieStorage, SessionTokenStore {

    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val cipher = KeystoreCipher("mokap_session")

    override fun read(): String? = decrypted(KEY_COOKIES)

    override fun write(value: String?) {
        val encoded = encrypted(value)
        prefs.edit {
            if (encoded == null) {
                remove(KEY_COOKIES)
                // Bez cookies niet session – ani jej tokenu.
                remove(KEY_TOKEN)
            } else {
                putString(KEY_COOKIES, encoded)
            }
        }
    }

    override fun readToken(): String? = decrypted(KEY_TOKEN)

    override fun writeToken(value: String?) {
        val encoded = encrypted(value)
        prefs.edit { if (encoded == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, encoded) }
    }

    private fun decrypted(key: String): String? {
        val encoded = prefs.getString(key, null) ?: return null
        return try {
            String(cipher.decrypt(Base64.decode(encoded, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (e: Exception) {
            // Kľúč zmizol (reset zámku obrazovky), poškodené dáta alebo chyba Keystore u výrobcu
            // (ProviderException, IOException) – session je stratená, treba sa prihlásiť. Pádom pri štarte nikdy.
            prefs.edit { remove(key) }
            null
        }
    }

    /** Bez Keystore sa nič neuloží (nikdy nešifrované) – session ostane len v pamäti. */
    private fun encrypted(value: String?): String? = value?.takeIf { it.isNotEmpty() }?.let {
        try {
            Base64.encodeToString(cipher.encrypt(it.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val KEY_COOKIES = "cookies"
        const val KEY_TOKEN = "anticsrf"
    }
}
