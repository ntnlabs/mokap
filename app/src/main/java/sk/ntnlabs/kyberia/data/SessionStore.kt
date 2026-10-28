package sk.ntnlabs.kyberia.data

import android.content.Context
import android.util.Base64
import androidx.core.content.edit

/** Session cookies šifrované kľúčom z Android Keystore, uložené v SharedPreferences. */
class SessionStore(context: Context) : CookieStorage {

    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val cipher = KeystoreCipher("mokap_session")

    override fun read(): String? {
        val encoded = prefs.getString(KEY_COOKIES, null) ?: return null
        return try {
            String(cipher.decrypt(Base64.decode(encoded, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (e: Exception) {
            // Kľúč zmizol (reset zámku obrazovky), poškodené dáta alebo chyba Keystore u výrobcu
            // (ProviderException, IOException) – session je stratená, treba sa prihlásiť. Pádom pri štarte nikdy.
            prefs.edit { remove(KEY_COOKIES) }
            null
        }
    }

    override fun write(value: String?) {
        val encoded = value?.takeIf { it.isNotEmpty() }?.let {
            try {
                Base64.encodeToString(cipher.encrypt(it.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
            } catch (e: Exception) {
                // Bez Keystore sa cookies neuložia vôbec (nikdy nešifrované) – session ostane len v pamäti.
                null
            }
        }
        prefs.edit { if (encoded == null) remove(KEY_COOKIES) else putString(KEY_COOKIES, encoded) }
    }

    private companion object {
        const val KEY_COOKIES = "cookies"
    }
}
