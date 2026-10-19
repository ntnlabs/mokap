package sk.ntnlabs.kyberia.data

/**
 * Kyberia odhlási pri načítaní stránky, ak od predošlého ubehli viac ako 2 hodiny
 * (`LOGOUT_IDLE_SEC = 7200` a `$_SESSION['last_request']` v jej zdrojákoch; kontrola pošty
 * cez AJAX sa nepočíta). Android pravidelné kontroly v spánku odkladá aj na hodiny,
 * preto strážca: budík od posledného načítania, ktorý sa pri bežných kontrolách nikdy nespustí.
 */
object SessionGuard {

    const val SERVER_IDLE_LOGOUT_MILLIS = 2 * 60 * 60_000L

    /** Kedy po poslednom načítaní zasiahnuť – s rezervou, lebo Android nepresné budíky odkladá. */
    const val DELAY_MILLIS = 100 * 60_000L

    /** Keď strážcovi nevyšla sieť, skúsi znova o chvíľu – do odhlásenia ešte zostáva čas. */
    const val RETRY_MILLIS = 10 * 60_000L

    private const val MIN_DELAY_MILLIS = 60_000L

    /** Za ako dlho má strážca zasiahnuť, keď poslednú úspešnú požiadavku videla kyberia v [lastOkMillis]. */
    fun delayAfter(lastOkMillis: Long?, nowMillis: Long): Long =
        if (lastOkMillis == null) DELAY_MILLIS else (lastOkMillis + DELAY_MILLIS - nowMillis).coerceIn(MIN_DELAY_MILLIS, DELAY_MILLIS)
}
