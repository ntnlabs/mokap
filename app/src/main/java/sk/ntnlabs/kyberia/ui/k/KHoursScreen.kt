package sk.ntnlabs.kyberia.ui.k

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.data.Nsfw

/** Tvoje príspevky s K za posledných [hours] hodín (nastavenie v „Pokročilé“). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KHoursScreen(viewModel: KHoursViewModel, hours: Int, hideNsfw: Boolean, onOpenNode: (Long) -> Unit) {
    // Len pri otvorení záložky (alebo po zmene intervalu) – nikdy v pozadí.
    LaunchedEffect(hours) { viewModel.refreshIfStale(hours) }
    val state = viewModel.state
    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = { viewModel.refresh(hours) },
        modifier = Modifier.fillMaxSize(),
    ) {
        MyKList(
            state.items.orEmpty().filterNot { hideNsfw && Nsfw.matches(it.title) },
            header = stringResource(R.string.k_hours_header, hours),
            empty = when {
                state.error -> stringResource(R.string.k_hours_error)
                state.items != null && !state.loading -> stringResource(R.string.k_hours_empty, hours)
                else -> null
            },
            onOpenNode = onOpenNode,
        )
    }
}
