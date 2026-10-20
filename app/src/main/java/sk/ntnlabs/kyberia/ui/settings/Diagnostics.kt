package sk.ntnlabs.kyberia.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

/** Verzia, posledné kontroly, cookies a história – v nastaveniach aj na prihlasovacej obrazovke. */
@Composable
fun DiagnosticsRows(container: AppContainer) {
    val diagnostics by container.diagnostics.diagnostics.collectAsStateWithLifecycle()
    // „Verzia: 0.7.0114“ a pod tým kódové meno v zátvorke.
    val version = if (BuildConfig.DEBUG) "${BuildConfig.APP_VERSION} · debug" else BuildConfig.APP_VERSION
    InfoRow(
        stringResource(R.string.settings_version, version),
        stringResource(R.string.settings_version_codename, BuildConfig.CODENAME),
    )
    val never = stringResource(R.string.diag_never)
    InfoRow(stringResource(R.string.diag_last_check), diagnostics.last?.let { "${time(it.atMillis)} – ${describe(it)}" } ?: never)
    InfoRow(stringResource(R.string.diag_last_ok), diagnostics.lastOkMillis?.let(::time) ?: never)
    InfoRow(
        stringResource(R.string.diag_last_logout),
        diagnostics.lastExpired?.let { expired ->
            val before = diagnostics.okBeforeExpiredMillis?.let { ok ->
                "\n" + stringResource(R.string.diag_ok_before, time(ok), Durations.short((expired.atMillis - ok) / 1000))
            }.orEmpty()
            "${time(expired.atMillis)} – ${describe(expired)}$before"
        } ?: never,
    )
    val cookies = remember(diagnostics) { container.cookieExpiry() }
    InfoRow(
        stringResource(R.string.diag_cookie),
        when {
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
        InfoRow(stringResource(R.string.diag_history), lines.joinToString("\n"))
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
