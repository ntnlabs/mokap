# mokap

Neoficiálny Android klient pre [kyberia.sk](https://kyberia.sk). Nie je to prehliadač:
appka sa prihlási, stiahne vybrané stránky, vytiahne z nich dáta a ukáže ich po svojom –
zoradené, prefiltrované a s lokálnymi notifikáciami.

> Projekt nie je oficiálnou aplikáciou kyberia.sk.

[Zásady ochrany súkromia](PRIVACY.md) · podklady pre Google Play: [Data safety](docs/play/data-safety.md), [store listing](docs/play/store-listing.md)

## Stav

| Časť | Stav |
|---|---|
| Prihlásenie cez nick alebo ID (natívne, heslo sa neukladá) | hotové, parser overený na skutočnej stránke |
| Bookmarky (`/id/19/`) – A–Z (slovenské radenie) alebo podľa noviniek, filter „len s novinkami“, hľadanie bez diakritiky | hotové |
| Odpovede na tvoje príspevky – vlastná záložka, počet K a odpovedí, animované avatary s diskovou cache | hotové |
| Last – 10 najnovších príspevkov na celej kyberii (z tej istej stránky, žiadna požiadavka navyše) | hotové |
| Motto z titulku kyberie pod názvom záložky | hotové |
| Zelený pruh nad záložkami do ďalšieho obnovenia, bodka na záložkách s novinkami | hotové |
| NSFW: „NSFW“ v názvoch ako 🔞, prepínač „Skryť NSFW“, predvolene zapnutý (zoznamy aj notifikácie) | hotové |
| Notifikácie: odpovede, pošta, novinky v bookmarkoch, priateľ online, K na tvoje príspevky | hotové, každá s vlastným kanálom a prepínačom |
| Friends online – kde sú, nečinnosť, počet na záložke | hotové |
| Prepínač vo Friends: Priatelia / Všetci online (`/id/27/`) / Diskusie – kde je najviac ľudí (`/id/4830026/`), sťahuje sa len na požiadanie | hotové |
| Nastavenia: téma (systém/svetlá/tmavá/tlmená), avatary (animácia, len cez Wi-Fi), interval kontrol 15–60 min, tichý režim, notifikácie, odhlásenie | hotové |
| K_wallet a vlastné ID (SELF) | parsované z hlavičky |
| Notifikácia o novej pošte (ľahký endpoint `check_new_mail.php`, otvorí poštu v appke, zmizne po prečítaní aj na webe) | hotové |
| K – tvoje príspevky s K za poslednú hodinu (z `/id/19/`) a top K (`/id/8340566/`, len na požiadanie) | hotové |
| Pošta v appke (`/id/24/`) – čítanie, odpoveď, nová správa (nick alebo ID), počet nových na záložke | hotové |
| Čítanie nodov priamo v appke – obsah, autor, K, odpovede vo vlákne (prvá stránka), odkazy na nody v appke | hotové |
| Odpovedanie do nodu aj na konkrétnu odpoveď (ako na webe – HTML, „Bez HTML“) | hotové |
| Dať K (len pridať, s potvrdením, aj sebe) v node aj v Top K a úprava vlastných príspevkov | hotové |
| K „Za N h“ – tvoje príspevky s K za nastaviteľný čas (predvolene 24 h) v záložke K, len na požiadanie | hotové |
| Téma nodu oddelená od diskusie | hotové |
| Obrázky na celú obrazovku so zväčšením a uložením do galérie (Obrázky/mokap); text sa dá označiť a kopírovať | hotové |

## Spätná väzba

Chyby a nápady cez [GitHub issues](https://github.com/ntnlabs/mokap/issues/new/choose) – šablóny **Chyba** a **Nápad**
(treba účet na GitHube). V appke: Nastavenia → „Nahlásiť chybu alebo nápad“. Issues sú verejné: žiadne heslá, cudzie
nicky, súkromná pošta ani neverejný obsah z kyberie – ani na screenshotoch.

## Ako to funguje

- **Sieť:** OkHttp, požiadavky idú výhradne na `https://kyberia.sk`. Cookie jar prijíma
  a posiela cookies iba pre kyberiu.
- **Parsovanie:** Jsoup. Parsery sú čistý Kotlin (`parse/`) s JVM testami na umelých vzorkách.
- **Session:** po prihlásení appka drží len session cookies, šifrované kľúčom
  z Android Keystore. Dáta appky sa nezálohujú ani neprenášajú na iný telefón.
- **Notifikácie:** WorkManager podľa nastavenia (15–60 min; 15 je minimum Androidu) stiahne
  `/id/19/`, porovná ho s naposledy videným stavom a pri novinkách vytvorí lokálnu notifikáciu.
  Počet novej pošty zistí jednou malou požiadavkou na `/ajax/check_new_mail.php` (len ak je zapnutá).
  Ťuknutie na odpoveď/fórum/K otvorí daný node v appke. Žiadny Firebase, žiadny server.
  Notifikácie ukazujú stav: zmiznú, keď je novinka videná (prečítané fórum, pošta, otvorená záložka Odpovede či K,
  priateľ odišiel). K sa berú z bloku „Tvoje K“ (posledná hodina); po dlhšej prestávke (tichý režim, Doze) sa
  /id/19/ načíta POST-om s blokom za celý výpadok (najviac 24 h, nie požiadavka navyše), nech sa K z noci nestratia.
- **Strážca session:** kyberia odhlási, ak 2 hodiny nevidí načítanie stránky, a Android kontroly
  v spánku odkladá aj na hodiny. Appka si preto natiahne jeden budík na „posledná požiadavka + 100 min“;
  pri bežných kontrolách sa nikdy nespustí, takže server nedostane nič navyše. V tichom režime
  nechodia notifikácie a jediná požiadavka je tento strážca.
- **Odhlásenie** sa vyhodnotí len vtedy, keď kyberia ukáže prihlasovací formulár; chybové stránky
  (údržba, ochrana proti botom) sú dočasná chyba. Nastavenia → Pokročilé (aj „Pre developerov“ na prihlasovacej obrazovke) ukazuje verziu,
  posledné kontroly a či kyberia pri odhlásení presmerovala (nečinnosť) alebo session len zmizla.
- **UI:** Jetpack Compose, Material 3.

## Build

Požiadavky: JDK 17+, Android SDK (compileSdk 37).

```sh
./gradlew assembleDebug          # debug APK -> app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # unit testy
./gradlew lintDebug              # lint
```

CI pri každom pushi spúšťa len gitleaks, lint a unit testy. APK sa vyrába iba na požiadanie:
GitHub → Actions → **Android CI** → **Run workflow**, alebo push tagu `apk-*`
(napr. `apk-2026-10-17`); výsledok je artefakt `mokap-debug-apk`.

Verzia má tvar `major.minor.build` (napr. `0.7.0112`) a kódové meno. Major, minor a meno sú
vo [`version.properties`](version.properties) a menia sa ručne; build je číslo behu CI (job `apk`),
lokálne `0000`.

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

### AAB pre Google Play z CI

Job `apk` (len na požiadanie) vyrobí aj `bundleRelease` a nahrá artefakt `mokap-release-aab` –
iba ak sú v GitHube (Settings → Secrets and variables → Actions) tieto repository secrets:

| Secret | Obsah |
|---|---|
| `MOKAP_KEYSTORE_BASE64` | upload keystore v base64 |
| `MOKAP_KEYSTORE_PASSWORD` | heslo keystoru |
| `MOKAP_KEY_ALIAS` | alias kľúča (napr. `upload`) |
| `MOKAP_KEY_PASSWORD` | heslo kľúča (pri PKCS12 rovnaké ako heslo keystoru) |

Upload kľúč si vyrob sám, mimo repa (keytool je v každom JDK aj v Android Studiu):

```sh
keytool -genkeypair -v -keystore mokap-upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 mokap-upload.jks        # Linux; macOS: base64 -i mokap-upload.jks
```

Keystore aj heslá si zálohuj (napr. v správcovi hesiel). Keystore sa v CI rozbalí len do dočasného
adresára runnera a po builde sa zmaže; CI overí podpis AAB a vypíše SHA-256 certifikátu na porovnanie
s „Upload key certificate“ v Play Console.

## Pravidlá

- žiadne secrets ani súkromné údaje v gite (CI to kontroluje cez gitleaks)
- testovacie HTML vzorky sú **umelé** – skutočné stránky s menami a príspevkami ľudí do gitu nejdú
- `applicationId` je `sk.ntnlabs.kyberia` a po prvom vydaní sa nemení

## Licencia

GPL-3.0 – pozri [LICENSE](LICENSE).
