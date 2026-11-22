package sk.ntnlabs.kyberia.ui.settings

import android.content.ClipData
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import sk.ntnlabs.kyberia.AppContainer
import sk.ntnlabs.kyberia.BuildConfig
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.data.CheckEntry
import sk.ntnlabs.kyberia.data.CheckResult
import sk.ntnlabs.kyberia.data.CheckTrigger
import sk.ntnlabs.kyberia.data.Durations

/**
 * Verzia a diagnostika aj pre odhláseného – nech je vidieť, prečo kyberia odhlásila.
 * Diagnostika sa pri odhlásení nemaže a neobsahuje osobné údaje.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(container: AppContainer, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.for_developers)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            DiagnosticsRows(container)
        }
    }
}

/**
 * Verzia, posledné kontroly, cookies a história – v nastaveniach aj na prihlasovacej obrazovke. Text sa dá označiť
 * a „Kopírovať diagnostiku“ ho celý vloží do schránky (do hlásenia chyby); appka ho nikam neposiela.
 */
@Composable
fun DiagnosticsRows(container: AppContainer) {
    val diagnostics by container.diagnostics.diagnostics.collectAsStateWithLifecycle()
    // „Verzia: 0.7.0114“ a pod tým kódové meno v zátvorke.
    val version = if (BuildConfig.DEBUG) "${BuildConfig.APP_VERSION} · debug" else BuildConfig.APP_VERSION
    val never = stringResource(R.string.diag_never)
    val cookies = remember(diagnostics) { container.cookieExpiry() }
    val lastLogout = diagnostics.lastExpired?.let { expired ->
        val before = diagnostics.okBeforeExpiredMillis?.let { ok ->
            "\n" + stringResource(R.string.diag_ok_before, time(ok), Durations.short((expired.atMillis - ok) / 1000))
        }.orEmpty()
        "${time(expired.atMillis)} – ${describe(expired)}$before"
    } ?: never
    val rows = buildList {
        add(stringResource(R.string.settings_version, version) to stringResource(R.string.settings_version_codename, BuildConfig.CODENAME))
        add(stringResource(R.string.diag_last_check) to (diagnostics.last?.let { "${time(it.atMillis)} – ${describe(it)}" } ?: never))
        add(stringResource(R.string.diag_last_ok) to (diagnostics.lastOkMillis?.let(::time) ?: never))
        add(stringResource(R.string.diag_last_logout) to lastLogout)
        add(
            stringResource(R.string.diag_cookie) to when {
                cookies.count == 0 -> stringResource(R.string.diag_cookie_none)
                cookies.earliestMillis == null -> stringResource(R.string.diag_cookie_session)
                else -> stringResource(R.string.diag_cookie_until, time(cookies.earliestMillis))
            },
        )
        if (diagnostics.history.isNotEmpty()) {
            // Najnovšie hore; „+X“ = koľko času ubehlo od predchádzajúcej kontroly.
            val lines = diagnostics.history.mapIndexed { index, entry ->
                val gap = diagnostics.history.getOrNull(index - 1)?.let { previous ->
                    " · +" + Durations.short((entry.atMillis - previous.atMillis) / 1000)
                }.orEmpty()
                "${time(entry.atMillis)} – ${describe(entry)}$gap"
            }.asReversed()
            add(stringResource(R.string.diag_history) to lines.joinToString("\n"))
        }
    }
    CopyButton(rows.joinToString("\n\n") { (label, value) -> "$label\n$value" })
    SelectionContainer {
        Column {
            rows.forEach { (label, value) -> InfoRow(label, value) }
        }
    }
}

/** Celá diagnostika do schránky; Android 13+ to potvrdí sám, staršie krátkou hláškou. */
@Composable
private fun CopyButton(text: String) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val label = stringResource(R.string.diag_copy)
    val copied = stringResource(R.string.diag_copied)
    OutlinedButton(
        onClick = {
            scope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(label, text)))
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                }
            }
        },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(label)
    }
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M. HH:mm").withZone(ZoneId.systemDefault())

private fun time(millis: Long): String = TIME_FORMAT.format(Instant.ofEpochMilli(millis))

@Composable
private fun describe(entry: CheckEntry): String {
    val text = when (entry.result) {
        CheckResult.OK -> stringResource(R.string.diag_ok)
        CheckResult.ERROR -> stringResource(R.string.diag_error, entry.detail)
        CheckResult.EXPIRED -> stringResource(R.string.diag_expired, entry.detail)
    }
    return when (entry.trigger) {
        CheckTrigger.SCHEDULED -> text
        CheckTrigger.APP -> "$text (${stringResource(R.string.diag_app)})"
        CheckTrigger.KEEPALIVE -> "$text (${stringResource(R.string.diag_keepalive)})"
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant) },
    )
}
