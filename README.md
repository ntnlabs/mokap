# mokap

Neoficiálny Android klient pre [kyberia.sk](https://kyberia.sk). Nie je to prehliadač:
appka sa prihlási, stiahne vybrané stránky, vytiahne z nich dáta a ukáže ich po svojom –
zoradené, prefiltrované a s lokálnymi notifikáciami.

> Projekt nie je oficiálnou aplikáciou kyberia.sk.

## Stav

| Časť | Stav |
|---|---|
| Prihlásenie (natívne, heslo sa neukladá) | hotové, parser overený na skutočnej stránke |
| Bookmarky (`/id/19/`) – zoradené podľa noviniek | hotové |
| Odpovede na tvoje príspevky | hotové (sekcia v bookmarkoch, animované avatary s diskovou cache) |
| Notifikácie: nové odpovede, novinky v bookmarkoch | hotové (kontrola každých ~15 min) |
| K_wallet a vlastné ID (SELF) | parsované z hlavičky |
| Pošta (`/id/24/`), K (`/id/8340566/`), Friends | čaká na vzorky HTML |
| Čítanie nodov priamo v appke | neskôr; zatiaľ sa node otvorí na webe |

## Ako to funguje

- **Sieť:** OkHttp, požiadavky idú výhradne na `https://kyberia.sk`. Cookie jar prijíma
  a posiela cookies iba pre kyberiu.
- **Parsovanie:** Jsoup. Parsery sú čistý Kotlin (`parse/`) s JVM testami na umelých vzorkách.
- **Session:** po prihlásení appka drží len session cookies, šifrované kľúčom
  z Android Keystore. Dáta appky sa nezálohujú ani neprenášajú na iný telefón.
- **Notifikácie:** WorkManager raz za ~15 minút stiahne bookmarky, porovná ich s naposledy
  videným stavom a pri novinkách vytvorí lokálnu notifikáciu. Žiadny Firebase, žiadny server.
- **UI:** Jetpack Compose, Material 3.

## Build

Požiadavky: JDK 17+, Android SDK (compileSdk 37).

```sh
./gradlew assembleDebug          # debug APK -> app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # unit testy
./gradlew lintDebug              # lint
```

Debug APK z každého pushu je v GitHub Actions ako artefakt `mokap-debug-apk`.

## Release podpisovanie (secrets mimo gitu)

Upload keystore **nikdy nepatrí do repozitára**. Build si hodnoty berie v tomto poradí:

1. env premenné: `MOKAP_KEYSTORE_FILE`, `MOKAP_KEYSTORE_PASSWORD`, `MOKAP_KEY_ALIAS`, `MOKAP_KEY_PASSWORD`
2. lokálny súbor `keystore.properties` v koreni repa (je v `.gitignore`):

```properties
storeFile=/cesta/mimo/repa/upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

Bez nich vznikne nepodpísaný release build. Na Google Play používame Play App Signing –
lokálny kľúč je len *upload key*.

## Pravidlá

- žiadne secrets ani súkromné údaje v gite (CI to kontroluje cez gitleaks)
- testovacie HTML vzorky sú **umelé** – skutočné stránky s menami a príspevkami ľudí do gitu nejdú
- `applicationId` je `sk.ntnlabs.kyberia` a po prvom vydaní sa nemení

## Licencia

GPL-3.0 – pozri [LICENSE](LICENSE).
