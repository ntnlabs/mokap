package sk.ntnlabs.kyberia.ui.common

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.staticCompositionLocalOf
import coil3.ImageLoader
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Request
import okio.Buffer
import sk.ntnlabs.kyberia.UrlPolicy
import sk.ntnlabs.kyberia.data.ImageType

/**
 * Uloží obrázok z príspevku či pošty do galérie (Obrázky/mokap) – len na ťuknutie používateľa. Cez MediaStore,
 * takže na Androide 10+ netreba žiadne oprávnenie a appka do galérie nič iné nezapisuje.
 *
 * Najprv vezme obrázok z diskovej cache (už je stiahnutý, keď ho používateľ vidí) – žiadna požiadavka navyše.
 * Až keď tam nie je, stiahne ho tým istým klientom ako pri zobrazení: obrázky z kyberie len cez kyberiu,
 * cudzie cez klienta bez cookies (a len keď sú v nastaveniach zapnuté, inak sa ani nezobrazia).
 */
class ImageSaver(
    private val context: Context,
    private val kyberiaCalls: Call.Factory,
    private val kyberiaLoader: () -> ImageLoader,
    private val externalCalls: () -> Call.Factory,
    private val externalLoader: () -> ImageLoader,
) {

    /** true = uložené. */
    suspend fun save(url: String, internal: Boolean): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (internal && !UrlPolicy.isInternal(url)) throw IOException("Nie je z kyberie")
            val bytes = cached(url, if (internal) kyberiaLoader() else externalLoader())
                ?: download(url, if (internal) kyberiaCalls else externalCalls())
            val type = ImageType.sniff(bytes) ?: throw IOException("Nie je obrázok")
            write(bytes, type)
        }.isSuccess
    }

    private fun cached(url: String, loader: ImageLoader): ByteArray? {
        val cache = loader.diskCache ?: return null
        return cache.openSnapshot(url)?.use { snapshot ->
            cache.fileSystem.read(snapshot.data) { readByteArray() }
        }
    }

    private fun download(url: String, calls: Call.Factory): ByteArray =
        calls.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            if (response.body.contentLength() > ImageType.MAX_BYTES) throw IOException("Priveľký obrázok")
            val buffer = Buffer()
            response.body.source().use { source ->
                while (buffer.size <= ImageType.MAX_BYTES && source.read(buffer, CHUNK) != -1L) Unit
            }
            if (buffer.size > ImageType.MAX_BYTES) throw IOException("Priveľký obrázok")
            buffer.readByteArray()
        }

    private fun write(bytes: ByteArray, type: ImageType) {
        val resolver = context.contentResolver
        val name = "mokap_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date()) + "." + type.extension
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, type.mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/mokap")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: throw IOException("MediaStore odmietol súbor")
        try {
            val stream = resolver.openOutputStream(uri) ?: throw IOException("Súbor sa nedá otvoriť")
            stream.use { it.write(bytes) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            // Nedokončený súbor v galérii nenechávať.
            resolver.delete(uri, null, null)
            throw e
        }
    }

    private companion object {
        const val CHUNK = 64L * 1024
    }
}

/** Ukladanie obrázkov – v appke vždy je (MainActivity), v náhľadoch nie. */
val LocalImageSaver = staticCompositionLocalOf<ImageSaver?> { null }
