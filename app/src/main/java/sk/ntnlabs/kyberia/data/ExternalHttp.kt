package sk.ntnlabs.kyberia.data

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

/**
 * Klient len pre obrázky a náhľady videí z cudzích stránok – a len keď ich používateľ v nastaveniach
 * zapne (predvolene nie). Úplne oddelený od kyberie: bez cookies (session nikdy nejde inam), len https.
 */
object ExternalHttp {

    fun client(): OkHttpClient = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .addNetworkInterceptor { chain ->
            val request = chain.request()
            // Aj pri presmerovaní – nešifrovane nič.
            if (!request.url.isHttps) throw IOException("Cudzí obrázok len cez https")
            chain.proceed(request)
        }
        .build()
}
