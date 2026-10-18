package sk.ntnlabs.kyberia.data

/** Kedy je čas na ďalšie obnovenie – pre lištu nad záložkami aj pre kontrolu v pozadí. Čistý Kotlin. */
object RefreshTimer {

    /** Rezerva pre kontrolu v pozadí: WorkManager nespúšťa presne na minútu. */
    const val SLACK_MILLIS = 2 * 60_000L

    /** 0..1 – koľko z intervalu ubehlo od posledného načítania; bez načítania 0. */
    fun progress(loadedAtMillis: Long?, nowMillis: Long, intervalMillis: Long): Float {
        if (loadedAtMillis == null || intervalMillis <= 0) return 0f
        return ((nowMillis - loadedAtMillis).toFloat() / intervalMillis).coerceIn(0f, 1f)
    }

    fun isDue(loadedAtMillis: Long?, nowMillis: Long, intervalMillis: Long): Boolean =
        loadedAtMillis == null || nowMillis - loadedAtMillis >= intervalMillis

    /** Kontrola v pozadí je zbytočná, ak sa stránka stiahla nedávno (napr. v otvorenej appke). */
    fun recentlyLoaded(lastOkMillis: Long?, nowMillis: Long, intervalMillis: Long): Boolean =
        lastOkMillis != null && nowMillis - lastOkMillis in 0 until intervalMillis - SLACK_MILLIS
}
