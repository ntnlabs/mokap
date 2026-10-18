package sk.ntnlabs.kyberia.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoadedPage(val page: BookmarksPage, val atMillis: Long)

/**
 * Posledná stiahnutá /id/19/ v pamäti. Zdieľa ju kontrola v pozadí aj appka – čo stiahne jedna,
 * ukáže aj druhá, a kyberia nedostane tú istú požiadavku dvakrát.
 */
class LatestPage {

    private val state = MutableStateFlow<LoadedPage?>(null)

    val flow: StateFlow<LoadedPage?> = state.asStateFlow()

    fun publish(page: BookmarksPage, atMillis: Long) {
        state.value = LoadedPage(page, atMillis)
    }

    /** Pri odhlásení – dáta predchádzajúceho účtu nesmú ostať v pamäti. */
    fun clear() {
        state.value = null
    }
}
