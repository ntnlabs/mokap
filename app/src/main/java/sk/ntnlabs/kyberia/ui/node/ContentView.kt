package sk.ntnlabs.kyberia.ui.node

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import java.net.URI
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.parse.ContentBlock
import sk.ntnlabs.kyberia.parse.Embeds
import sk.ntnlabs.kyberia.parse.TextSpan
import sk.ntnlabs.kyberia.ui.common.LocalAvatarOptions
import sk.ntnlabs.kyberia.ui.theme.LocalDimImages

/**
 * Obsah príspevku či správy z [ContentBlock]: text s odkazmi a obrázky z kyberie. Cudzie obrázky a náhľady videí
 * len s [externalImages] (nastavenie, predvolene vypnuté) – inak odkaz; video sa vždy otvorí mimo appky.
 * Text sa dá označiť a skopírovať (dlhé podržanie) – v pošte, nodoch aj Top K.
 */
@Composable
fun ContentView(
    blocks: List<ContentBlock>,
    onLink: (String) -> Unit,
    externalImages: ImageLoader?,
    modifier: Modifier = Modifier,
) {
    SelectionContainer(modifier) {
        ContentBlocks(blocks, onLink, externalImages)
    }
}

@Composable
private fun ContentBlocks(blocks: List<ContentBlock>, onLink: (String) -> Unit, externalImages: ImageLoader?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is ContentBlock.Text -> Text(annotated(block.spans, onLink), style = MaterialTheme.typography.bodyMedium)
                is ContentBlock.Image -> when {
                    block.internal -> KyberiaImage(block.url)
                    externalImages != null -> ExternalImage(block.url, externalImages) { MediaLink(R.string.node_image, block.url, onLink) }
                    else -> MediaLink(R.string.node_image, block.url, onLink)
                }
                is ContentBlock.Embed -> {
                    val target = Embeds.openUrl(block.url)
                    val thumbnail = Embeds.thumbnail(block.url)
                    if (externalImages != null && thumbnail != null) {
                        Column(Modifier.clickable { onLink(target) }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ExternalImage(thumbnail, externalImages) {}
                            MediaLink(R.string.node_embed, target, onLink)
                        }
                    } else {
                        MediaLink(R.string.node_embed, target, onLink)
                    }
                }
            }
        }
    }
}

@Composable
private fun annotated(spans: List<TextSpan>, onLink: (String) -> Unit): AnnotatedString {
    val linkStyle = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline))
    return buildAnnotatedString {
        spans.forEach { span ->
            val style = SpanStyle(
                fontWeight = if (span.bold) FontWeight.Bold else null,
                fontStyle = if (span.italic) FontStyle.Italic else null,
            )
            val link = span.link
            if (link == null) {
                withStyle(style) { append(span.text) }
            } else {
                withLink(LinkAnnotation.Clickable(link, linkStyle) { onLink(link) }) {
                    withStyle(style) { append(span.text) }
                }
            }
        }
    }
}

/** Obrázok z kyberie – ten istý klient ako avatary (len kyberia), rovnaké „len cez Wi-Fi“ a tlmenie. */
@Composable
private fun KyberiaImage(url: String) {
    val options = LocalAvatarOptions.current
    val context = LocalPlatformContext.current
    val request = remember(url, options.network) {
        ImageRequest.Builder(context)
            .data(url)
            .apply { if (!options.network) networkCachePolicy(CachePolicy.DISABLED) }
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = if (LocalDimImages.current) DIM else null,
        modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).clip(RoundedCornerShape(6.dp)),
    )
}

/** Cudzí obrázok – zvlášť klient bez cookies; keď sa nenačíta (http, chyba), ukáže sa [fallback]. */
@Composable
private fun ExternalImage(url: String, loader: ImageLoader, fallback: @Composable () -> Unit) {
    val options = LocalAvatarOptions.current
    val context = LocalPlatformContext.current
    val request = remember(url, options.network) {
        ImageRequest.Builder(context)
            .data(url)
            .apply { if (!options.network) networkCachePolicy(CachePolicy.DISABLED) }
            .build()
    }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        imageLoader = loader,
        contentScale = ContentScale.Fit,
        colorFilter = if (LocalDimImages.current) DIM else null,
        modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).clip(RoundedCornerShape(6.dp)),
        error = { fallback() },
    )
}

/** Cudzí obrázok alebo video – len odkaz s doménou, otvorí sa v prehliadači. */
@Composable
private fun MediaLink(@StringRes label: Int, url: String, onLink: (String) -> Unit) {
    val host = remember(url) { runCatching { URI(url).host }.getOrNull()?.removePrefix("www.") ?: url }
    Text(
        stringResource(label, host),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable { onLink(url) }.padding(vertical = 4.dp),
    )
}

private val DIM = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.55f, 0.55f, 0.55f, 1f) })
