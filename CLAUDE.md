# mokap – pravidlá projektu

Neoficiálny Android klient (wrapper) pre kyberia.sk. Kotlin + WebView, bez Compose.

## Nepreklenuteľné pravidlá
- **Žiadne secrets v gite.** Keystore, heslá, API kľúče, `keystore.properties`,
  `google-services.json`, Play service account JSON – nič z toho necommitovať.
  Release podpisovanie číta env premenné `MOKAP_*` alebo lokálny `keystore.properties`.
- **Žiadne súkromné údaje v gite** (emaily, mená, prihlasovacie údaje, cookies,
  testovacie účty, screenshoty s osobným obsahom).
- Appka si **neukladá heslá**; prihlásenie rieši web kyberie, session drží WebView cookie.
- Appka sa nevydáva za oficiálnu aplikáciu kyberie ani nepoužíva jej logo bez súhlasu.

## Technické
- `applicationId` / `namespace`: `sk.ntnlabs.kyberia` (po vydaní na Play sa nesmie meniť).
- Verzie v `gradle/libs.versions.toml`; AGP 9 má vstavaný Kotlin – neaplikovať
  `org.jetbrains.kotlin.android`.
- Logika mimo Android API (napr. `UrlPolicy`) má JVM unit testy v `app/src/test`.
- CI: `.github/workflows/android.yml` – gitleaks, lint, unit testy, debug APK.
