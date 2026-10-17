package sk.ntnlabs.kyberia.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.decode.BitmapFactoryDecoder
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import sk.ntnlabs.kyberia.ui.theme.LocalDimImages

/** Ako načítavať avatary – z nastavení a podľa toho, či je sieť práve meraná. */
data class AvatarOptions(val animate: Boolean = true, val network: Boolean = true)

val LocalAvatarOptions = compositionLocalOf { AvatarOptions() }

/** Avatar z kyberie; animované GIFy hrajú, obrázok sa po prvom stiahnutí berie z disku. */
@Composable
fun Avatar(url: String?, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val shaped = modifier
        .size(size)
        .clip(RoundedCornerShape(6.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
    if (url == null) {
        Box(shaped)
    } else {
        val options = LocalAvatarOptions.current
        val context = LocalPlatformContext.current
        val request = remember(url, options) {
            ImageRequest.Builder(context)
                .data(url)
                .apply {
                    if (!options.animate) {
                        // BitmapFactory z GIFu vezme len prvý snímok; vlastný kľúč, aby z pamäte neprišla animácia.
                        decoderFactory(STATIC)
                        memoryCacheKeyExtra("static", "1")
                    }
                    // Bez siete len z diskovej cache; čo tam nie je, ostane prázdne políčko.
                    if (!options.network) networkCachePolicy(CachePolicy.DISABLED)
                }
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = if (LocalDimImages.current) DIM else null,
            modifier = shaped,
        )
    }
}

private val STATIC = BitmapFactoryDecoder.Factory()

/** Tlmená téma: avatary na ~55 % jasu. */
private val DIM = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.55f, 0.55f, 0.55f, 1f) })
