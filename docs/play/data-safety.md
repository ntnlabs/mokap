# Play Console – Data safety (návrh odpovedí)

Odpovede podľa toho, čo appka naozaj robí (pozri [PRIVACY.md](../../PRIVACY.md)). Play počíta ako „collected“
všetko, čo appka pošle z telefónu – aj keď to ide na kyberia.sk a vývojár to nikdy nevidí. Preto deklarujeme
prihlásenie, príspevky a akcie, hoci ich spracúva len kyberia.

Pri zmene správania appky (nová služba, SDK, analytika…) treba formulár aj [PRIVACY.md](../../PRIVACY.md) upraviť.

## 1. Data collection and security

| Otázka | Odpoveď | Prečo |
|---|---|---|
| Does your app collect or share any of the required user data types? | **Yes** | Prihlásenie, príspevky a akcie idú na kyberia.sk. |
| Is all of the user data collected by your app encrypted in transit? | **Yes** | Požiadavky idú len cez HTTPS (`UrlPolicy`, cleartext je vypnutý). |
| Which of the following methods of account creation does your app support? / Does your app allow users to create an account? | **My app does not allow users to create an account** | Účet sa zakladá na webe kyberie; appka sa len prihlasuje. Preto netreba URL na zrušenie účtu. |

## 2. Data types

Deklarovať **len tieto tri**. Ostatné typy (poloha, kontakty, fotky, finančné údaje, zdravie, súbory, kalendár,
identifikátory zariadenia, diagnostika appky…) appka z telefónu neposiela.

### Personal info → **User IDs**
- Collected: **Yes** · Shared: **No**
- Processed ephemerally: **No**
- Required or optional: **Required** (bez prihlásenia appka nefunguje)
- Purposes: **App functionality**, **Account management**
- Poznámka: nick alebo ID (a heslo) pri prihlásení na kyberia.sk. Heslo sa v appke neukladá.

### App activity → **Other user-generated content**
- Collected: **Yes** · Shared: **No**
- Processed ephemerally: **No**
- Required or optional: **Optional** (len keď píšeš alebo upravuješ príspevok)
- Purposes: **App functionality**
- Poznámka: odpovede a úpravy vlastných príspevkov, ktoré odošleš na kyberiu.

### App activity → **App interactions**
- Collected: **Yes** · Shared: **No**
- Processed ephemerally: **No**
- Required or optional: **Required**
- Purposes: **App functionality**
- Poznámka: načítanie stránok kyberie, dané K a „používa MOKAP“ – kyberia ich vidí ako bežné používanie webu.

**Shared = No** pri všetkých: prenos ide na službu, do ktorej sa používateľ sám prihlasuje, a len na jeho
pokyn. Play to vyníma zo „sharing“ (user-initiated action, kde používateľ prenos očakáva).

## 3. Security practices

| Otázka | Odpoveď | Prečo |
|---|---|---|
| Data is encrypted in transit | **Yes** | Len HTTPS. |
| You provide a way for users to request that their data is deleted | **No** | Vývojár žiadne údaje nemá. Lokálne údaje zmaže odhlásenie alebo odinštalovanie, účet a príspevky spravuje kyberia.sk (je to v PRIVACY.md). |
| Committed to follow the Play Families Policy | nevypĺňať | Appka nie je pre deti. |
| Independent security review | **No** | |

## 4. Súvisiace formuláre v *App content*

- **Privacy policy:** `https://github.com/ntnlabs/mokap/blob/main/PRIVACY.md`
- **Ads:** *No, my app does not contain ads.*
- **App access:** časti appky sú za prihlásením. Odpoveď: *All or some functionality is restricted* a pre
  reviewera popísať, že ide o klienta kyberia.sk, ktorý vyžaduje vlastný účet na kyberii. Na review treba
  testovací účet kyberie (nick + heslo vložiť len do Play Console, **nikdy nie do repa**) – inak reviewer appku
  neotvorí.
- **Target audience and content:** **18+** (kyberia obsahuje aj obsah pre dospelých; appka nie je pre deti).
- **Content rating** (dotazník IARC): kategória *Social / Communication*; **users can interact / exchange content:
  Yes**; obsah tvoria používatelia a appka ho nemoderuje (NSFW skrýva len podľa názvu nodu). Hodnotenie vyjde
  pravdepodobne „Mature 17+ / 18“.
- **News app:** No · **Government app:** No · **Financial features:** None · **Health:** No.
