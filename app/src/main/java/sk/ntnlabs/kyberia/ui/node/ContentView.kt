package sk.ntnlabs.kyberia.ui.node

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import java.net.URI
import kotlinx.coroutines.launch
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.parse.ContentBlock
import sk.ntnlabs.kyberia.parse.Embeds
import sk.ntnlabs.kyberia.parse.TextSpan
import sk.ntnlabs.kyberia.ui.common.LocalAvatarOptions
import sk.ntnlabs.kyberia.ui.common.LocalImageSaver
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
    // Ťuknutý obrázok na celú obrazovku (zväčšenie, uloženie do galérie).
    var viewing by remember { mutableStateOf<ContentBlock.Image?>(null) }
    SelectionContainer(modifier) {
        ContentBlocks(blocks, onLink, externalImages, onImage = { viewing = it })
    }
    viewing?.let { image -> ImageViewer(image, externalImages, onDismiss = { viewing = null }) }
}

@Composable
private fun ContentBlocks(
    blocks: List<ContentBlock>,
    onLink: (String) -> Unit,
    externalImages: ImageLoader?,
    onImage: (ContentBlock.Image) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is ContentBlock.Text -> Text(annotated(block.spans, onLink), style = MaterialTheme.typography.bodyMedium)
                is ContentBlock.Image -> when {
                    block.internal -> KyberiaImage(block.url, onClick = { onImage(block) })
                    externalImages != null -> ExternalImage(block.url, externalImages, onClick = { onImage(block) }) {
                        MediaLink(R.string.node_image, block.url, onLink)
                    }
                    else -> MediaLink(R.string.node_image, block.url, onLink)
                }
                is ContentBlock.Embed -> {
                    val target = Embeds.openUrl(block.url)
                    val thumbnail = Embeds.thumbnail(block.url)
                    if (externalImages != null && thumbnail != null) {
                        Column(Modifier.clickable { onLink(target) }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ExternalImage(thumbnail, externalImages, onClick = null) {}
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
private fun KyberiaImage(url: String, onClick: () -> Unit) {
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
        modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick),
    )
}

/** Cudzí obrázok – zvlášť klient bez cookies; keď sa nenačíta (http, chyba), ukáže sa [fallback]. [onClick] null = náhľad videa. */
@Composable
private fun ExternalImage(url: String, loader: ImageLoader, onClick: (() -> Unit)?, fallback: @Composable () -> Unit) {
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
        modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).clip(RoundedCornerShape(6.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        error = { fallback() },
    )
}

/**
 * Obrázok na celú obrazovku: zväčšenie prstami, „Uložiť“ do galérie (Obrázky/mokap) a zatvorenie. Načíta sa tým
 * istým ImageLoaderom ako v príspevku – z cache, bez požiadavky navyše.
 */
@Composable
private fun ImageViewer(image: ContentBlock.Image, externalImages: ImageLoader?, onDismiss: () -> Unit) {
    val saver = LocalImageSaver.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
        offset = if (scale == 1f) Offset.Zero else offset + pan
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val imageModifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                .transformable(transform)
            val dim = if (LocalDimImages.current) DIM else null
            if (image.internal || externalImages == null) {
                AsyncImage(model = image.url, contentDescription = null, contentScale = ContentScale.Fit, colorFilter = dim, modifier = imageModifier)
            } else {
                AsyncImage(
                    model = image.url,
                    contentDescription = null,
                    imageLoader = externalImages,
                    contentScale = ContentScale.Fit,
                    colorFilter = dim,
                    modifier = imageModifier,
                )
            }
            Row(Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(8.dp)) {
                if (saver != null) {
                    IconButton(
                        enabled = !saving,
                        onClick = {
                            saving = true
                            scope.launch {
                                val saved = saver.save(image.url, image.internal)
                                saving = false
                                val text = resources.getString(if (saved) R.string.image_saved else R.string.image_save_failed)
                                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                            }
                        },
                    ) {
                        Icon(painterResource(R.drawable.ic_download), contentDescription = stringResource(R.string.image_save), tint = Color.White)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.image_close), tint = Color.White)
                }
            }
        }
    }
}

private const val MAX_ZOOM = 5f

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
