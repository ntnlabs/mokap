package sk.ntnlabs.kyberia.data

import okhttp3.CookieJar
import org.junit.Assert.assertSame
import org.junit.Test

class ExternalHttpTest {

    @Test
    fun noCookiesEverLeaveForeignHosts() {
        // Kyberia session ide len cez KyberiaClient; klient na cudzie obrázky nemá žiadne cookies.
        assertSame(CookieJar.NO_COOKIES, ExternalHttp.client().cookieJar)
    }
}
