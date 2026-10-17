package sk.ntnlabs.kyberia.data

import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import sk.ntnlabs.kyberia.UrlPolicy

/** Stiahnutá a rozparsovaná stránka kyberie. */
class Page(val url: String, val code: Int, val document: Document)

/** Jediný vstup appky na kyberiu: GET stránky a POST formulára, nič iné. */
class KyberiaClient(cookieJar: CookieJar, userAgent: String) {

    // Každá požiadavka vrátane presmerovaní ide len na kyberiu; inam sa ani neodošle.
    private val http = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .callTimeout(30, TimeUnit.SECONDS)
        .addNetworkInterceptor { chain ->
            val request = chain.request()
            if (!UrlPolicy.isInternal(request.url.toString())) throw IOException("Mimo kyberie: ${request.url.host}")
            chain.proceed(request.newBuilder().header("User-Agent", userAgent).build())
        }
        .build()

    /** Pre obrázky (avatary): ten istý klient – cookies, UA aj obmedzenie na kyberiu. */
    val imageCallFactory: Call.Factory = http

    suspend fun get(url: String): Page = execute(Request.Builder().url(url).get())

    /** Krátka textová odpoveď (AJAX endpoint), bez parsovania HTML. */
    suspend fun getText(url: String): String = call(Request.Builder().url(url).get()) { response ->
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
        response.body.string()
    }

    /** Odošle formulár tak, ako by ho poslal prehliadač (urlencoded alebo multipart podľa `enctype`). */
    suspend fun postForm(
        url: String,
        fields: List<Pair<String, String>>,
        charset: Charset,
        multipart: Boolean,
    ): Page {
        val body = if (multipart) {
            MultipartBody.Builder().setType(MultipartBody.FORM).apply {
                fields.forEach { (name, value) -> addFormDataPart(name, value) }
            }.build()
        } else {
            FormBody.Builder(charset).apply {
                fields.forEach { (name, value) -> add(name, value) }
            }.build()
        }
        return execute(Request.Builder().url(url).post(body))
    }

    private suspend fun execute(builder: Request.Builder): Page = call(builder) { response ->
        // Aj stránku „nemáš práva“ (napr. po odhlásení) chceme rozparsovať, chyba je len 5xx.
        if (response.code >= 500) throw IOException("HTTP ${response.code}")
        val finalUrl = response.request.url.toString()
        val charset = response.body.contentType()?.charset()?.name()
        Page(finalUrl, response.code, Jsoup.parse(response.body.byteStream(), charset, finalUrl))
    }

    private suspend fun <T> call(builder: Request.Builder, read: (Response) -> T): T = withContext(Dispatchers.IO) {
        val request = builder.build()
        if (!UrlPolicy.isInternal(request.url.toString())) throw IOException("Mimo kyberie: ${request.url.host}")
        http.newCall(request).execute().use(read)
    }
}
