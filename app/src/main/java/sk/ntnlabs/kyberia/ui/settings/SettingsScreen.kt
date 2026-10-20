package sk.ntnlabs.kyberia.ui.settings

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import kotlinx.coroutines.launch
import sk.ntnlabs.kyberia.AppContainer
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.data.Settings
import sk.ntnlabs.kyberia.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val store = container.settings
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            SectionHeader(stringResource(R.string.settings_section_look))
            ChoiceRow(
                label = stringResource(R.string.settings_theme),
                value = themeLabel(settings.theme),
                options = ThemeMode.entries,
                optionLabel = { themeLabel(it) },
                onSelect = { mode -> store.update { it.copy(theme = mode) } },
            )
            Note(stringResource(R.string.theme_dim_note))
            SwitchRow(stringResource(R.string.settings_animate_avatars), settings.animateAvatars) { on ->
                store.update { it.copy(animateAvatars = on) }
            }
            SwitchRow(stringResource(R.string.settings_avatars_only_wifi), settings.avatarsOnlyWifi) { on ->
                store.update { it.copy(avatarsOnlyWifi = on) }
            }
            Note(stringResource(R.string.settings_avatars_note))
            SwitchRow(stringResource(R.string.settings_hide_nsfw), settings.hideNsfw) { on ->
                store.update { it.copy(hideNsfw = on) }
            }
            Note(stringResource(R.string.settings_hide_nsfw_note))

            SectionHeader(stringResource(R.string.settings_section_checks))
            ChoiceRow(
                label = stringResource(R.string.settings_interval),
                value = stringResource(R.string.settings_interval_value, settings.intervalMinutes),
                options = Settings.INTERVALS,
                optionLabel = { stringResource(R.string.settings_interval_value, it) },
                onSelect = { minutes ->
                    store.update { it.copy(intervalMinutes = minutes) }
                    container.scheduler.schedule()
                },
            )
            Note(stringResource(R.string.settings_interval_note))
            SwitchRow(stringResource(R.string.settings_refresh_on_open), settings.refreshOnOpen) { on ->
                store.update { it.copy(refreshOnOpen = on) }
            }

            SectionHeader(stringResource(R.string.settings_section_notify))
            SwitchRow(stringResource(R.string.settings_notify_replies), settings.notifyReplies) { on ->
                store.update { it.copy(notifyReplies = on) }
            }
            SwitchRow(stringResource(R.string.settings_notify_mail), settings.notifyMail) { on ->
                store.update { it.copy(notifyMail = on) }
            }
            SwitchRow(stringResource(R.string.settings_notify_bookmarks), settings.notifyBookmarks) { on ->
                store.update { it.copy(notifyBookmarks = on) }
            }
            SwitchRow(stringResource(R.string.settings_notify_friends), settings.notifyFriends) { on ->
                store.update { it.copy(notifyFriends = on) }
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_system_notifications)) },
                modifier = Modifier.clickable {
                    context.startActivity(
                        Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                },
            )

            SectionHeader(stringResource(R.string.settings_section_quiet))
            SwitchRow(stringResource(R.string.settings_quiet), settings.quietEnabled) { on ->
                store.update { it.copy(quietEnabled = on) }
            }
            ChoiceRow(
                label = stringResource(R.string.settings_quiet_from),
                value = hour(settings.quietFromHour),
                options = HOURS,
                optionLabel = { hour(it) },
                enabled = settings.quietEnabled,
                onSelect = { h -> store.update { it.copy(quietFromHour = h) } },
            )
            ChoiceRow(
                label = stringResource(R.string.settings_quiet_to),
                value = hour(settings.quietToHour),
                options = HOURS,
                optionLabel = { hour(it) },
                enabled = settings.quietEnabled,
                onSelect = { h -> store.update { it.copy(quietToHour = h) } },
            )
            Note(stringResource(R.string.settings_quiet_note))

            SectionHeader(stringResource(R.string.settings_section_account))
            ListItem(
                headlineContent = { Text(stringResource(R.string.logout), color = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { scope.launch { container.auth.logout() } },
            )

            // Verzia a diagnostika bežne netreba – zbalené, kým ich používateľ nerozbalí.
            var advanced by rememberSaveable { mutableStateOf(false) }
            ExpandableHeader(stringResource(R.string.settings_section_advanced), advanced) { advanced = !advanced }
            if (advanced) {
                DiagnosticsRows(container)
            }
        }
    }
}

private val HOURS = (0..23).toList()

@Composable
private fun themeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
        ThemeMode.DIM -> R.string.theme_dim
    },
)

private fun hour(h: Int): String = String.format(Locale.ROOT, "%02d:00", h)

@Composable
private fun SectionHeader(text: String) {
    HorizontalDivider()
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ExpandableHeader(text: String, expanded: Boolean, onToggle: () -> Unit) {
    HorizontalDivider()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painterResource(R.drawable.ic_expand_more),
            contentDescription = stringResource(if (expanded) R.string.settings_collapse else R.string.settings_expand),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.rotate(if (expanded) 180f else 0f),
        )
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
    )
}

@Composable
private fun <T> ChoiceRow(
    label: String,
    value: String,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ListItem(
            headlineContent = { Text(label) },
            trailingContent = {
                Text(
                    value,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
            },
            modifier = Modifier.clickable(enabled = enabled) { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}
