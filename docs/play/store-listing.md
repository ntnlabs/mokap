# Play Console – store listing (návrh)

Limity Play: názov ≤ 30 znakov, krátky popis ≤ 80, dlhý popis ≤ 4000, „Čo je nové“ ≤ 500.
Predvolený jazyk listingu: **slovenčina (sk-SK)**, preklad: **angličtina (en-US)**.

Appka sa nesmie vydávať za oficiálnu aplikáciu kyberie ani používať jej logo – preto „neoficiálny“ hneď
v krátkom popise a vety na konci dlhého popisu. Súhlas adminov kyberie sa hodí mať po ruke, keby si ho Google
vypýtal (pri appkách pre cudzie služby sa to stáva).

## Slovensky (sk-SK)

**Názov:** `mokap`

**Krátky popis:**

```
Neoficiálny klient pre kyberia.sk – bookmarky, odpovede, K a notifikácie.
```

**Dlhý popis:**

```
mokap je neoficiálny Android klient pre komunitu kyberia.sk. Nie je to prehliadač ani zabalený web: appka si stiahne stránky kyberie a ukáže ich po svojom – prehľadne na mobile, s lokálnymi notifikáciami.

ČO VIE
• Bookmarky – A–Z alebo podľa noviniek, filter „len nové“, hľadanie bez diakritiky
• Odpovede na tvoje príspevky, Last a Top K
• Tvoje K za poslednú hodinu aj za nastaviteľný čas
• Čítanie nodov priamo v appke – téma, diskusia vo vlákne, obrázky
• Odpovedanie do nodu aj na konkrétnu odpoveď, úprava vlastných príspevkov
• Dávanie K – len pridať, vždy s potvrdením
• Priatelia online, kto je kde a kde sa práve diskutuje
• Notifikácie: odpovede, pošta, novinky v bookmarkoch, priateľ online – každá zvlášť, s tichým režimom
• Témy: systémová, svetlá, tmavá a tlmená
• Nody s „NSFW“ v názve predvolene skryté

SÚKROMIE
• Heslo appka neukladá, prihlásenie drží zašifrované v telefóne
• Komunikuje len s kyberia.sk – žiadna analytika, reklamy ani sledovanie
• Obrázky z iných stránok len vtedy, keď ich zapneš
• Otvorený zdrojový kód: github.com/ntnlabs/mokap

Na používanie potrebuješ účet na kyberia.sk.

mokap nie je oficiálna aplikácia kyberia.sk – vyvíja ju člen komunity vo voľnom čase.
```

**Čo je nové:** pri každom builde v [`whats-new.txt`](whats-new.txt) – v Play formáte s jazykmi, rovno na
skopírovanie do Release notes; job `apk` ho vypíše aj do súhrnu behu.

## English (en-US)

**App name:** `mokap`

**Short description:**

```
Unofficial client for kyberia.sk – bookmarks, replies, K and notifications.
```

**Full description:**

```
mokap is an unofficial Android client for the kyberia.sk community. It is not a browser or a wrapped website: the app downloads kyberia pages and shows them its own way – clear on a phone, with local notifications.

FEATURES
• Bookmarks – A–Z or by news, "only new" filter, search without diacritics
• Replies to your posts, Last and Top K
• Your K for the last hour and for an adjustable period
• Reading nodes right in the app – topic, threaded discussion, images
• Replying to a node or to a specific reply, editing your own posts
• Giving K – add only, always with confirmation
• Friends online, who is where and where people are talking right now
• Notifications: replies, mail, bookmark news, friend online – each on its own, with quiet hours
• Themes: system, light, dark and dimmed
• Nodes with "NSFW" in their name hidden by default

PRIVACY
• The app never stores your password; your login is kept encrypted on the phone
• Talks only to kyberia.sk – no analytics, ads or tracking
• Images from other sites only when you turn them on
• Open source: github.com/ntnlabs/mokap

You need an account on kyberia.sk to use the app.

mokap is not an official kyberia.sk app – it is developed by a community member in their spare time.
```

**What's new:** see [`whats-new.txt`](whats-new.txt) (both languages, Play format).

## Ostatné polia

| Pole | Hodnota |
|---|---|
| App category | **Social** |
| Tags | Social networking, Forums (čo Play ponúkne) |
| Website | `https://github.com/ntnlabs/mokap` |
| Contact e-mail | **tvoj** – vyplniť len v Play Console, nie do repa |
| Privacy policy | `https://github.com/ntnlabs/mokap/blob/main/PRIVACY.md` |

## Grafika

Hotové v [`graphics/`](graphics/), vyrobené skriptom [`make_graphics.py`](graphics/make_graphics.py) z launcher
ikony appky. Pri zmene ikony ho spusti znova (`python3 docs/play/graphics/make_graphics.py`, Pillow + fonty DejaVu).

- **Ikona 512 × 512:** [`icon-512.png`](graphics/icon-512.png) – rovnaké K ako launcher, bez priehľadnosti (rohy
  si Play zaoblí sám).
- **Feature graphic 1024 × 500:** [`feature-graphic-sk.png`](graphics/feature-graphic-sk.png) pre sk-SK,
  [`feature-graphic-en.png`](graphics/feature-graphic-en.png) pre en-US – bez loga kyberie.
- **Screenshoty z telefónu:** aspoň 2 (odporúčané 4–8), napr. Bookmarky, node s diskusiou, záložka K, notifikácie.
  Pred nahratím skontroluj, že na nich nie sú cudzie nicky, súkromná pošta či príspevky, ktoré by autori nechceli
  ukázať mimo kyberie – najlepšie vlastný alebo testovací účet a verejné nody.
