package sk.ntnlabs.kyberia.data

import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import sk.ntnlabs.kyberia.UrlPolicy

/** Stiahnutá a rozparsovaná stránka kyberie. */
class Page(val url: String, val document: Document)

/** Jediný vstup appky na kyberiu: GET stránky a POST formulára, nič iné. */
class KyberiaClient(cookieJar: CookieJar, private val userAgent: String) {

    private val http = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun get(url: String): Page = execute(Request.Builder().url(url).get())

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

    private suspend fun execute(builder: Request.Builder): Page = withContext(Dispatchers.IO) {
        val request = builder.header("User-Agent", userAgent).build()
        val url = request.url.toString()
        if (!UrlPolicy.isInternal(url)) throw IOException("Mimo kyberie: ${request.url.host}")
        http.newCall(request).execute().use { response ->
            // Aj stránku „nemáš práva“ (napr. po odhlásení) chceme rozparsovať, chyba je len 5xx.
            if (response.code >= 500) throw IOException("HTTP ${response.code}")
            val finalUrl = response.request.url.toString()
            val charset = response.body.contentType()?.charset()?.name()
            Page(finalUrl, Jsoup.parse(response.body.byteStream(), charset, finalUrl))
        }
    }
}
