# mokap – pravidlá projektu

Neoficiálny natívny Android klient pre kyberia.sk. **Nie je to WebView wrapper ani prehliadač:**
appka sťahuje stránky kyberie, parsuje ich (Jsoup) a zobrazuje dáta vlastným UI (Compose).

## Nepreklenuteľné pravidlá
- **Žiadne secrets v gite.** Keystore, heslá, API kľúče, `keystore.properties`,
  `google-services.json`, Play service account JSON, session cookies, `anticsrf` tokeny – nič z toho.
  Release podpisovanie číta env premenné `MOKAP_*` alebo lokálny `keystore.properties`.
- **Žiadne súkromné údaje v gite**: emaily, nicky, user ID, príspevky, pošta, prihlasovacie údaje.
  Skutočné HTML stránky od používateľa sa používajú len lokálne na analýzu (mimo repa);
  do `app/src/test/resources/fixtures` idú výhradne umelé vzorky s vymyslenými dátami.
- Appka **neukladá heslo**; drží len session cookies šifrované cez Android Keystore.
- Požiadavky a cookies idú výhradne na `https://kyberia.sk` (`UrlPolicy`, `KyberiaCookieJar`).
- Appka sa nevydáva za oficiálnu aplikáciu kyberie ani nepoužíva jej logo bez súhlasu.

## Štruktúra kyberie
- Všetko je node s URL `/id/<ID>/`. Pevné: 19 = bookmarky, 24 = pošta, 8340566 = top K za ~48 h.
- Akcie sa posielajú ako POST `event=<názov>` + skrytý `anticsrf` token z tej istej stránky.
- Prihlásená stránka má vždy tlačidlo `event=logout`; hlavička obsahuje odkaz SELF (vlastné ID).

## Technické
- `applicationId` / `namespace`: `sk.ntnlabs.kyberia` (po vydaní na Play sa nesmie meniť).
- Verzie v `gradle/libs.versions.toml`. AGP 9 má vstavaný Kotlin – neaplikovať
  `org.jetbrains.kotlin.android`; verzia KGP (root `buildscript`) a Compose compiler pluginu musí byť rovnaká.
- `parse/` a čistá logika v `data/` nesmú závisieť od Android API – majú JVM unit testy.
- Notifikácie sú lokálne (WorkManager, min. 15 min), žiadny Firebase.
- CI: `.github/workflows/android.yml` – gitleaks, lint, unit testy, debug aj release build.
