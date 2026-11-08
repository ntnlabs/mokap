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
  Jediná výnimka – rozhodnutie používateľa: nastavenie „Obrázky a videá z iných stránok“ (predvolene **vypnuté**)
  načíta cudzie obrázky a náhľady YouTube v príspevkoch cez `ExternalHttp` – samostatný klient bez cookies, len https.
  Certificate pinning zámerne nie (kyberia mení certifikáty mimo nás – pin by appku zabil všetkým);
  cleartext aj používateľské CA sú pri targetSdk 36 vypnuté predvolene, network security config netreba.
  Zlyhanie Keystore (`SessionStore`) = stratená session a nové prihlásenie, nikdy pád ani nešifrované uloženie.
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
- **K** (`KParser`, `KScreen`): „Tvoje K“ = blok `div.module_your_k_nodes` na /id/19/ (`get_temp_k`):
  `<span class=most_important>N k</span> <a href=/id/ID/>názov</a> Mk <br>` (za hodinu / celkovo). Interval sa posiela
  len POST-om a kyberia ho neukladá – GET /id/19/ dá vždy 1 h; appka zatiaľ len tú (iný interval = možno nastavenie neskôr).
  „Top K“ = /id/8340566/: `div.item` s `div.icon-k` (avatar, „24 k“) a `div.title` (`<a>`príspevok, „in“ `<a>`rodič,
  „by“ `<a>`autor, „created: …“, „- 3ch“) + `div.content`. Sťahuje sa **len pri prepnutí na Top K** (staršie ako 5 min),
  nikdy v pozadí; po ňom `Presence`.
- Titulok stránky je `<názov> :: <motto>`; motto sa strieda a appka ho ukazuje pod názvom záložky.
- `/id/19/` má aj blok „Friends“ (`div.active_friend`): avatar+nick, `[odkaz]` na node, kde
  priateľ práve je, a čas (napr. `84m39s`). Avatary sú animované GIFy (Coil + disková cache).
- People (`/id/27/`): zoznamy `<b>friends</b>` a `<b>users</b>` (`div.user-list`), každý človek
  `div[data-idle=sekundy]` s nickom v `td.important`, avatarom a `[odkaz]` na node, kde je.
- Populated (`/id/4830026/`): JS pole `var tags = [[ID,'názov'],…]`, jedna položka za človeka; `(68/110)` =
  ľudia vo verejných nodoch / online. Appka to spočíta ako „Tu sa práve diskutuje“.
- People a populated sa sťahujú **len na požiadanie** (prepínač vo Friends), nikdy v pozadí.
- „Kde je“ (`user_action`) nastaví kyberia pri **každom** zobrazení nodu (`display.inc`) na jeho názov.
  `Presence` na konci každého obnovenia (appka, pozadie, strážca, people/populated) načíta prázdny node
  „používa MOKAP“ (`KyberiaUrls.PRESENCE_ID`), aby priatelia videli mokap namiesto „bookmarks“. Výnimka z pravidla
  o požiadavkách – rozhodnutie používateľa: +1 malá požiadavka na obnovenie, prepínač v nastaveniach (predvolene
  zapnutý). Chyba pri ňom obnovenie nepokazí. Node so šablónou /id/19/ nefunguje (živá /id/19/ má prispôsobiteľnú šablónu).
- **Node v appke** (`NodeParser`, `NodeScreen`): názov z `<title>`, vlastník/rodič/čas z `tr#sidebar_*` („owner:“ – riadok
  „viewed by:“ má tiež id `sidebar_owner`), K a „total children::“ v `span.node_info2`, obsah v `div#topic`, odpovede ako
  vnorené `li.lvl.levelN` (`node_header`: `node_login`, `node_header_created`, `node_header_k`, `node_header_new`;
  `node_body` bez `small.mood`). Obsah sa **nevykresľuje ako HTML**: `ContentParser` → bloky (text s tučným/šikmým/
  odkazmi, obrázok, embed); skripty a `javascript:` vypadnú, cudzie obrázky a iframe sú len odkaz (nič mimo kyberie
  sa nesťahuje, ak to používateľ nezapne), odkazy na `/id/N` otvárajú node v appke; video ide na stránku YouTube
  (`Embeds.openUrl`, nie embed). Vlákno do 4 úrovní (`ThreadView.MAX_DEPTH`), hlbšie ako „↳ N ďalších odpovedí“ = otvorí sa
  node tej odpovede; názvy odpovedí sa ukazujú aj automatické („Re[2]: …“, dátum) – pomáhajú v orientácii (želanie
  používateľa). Jedna požiadavka na otvorenie, po nej žiadna `Presence`
  (ručné kliknutie = skutočné „kde je“); kyberia stránkuje odpovede (32 nodov vlákna) – staršie zatiaľ na webe.
  `NodesViewModel` drží len nody zo zásobníka v `HomeScreen`.
- **Odpovedanie** (`ReplyFormParser`, `NodeRepository.reply`, `Replies`): formulár `formular` s `textarea[name=node_content]`
  kyberia ukáže len s právom zápisu – inak appka odpovedať neponúkne. POST (multipart ako web) posiela **len** `anticsrf`,
  `node_name` (prázdny – „Re: …“ doplní kyberia), `node_content`, `template_id` (4), `node_parent`, voliteľne `no_html=yes`
  a `node_chosen[]` (odpoveď na odpoveď) a `event=add` – nikdy celý formulár: obsahuje aj skryté `event=K` a PHP berie
  posledné `event`. Text ide ako na webe (HTML cez kyberiin `htmlparse`, nové riadky cez `nl2br`). Chyba („Chyba HTML
  syntaxe!“, prázdny obsah) je v hlavičke `center > span.most_important`. Úspech = na vrátenej stránke je nová odpoveď
  od teba; inak „nepotvrdené“. POST je `oneShot` (OkHttp ho po chybe spojenia neopakuje) a appka ho nikdy neposiela
  znova sama – pri chybe siete nevieme, či prešiel. Text v editore sa zahodí len po potvrdenom odoslaní.
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
  žiadny Firebase. Každý typ má vlastný kanál; jednotlivé odpovede/fóra otvárajú node v appke (pošta web).
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
- **Cieľ je telefón v predvolenom stave** (šetrič dát, optimalizácia batérie, Doze) – appka v ňom nesmie skolabovať
  (falošné odhlásenia, zlé notifikácie, zbytočné budenie). Výnimky od používateľa (dáta, batéria) ju môžu len zlepšiť;
  appka o nich informuje a radí, ale nepočíta s nimi. Strážca po chybe siete skúša znova len do termínu odhlásenia
  (`SessionGuard.retryAfter`), potom už nič – odhlásenie zistí kontrola so sieťou alebo appka pri otvorení.
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
- **Šetrič dát** bez výnimky pre mokap zakáže appke na pozadí sieť na mobilných dátach – strážca session vtedy nepomôže.
  Appka to len oznámi (riadok v nastaveniach, text a tlačidlo v notifikácii o odhlásení) a otvorí systémové
  „neobmedzené dáta“ (`NetworkProbe.unrestrictedDataIntent`); sama nič neobchádza.
- Notifikácie ukazujú stav, nie prírastok: fórum celkový počet nových (ako „N NEW Child“), prečítané fórum svoju
  notifikáciu zruší, pošta pri čiastočnom prečítaní potichu opraví číslo; po prihlásení zmizne stará „Odhlásilo ťa“.
- Chyba kontroly na pozadí zapíše do diagnostiky aj stav siete (`NetworkProbe`: bez siete / zablokovaná pre appku,
  Doze, šetriče, standby bucket) – „Unable to resolve host“ inak nerozlíši offline telefón od zákazu Androidu.
- Čas pri priateľoch je **nečinnosť** (idle); zoznam je zoradený od najaktívnejšieho.
- Appka beží vždy so slovenským locale (`AppLocale` v `attachBaseContext` aplikácie aj aktivity) – inak by
  Android vyberal tvar plurálu podľa jazyka telefónu („2 ľudí“ v anglickom telefóne).
- Odkazy na web idú cez `Web.open` s `Browser.EXTRA_APPLICATION_ID` – Chromium prehliadače (Chrome, Brave)
  ich otvárajú v tej istej karte namiesto novej.
- UI je pre mobil: každá oblasť má vlastnú záložku (Bookmarky, Odpovede, Last, Pošta, K, Friends),
  v spodnej lište len ikony (popis pre čítačku). Pošta zatiaľ „Pracujeme na tom.“ + tlačidlo na web.
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
  S repository secrets `MOKAP_KEYSTORE_BASE64` + `MOKAP_KEYSTORE_PASSWORD`/`MOKAP_KEY_ALIAS`/`MOKAP_KEY_PASSWORD`
  podpíše release upload kľúčom a nahrá aj `mokap-release-aab` (Google Play, Play App Signing). Upload kľúč
  vyrába a drží používateľ – nikdy nejde cez Clauda, do gitu ani do logov. Release AAB nesie aj R8 mapovanie.
  **Native symboly nie sú a byť nemôžu:** jediný native kód je `libandroidx.graphics.path.so` (Compose) a AndroidX ho
  dodáva už orezaný – AGP z neho nič nevytiahne („already been stripped“), varovanie Play o debug symboloch je neškodné.
  `ndk.debugSymbolLevel = SYMBOL_TABLE` ostáva pre prípad knižnice so symbolmi; NDK dáva job `apk` z runnera
  (`MOKAP_NDK_PATH`/`MOKAP_NDK_VERSION` → `ndkPath`/`ndkVersion` – AGP chce, aby sedeli). Krok „Native symboly v AAB“ len vypíše stav.
