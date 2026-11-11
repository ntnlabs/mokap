package sk.ntnlabs.kyberia.data

/**
 * Typ obrázka podľa prvých bajtov (nie podľa URL či hlavičky) – do galérie sa uloží len skutočný obrázok,
 * nikdy chybová HTML stránka.
 */
enum class ImageType(val mime: String, val extension: String) {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    GIF("image/gif", "gif"),
    WEBP("image/webp", "webp"),
    ;

    companion object {
        /** Najväčší obrázok, ktorý appka uloží – ochrana pamäte telefónu. */
        const val MAX_BYTES = 25L * 1024 * 1024

        fun sniff(bytes: ByteArray): ImageType? = when {
            bytes.startsWith(0xFF, 0xD8, 0xFF) -> JPEG
            bytes.startsWith(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> PNG
            bytes.startsWith(0x47, 0x49, 0x46, 0x38) -> GIF
            bytes.size >= 12 && bytes.startsWith(0x52, 0x49, 0x46, 0x46) &&
                bytes.copyOfRange(8, 12).contentEquals("WEBP".toByteArray(Charsets.US_ASCII)) -> WEBP
            else -> null
        }

        private fun ByteArray.startsWith(vararg prefix: Int): Boolean =
            size >= prefix.size && prefix.indices.all { this[it] == prefix[it].toByte() }
    }
}
