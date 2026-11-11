package sk.ntnlabs.kyberia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageTypeTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun sniffsImages() {
        assertEquals(ImageType.JPEG, ImageType.sniff(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00)))
        assertEquals(ImageType.PNG, ImageType.sniff(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00)))
        assertEquals(ImageType.GIF, ImageType.sniff("GIF89a....".toByteArray()))
        assertEquals(ImageType.WEBP, ImageType.sniff("RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".toByteArray(Charsets.ISO_8859_1)))
        assertEquals("jpg", ImageType.JPEG.extension)
        assertEquals("image/gif", ImageType.GIF.mime)
    }

    @Test
    fun rejectsNonImages() {
        assertNull(ImageType.sniff("<html><body>nemáš práva</body></html>".toByteArray()))
        assertNull(ImageType.sniff("RIFF\u0000\u0000\u0000\u0000WAVE".toByteArray(Charsets.ISO_8859_1)))
        assertNull(ImageType.sniff(ByteArray(0)))
        assertNull(ImageType.sniff(bytes(0xFF, 0xD8)))
    }
}
