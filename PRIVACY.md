# Zásady ochrany súkromia – mokap

*Platné od 13. 11. 2026* · [English version below](#privacy-policy--mokap)

**mokap** je neoficiálny Android klient pre [kyberia.sk](https://kyberia.sk), ktorý vyvíja **ntnlabs**
(zdrojový kód: <https://github.com/ntnlabs/mokap>). Nie je to oficiálna aplikácia kyberia.sk.

## Stručne

- **Vývojár nezbiera žiadne údaje.** Appka nemá vlastný server, analytiku, reklamy ani sledovanie.
- Appka komunikuje **len so serverom kyberia.sk** – rovnako ako webový prehliadač, keď kyberiu používaš.
- **Heslo appka neukladá.** V telefóne drží len prihlásenie (session cookies) zašifrované kľúčom z Android Keystore.

## Čo appka posiela a kam

- **kyberia.sk** (len cez šifrované HTTPS): prihlásenie (nick alebo ID a heslo), načítanie stránok
  a akcie, ktoré v appke urobíš – odpovede, úpravy vlastných príspevkov, K a **správy v pošte** (adresát a text).
  Appka sa predstavuje ako
  `mokap/<verzia> (Android; +https://github.com/ntnlabs/mokap)`. Tieto údaje spracúva kyberia.sk podľa svojich pravidiel.
- **„Používa MOKAP“:** po obnovení appka na kyberii otvorí node „používa MOKAP“, takže ostatní používatelia
  kyberie pri tebe namiesto „bookmarks“ vidia, že si v mokape. Dá sa vypnúť v nastaveniach (Účet).
- **Obrázky a videá z iných stránok** (predvolene **vypnuté**): ak ich zapneš, appka stiahne obrázky
  a náhľady videí v príspevkoch priamo z cudzích serverov – bez cookies, len cez HTTPS. Tieto servery
  uvidia tvoju IP adresu, ako pri každom stiahnutí obrázka.
- **Pošta:** appka ju stiahne len pri otvorení záložky Pošta (a keď ju potiahneš nadol) – kyberia ju tým, rovnako ako
  na webe, označí ako prečítanú. Správy appka drží len v pamäti počas behu, do telefónu ich neukladá.
- Odkazy mimo kyberie sa otvárajú v tvojom prehliadači.

## Čo appka ukladá v telefóne

- prihlásenie do kyberie (session cookies), šifrované kľúčom z Android Keystore;
- nastavenia appky a voľbu, či sa prihlasuješ nickom alebo ID (nie samotný nick či ID);
- stav pre notifikácie: čísla nodov z bookmarkov a odpovedí a ich počty, čísla (ID) priateľov online,
  počet neprečítanej pošty, počty K pri tvojich príspevkoch a čas, kedy sa K naposledy kontrolovali;
- diagnostiku kontrol (čas, výsledok, stav siete) – bez osobných údajov, slúži na zistenie, prečo appku odhlásilo;
- vyrovnávaciu pamäť obrázkov (avatary; cudzie obrázky, ak sú zapnuté);
- obrázky, ktoré **sám uložíš** tlačidlom „Uložiť“ – do galérie (Obrázky/mokap). Appka ich vezme z vyrovnávacej
  pamäte, inak ich stiahne znova z toho istého miesta; nepotrebuje na to žiadne oprávnenie a do galérie nič iné
  nezapisuje. Tie ostanú v galérii aj po odinštalovaní appky.

Nič z toho (okrem obrázkov, ktoré si uložil do galérie) sa **nezálohuje do cloudu ani neprenáša na iné zariadenie** a nič z toho appka neposiela vývojárovi.

## Notifikácie

Notifikácie sú lokálne – appka sama kontroluje kyberiu v intervale z nastavení. Nepoužíva Firebase
ani iný push server. Notifikácia o pošte môže obsahovať nick odosielateľa; nikam sa neukladá. Pri obnovení
otvorenej appky sa skontroluje aj počet novej pošty (malá požiadavka), aby notifikácia zmizla, keď si poštu
prečítal inde. Notifikácia o K berie údaje z tej istej stránky ako ostatné; po dlhšej prestávke (napr. tichý režim
v noci) si appka raz vypýta od kyberie prehľad K za celý ten čas, nech sa žiadne K nestratí.

## Oprávnenia

Internet a stav siete (komunikácia s kyberiou), notifikácie, spustenie po reštarte telefónu a krátke
prebudenie (pravidelné kontroly cez Android WorkManager).

## Tretie strany

Appka neobsahuje SDK tretích strán na analytiku, reklamy ani hlásenie pádov a nikomu nepredáva ani
neposkytuje údaje. Google Play a Android môžu zbierať technické údaje (napr. o pádoch) podľa vlastných
pravidiel a tvojich nastavení telefónu – appka sama nie.

## Vymazanie údajov

- **Odhlásenie** v appke zmaže prihlásenie z telefónu.
- **Odinštalovanie** alebo „Vymazať údaje“ v nastaveniach Androidu zmaže všetko, čo appka uložila.
- Tvoj účet a príspevky na kyberii spravuje **kyberia.sk** – appka nevie účet vytvoriť ani zrušiť.

## Deti

Appka nie je určená deťom. Kyberia obsahuje aj obsah pre dospelých; nody s „NSFW“ v názve appka
predvolene skrýva.

## Zmeny

Zmeny týchto zásad sa zverejňujú v repozitári (história je v gite). Dátum platnosti je hore.

## Kontakt

Otázky a pripomienky: <https://github.com/ntnlabs/mokap/issues> (aj z appky: Nastavenia → „Nahlásiť chybu alebo nápad“
otvorí túto stránku v prehliadači; appka sama nič neposiela). Issues sú verejné – nepíš do nich heslo, cudzie nicky ani poštu.

---

# Privacy policy – mokap

*Effective 13 November 2026*

**mokap** is an unofficial Android client for [kyberia.sk](https://kyberia.sk), developed by **ntnlabs**
(source code: <https://github.com/ntnlabs/mokap>). It is not an official kyberia.sk app.

## In short

- **The developer collects no data.** The app has no server of its own, no analytics, no ads and no tracking.
- The app talks **only to kyberia.sk** – the same way a web browser does when you use kyberia.
- **The app never stores your password.** It only keeps your login (session cookies) on the phone,
  encrypted with a key from the Android Keystore.

## What the app sends, and where

- **kyberia.sk** (encrypted HTTPS only): your login (nick or ID and password), page requests and the actions
  you take in the app – replies, edits of your own posts, K and **mail messages** (recipient and text). The app identifies itself as
  `mokap/<version> (Android; +https://github.com/ntnlabs/mokap)`. kyberia.sk processes this data under its own rules.
- **“Using MOKAP”:** after each refresh the app opens the “using MOKAP” node on kyberia, so other kyberia users
  see that you are in mokap instead of “bookmarks”. You can turn this off in settings (Account).
- **Images and videos from other sites** (**off** by default): when enabled, the app downloads images and video
  previews in posts directly from third-party servers – without cookies, HTTPS only. Those servers see your IP
  address, as with any image download.
- **Mail:** the app downloads it only when you open the Mail tab (or pull to refresh) – like on the web, kyberia then
  marks it as read. The app keeps messages in memory only while running; it does not store them on the phone.
- Links outside kyberia open in your browser.

## What the app stores on the phone

- your kyberia login (session cookies), encrypted with a key from the Android Keystore;
- app settings and whether you log in by nick or by ID (not the nick or ID itself);
- notification state: node numbers from your bookmarks and replies with their counts, IDs of friends online,
  the number of unread mails, K counts of your posts and when K were last checked;
- check diagnostics (time, result, network state) – no personal data, used to find out why you were logged out;
- an image cache (avatars; third-party images if enabled);
- images **you save** with the “Save” button – into your gallery (Pictures/mokap). The app takes them from the image
  cache, or downloads them again from the same place; it needs no permission for this and writes nothing else to
  the gallery. Saved images stay in the gallery even after you uninstall the app.

None of this (except images you saved to the gallery) is **backed up to the cloud or transferred to another device**, and none of it is sent to the developer.

## Notifications

Notifications are local – the app checks kyberia itself at the interval set in settings. It does not use
Firebase or any other push server. A mail notification may contain the sender's nick; it is not stored. When the
open app refreshes, it also checks the number of new mails (a small request), so the notification disappears once
you have read your mail elsewhere. The K notification uses the same page as the others; after a longer break (e.g.
quiet hours at night) the app asks kyberia once for a K overview covering the whole break, so no K gets lost.

## Permissions

Internet and network state (talking to kyberia), notifications, starting after a reboot and short wake-ups
(regular checks via Android WorkManager).

## Third parties

The app contains no third-party SDKs for analytics, advertising or crash reporting, and does not sell or share
data with anyone. Google Play and Android may collect technical data (e.g. crashes) under their own policies
and your phone settings – the app itself does not.

## Deleting data

- **Logging out** in the app removes your login from the phone.
- **Uninstalling** the app or “Clear data” in Android settings removes everything the app stored.
- Your account and posts on kyberia are managed by **kyberia.sk** – the app cannot create or delete accounts.

## Children

The app is not intended for children. kyberia also contains adult content; nodes with “NSFW” in their name
are hidden by default.

## Changes

Changes to this policy are published in the repository (history is in git). The effective date is at the top.

## Contact

Questions and feedback: <https://github.com/ntnlabs/mokap/issues> (also from the app: Settings → “Nahlásiť chybu alebo nápad”
opens this page in your browser; the app itself sends nothing). Issues are public – do not post passwords, other people's nicks or mail.
