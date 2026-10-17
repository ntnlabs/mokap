package sk.ntnlabs.kyberia.data

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import java.security.GeneralSecurityException

/** Session cookies šifrované kľúčom z Android Keystore, uložené v SharedPreferences. */
class SessionStore(context: Context) : CookieStorage {

    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val cipher = KeystoreCipher("mokap_session")

    override fun read(): String? {
        val encoded = prefs.getString(KEY_COOKIES, null) ?: return null
        return try {
            String(cipher.decrypt(Base64.decode(encoded, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            // Kľúč zmizol (napr. reset zámku obrazovky) – session je stratená, treba sa prihlásiť.
            prefs.edit { remove(KEY_COOKIES) }
            null
        } catch (e: IllegalArgumentException) {
            prefs.edit { remove(KEY_COOKIES) }
            null
        }
    }

    override fun write(value: String?) {
        prefs.edit {
            if (value.isNullOrEmpty()) {
                remove(KEY_COOKIES)
            } else {
                val encrypted = cipher.encrypt(value.toByteArray(Charsets.UTF_8))
                putString(KEY_COOKIES, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            }
        }
    }

    private companion object {
        const val KEY_COOKIES = "cookies"
    }
}
