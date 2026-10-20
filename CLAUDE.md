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
- Appka **neukladá heslo**; drží len session cookies šifrované cez Android Keystore. Prihlasovacie polia
  sú označené pre autofill (`ContentType.Username/Password`) – uložiť ich môže len správca hesiel
  v telefóne so súhlasom používateľa, appka nie.
- Požiadavky a cookies idú výhradne na `https://kyberia.sk` (`UrlPolicy`, `KyberiaCookieJar`).
- Appka sa nevydáva za oficiálnu aplikáciu kyberie ani nepoužíva jej logo bez súhlasu.

## Štruktúra kyberie
- Všetko je node s URL `/id/<ID>/`. Pevné: 19 = bookmarky, 24 = pošta, 8340566 = top K za ~48 h.
- Akcie sa posielajú ako POST `event=<názov>` + skrytý `anticsrf` token z tej istej stránky.
- Prihlásená stránka má vždy tlačidlo `event=logout`; hlavička obsahuje odkaz SELF (vlastné ID).
- Prihlásenie: odhlásený `/id/19/` vráti „nemáš práva“ s čistým formulárom (multipart POST,
  `login`, `password`, rádio `login_type` = `name`|`id`, `event=login`). Hlavná stránka sa na login
  nepoužíva. Appka ponúka Nick/ID (niektoré nicky sa napísať nedajú); pamätá si len voľbu, nie nick/ID.
- `/id/19/` obsahuje aj blok „Last 10 user submissions children“ (`div.user_sub_child`): avatar
  autora odpovede, odkaz na **tvoj** príspevok (nie na odpoveď) a úryvok textu. Počítadlá pred `<br>`:
  `[<span>1K</span>]` = počet K, `[<span>2</span>]` (bez K) = počet odpovedí pod odpoveďou. Odpoveď novšia ako
  posledná návšteva /id/19/ má pred odkazom ešte `<a class=most_important>NEW</a>` (/id/3579407/) – nie je to príspevok.
- Blok „Last“ (`div.last`) = 10 najnovších príspevkov na celej kyberii: avatar s `title="nick | čas - dátum"`,
  odkaz na príspevok, „in“, odkaz na rodiča, čas, úryvok. Celý last je na `/id/35/`.
- Titulok stránky je `<názov> :: <motto>`; motto sa strieda a appka ho ukazuje pod názvom záložky.
- `/id/19/` má aj blok „Friends“ (`div.active_friend`): avatar+nick, `[odkaz]` na node, kde
  priateľ práve je, a čas (napr. `84m39s`). Avatary sú animované GIFy (Coil + disková cache).
- People (`/id/27/`): zoznamy `<b>friends</b>` a `<b>users</b>` (`div.user-list`), každý človek
  `div[data-idle=sekundy]` s nickom v `td.important`, avatarom a `[odkaz]` na node, kde je.
- Populated (`/id/4830026/`): JS pole `var tags = [[ID,'názov'],…]`, jedna položka za človeka; `(68/110)` =
  ľudia vo verejných nodoch / online. Appka to spočíta ako „Tu sa práve diskutuje“.
- People a populated sa sťahujú **len na požiadanie** (prepínač vo Friends), nikdy v pozadí.
- Pošta: `GET /ajax/check_new_mail.php` (web ho volá raz za minútu) vráti
  `<počet nových>;<nick posledného odosielateľa>;…` alebo `offline`. Nick ide len do notifikácie
  („Nová pošta od …“), nikam sa neukladá; posledné pole nemá overený význam. `offline` session
  nezahadzuje. Do testov/gitu len vymyslené nicky.
- Avatary: prepínač animácie (vypnutá = prvý snímok cez `BitmapFactoryDecoder`) a „len cez Wi-Fi“
  (na meranej sieti len z diskovej cache, `networkCachePolicy(DISABLED)`); `LocalAvatarOptions`.

## Technické
- `applicationId` / `namespace`: `sk.ntnlabs.kyberia` (po vydaní na Play sa nesmie meniť).
- Verzie v `gradle/libs.versions.toml`. AGP 9 má vstavaný Kotlin – neaplikovať
  `org.jetbrains.kotlin.android`; verzia KGP (root `buildscript`) a Compose compiler pluginu musí byť rovnaká.
- `parse/` a čistá logika v `data/` nesmú závisieť od Android API – majú JVM unit testy.
- Notifikácie sú lokálne (WorkManager, interval 15–60 min z nastavení – 15 je minimum Androidu),
  žiadny Firebase. Každý typ má vlastný kanál; jednotlivé odpovede/fóra otvárajú node na webe.
- Kontrolu robí `NewsChecker` (mutex), volá ho `CheckWorker` a strážca session (`SessionGuardReceiver`).
- **Kyberia odhlási pri načítaní stránky, ak od predošlého ubehli > 2 h** (`LOGOUT_IDLE_SEC = 7200`,
  `$_SESSION['last_request']` v zdrojákoch Kyberia-bloodline; AJAX pošty sa nepočíta). Android kontroly
  v Doze/standby odkladá aj na hodiny → **strážca session**: jeden budík `setAndAllowWhileIdle` na
  „posledná úspešná požiadavka + 100 min“, každé načítanie ho posunie. Pri bežných kontrolách sa nespustí
  (0 požiadaviek navyše). Tichý režim = žiadne notifikácie; jediná požiadavka je strážca.
- `LatestPage` = posledná /id/19/ v pamäti, zdieľaná kontrolou v pozadí aj appkou (čo stiahne jedna,
  ukáže druhá). Zelený pruh nad záložkami rastie do ďalšieho obnovenia (interval z nastavení); v otvorenej
  appke sa po naplnení obnoví sama. Kontrola v pozadí preskočí /id/19/, ak ju appka stiahla nedávno.
- Bodka na záložke (Bookmarky, Odpovede, Last) = novinka od poslednej návštevy záložky (`TabChanges`).
- **Výpadok siete alebo kyberie je bežný stav** – appka naň reaguje pasívne (strážca to skúsi znova, pri odhlásení
  notifikácia). Žiadne obchádzanie DNS (DoH, Cloudflare, cache IP) a žiadne automatické prihlasovanie
  (ani heslom zo správcu hesiel) – rozhodnutie používateľa.
- **Žiadny pravidelný keepalive** a žiadne zbytočné požiadavky – kyberia má obmedzený hosting; pri každej
  novej funkcii zvažuj záťaž servera (1000 používateľov × interval). Strážca session je výnimka len
  pre prípad, keď by inak prišlo odhlásenie.
- Session sa zahodí **len** pri `PageKind.LOGGED_OUT` (prihlasovací formulár na stránke);
  `UNKNOWN` stránka je dočasná chyba. Výsledky kontrol ide do `DiagnosticsStore` (bez osobných údajov).
  `LogoutDetail`: odhlásenie pri nečinnosti kyberia robí presmerovaním na `/` (`logout.inc`), zmiznutá session
  ukáže login bez presmerovania. Diagnostika sa nemaže a je dostupná aj z prihlasovacej obrazovky.
- Cookie jar zahadzuje `jabber_login` a `jabber_password` (web ich nastavuje pri prihlásení, druhá je MD5 z hesla).
  Po konci session (`endSession`) neprijíma cookies až do ďalšieho prihlásenia (`clear`) – odpoveď súbežnej
  požiadavky by inak session po reštarte „oživila“.
- Chyba kontroly na pozadí zapíše do diagnostiky aj stav siete (`NetworkProbe`: bez siete / zablokovaná pre appku,
  Doze, šetriče, standby bucket) – „Unable to resolve host“ inak nerozlíši offline telefón od zákazu Androidu.
- Čas pri priateľoch je **nečinnosť** (idle); zoznam je zoradený od najaktívnejšieho.
- Appka beží vždy so slovenským locale (`AppLocale` v `attachBaseContext` aplikácie aj aktivity) – inak by
  Android vyberal tvar plurálu podľa jazyka telefónu („2 ľudí“ v anglickom telefóne).
- Odkazy na web idú cez `Web.open` s `Browser.EXTRA_APPLICATION_ID` – Chromium prehliadače (Chrome, Brave)
  ich otvárajú v tej istej karte namiesto novej.
- UI je pre mobil: každá oblasť má vlastnú záložku (Bookmarky, Odpovede, Last, Pošta, K, Friends),
  v spodnej lište len ikony (popis pre čítačku). Pošta a K zatiaľ „Pracujeme na tom.“ + tlačidlo na web.
  Bookmarky predvolene A–Z (slovenský Collator); radenie/filter si appka pamätá v nastaveniach.
- NSFW: **len názov samotného nodu** obsahuje „NSFW“ (samostatné slovo, `Nsfw`) – nikdy obsah, úryvky
  ani rodič (NSFW sa dedí, všetko aj tak nechytíme; v textoch priveľa omylov). V názvoch sa ukazuje 🔞;
  prepínač „Skryť NSFW“ (predvolene zapnutý) node vyradí zo zoznamov, notifikácií aj bodiek na záložkách.
- Téma „Tlmená“ = tmavá s nízkym jasom textu, farieb aj avatarov (`LocalDimImages`).
- CI: `.github/workflows/android.yml` – na push/PR len gitleaks, lint a unit testy.
  Gradle cache cez `setup-java` (`cache: gradle`); `gradle/actions` nepoužívať – od v6 má proprietárne
  cachovanie. `gradle-wrapper.jar` sa v CI overuje proti oficiálnemu sha256 od Gradle.
- Hotové kolo zmien sa hneď zlučuje do `main` cez PR (merge commit, po zelenom CI); dlhé vetvy nedržíme.
- Dependabot: patch a minor verzie zlučuj sám po zelenom CI; major verzie najprv vysvetli používateľovi
  (zmeny, licencia, riziko) a čakaj na súhlas.
- Verzia `major.minor.build` + kódové meno: major, minor a meno vo `version.properties` (teraz 0.7 „Bugz Bunny“)
  **mení len používateľ**. Build = `github.run_number` (job `apk` ho dáva ako `MOKAP_BUILD_NUMBER`), štvormiestne;
  inde 0, aby sa BuildConfig nemenil. `versionCode` = major·10⁷ + minor·10⁵ + build. V nastaveniach sú verzia
  a diagnostika v sekcii „Pokročilé“, predvolene zbalenej.
- **APK sa builduje iba so súhlasom používateľa.** Pred každým buildom APK sa najprv opýtaj;
  build spúšťa job `apk` cez `workflow_dispatch` (Actions → Android CI → Run workflow) alebo
  push tagu `apk-*`; vyrobí debug aj release APK, vypíše audit oprávnení a nahrá `mokap-debug-apk`.
