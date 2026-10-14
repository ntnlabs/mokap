package sk.ntnlabs.kyberia.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

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
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = shaped,
        )
    }
}
