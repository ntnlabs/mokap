# mokap

Mobilná aplikácia (Android) pre [kyberia.sk](https://kyberia.sk) – neoficiálny klient,
ktorý zobrazuje web a pridáva veci, ktoré pasívny web nemá (notifikácie a ďalšie).

> Projekt nie je oficiálnou aplikáciou kyberia.sk.

## Čo už vie

- web kyberie vo WebView, prihlásenie cez web (appka heslá neukladá)
- odkazy mimo kyberie sa otvárajú v prehliadači, `http://` odkazy na kyberiu sa prepnú na `https://`
- pull-to-refresh, späť cez históriu, chybová obrazovka s „Skúsiť znova“
- nahrávanie súborov (file input), sťahovanie súborov, video na celú obrazovku
- otváranie kyberia.sk odkazov priamo v appke (treba zapnúť v systémových nastaveniach)
- žiadna záloha dát appky (session cookies neodídu do cloudu ani na iný telefón)

## Build

Požiadavky: JDK 17+, Android SDK (compileSdk 37).

```sh
./gradlew assembleDebug          # debug APK -> app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # unit testy
./gradlew lintDebug              # lint
```

Debug APK zbuildovaný z každého pushu je aj v GitHub Actions → artefakt `mokap-debug-apk`.

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
- `applicationId` je `sk.ntnlabs.kyberia` a po prvom vydaní sa nemení

## Licencia

GPL-3.0 – pozri [LICENSE](LICENSE).
