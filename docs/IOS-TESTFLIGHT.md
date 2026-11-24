# Honest na iPhone přes TestFlight: postup krok za krokem

Dnes iPhone funguje bez appky: Safari › os.honestlead.cz › Sdílet › Přidat na plochu › v Honest Chatu zapnout
upozornění (návod na https://valouseko.github.io/honest-app/#iphone). Tenhle postup je pro chvíli, kdy chceš
skutečnou appku přes TestFlight. Projekt je připravený ve složce `ios-expo/` (Expo, stejný účet `honestandrew` jako
messenger). Sám nic nestaví, dokud neuděláš kroky níž.

**Co to stojí:** Apple Developer Program 99 USD ročně. Nic jiného (EAS build pro pár buildů měsíčně je zdarma).
**Kolik času:** zaplacení a schválení účtu 1 až 2 dny čekání, samotná práce cca 30 minut.

## 1. Zaplať Apple Developer Program (jen poprvé)
1. Na iPhonu nebo v počítači otevři https://developer.apple.com/programs/enroll/ a klepni **Start your enrollment**.
2. Přihlas se svým Apple ID (musí mít zapnuté dvoufázové ověření).
3. Typ účtu:
   - **Individual** (na tvoje jméno): nejrychlejší, stačí občanka. Pro interní appku úplně stačí.
   - **Organization** (Honest Lead jako firma): potřebuje číslo D-U-N-S firmy, schválení trvá i týdny.
   Doporučení: **Individual**, appka je jen pro tým.
4. Zaplať 99 USD kartou. Počkej na e-mail „Welcome to the Apple Developer Program“ (hodiny až 2 dny).

## 2. Postav appku (na počítači, jednou na každou novou verzi)
V terminálu (PowerShell) ve složce repa:
```
cd "C:\Users\valou\Claude\Projects\Honest Enterprise\honest-app\ios-expo"
npm install
npx eas-cli login            # účet honestandrew (valouseko@gmail.com)
npx eas-cli init             # jen poprvé: založí projekt v Expo a zapíše projectId do app.json
npx eas-cli build -p ios --profile production
```
Během `build` se EAS zeptá:
- **Apple ID a heslo, kód z iPhonu**: zadej svoje (to, kterým jsi platil).
- **Generate a new Apple Distribution Certificate?** Yes.
- **Generate a new Apple Provisioning Profile?** Yes.
- **Set up Push Notifications / generate a new Apple Push Notifications service key?** Yes.
Build běží v cloudu cca 15 až 30 minut. Počítač může dělat cokoli jiného (nic se nestaví lokálně).

## 3. Pošli build do TestFlight
```
npx eas-cli submit -p ios --latest
```
- Poprvé se zeptá na název appky v App Store Connect: **Honest** (když je obsazený, třeba „Honest Lead Chat“).
- Po nahrání počkej 10 až 30 minut, než Apple build zpracuje (přijde e-mail).
- Otázku na šifrování neřešíš, je vyřešená v `app.json` (`ITSAppUsesNonExemptEncryption: false`).

## 4. Pozvi Míšu (a sebe)
Varianta A, **interní testeři** (bez schvalování Applem, nejrychlejší):
1. https://appstoreconnect.apple.com › **Users and Access** › + › e-mail Míši, role **Developer**. Míša potvrdí
   pozvánku z e-mailu.
2. **Apps › Honest › TestFlight › Internal Testing** › + skupina „Tým“ › přidej sebe a Míšu.
3. Oba dostanete e-mail z TestFlightu.

Varianta B, **veřejný odkaz** `testflight.apple.com/join/…` (jako mají jiné firmy):
1. **TestFlight › External Testing** › + skupina „Honest“ › přidej build › vyplň krátký popis a kontakt.
2. Apple build jednou zkontroluje (Beta App Review, obvykle do 1 dne).
3. Ve skupině zapni **Public Link** a zkopíruj odkaz.

## 5. Odkaz na landing page
V `docs/index.html` najdi `var TESTFLIGHT_URL = "";`, vlož odkaz mezi uvozovky, commitni a pushni. Landing pak ukáže:
„Nainstaluj z App Store aplikaci TestFlight, pak otevři tento odkaz a klepni Install.“

## 6. Instalace na iPhonu (to pošleš Míšovi)
1. Z App Store nainstaluj **TestFlight**.
2. Otevři pozvánku z e-mailu (nebo veřejný odkaz) › **Accept** › **Install**.
3. Otevři Honest, přihlas se do Honest OS, povol upozornění.

## Důležité vědět
- **Build v TestFlightu platí 90 dní.** Do té doby pusť znovu krok 2 a 3 (verze se zvýší sama), jinak appka přestane
  jít otevřít. Obsah (chat, Honest OS) se ale aktualizuje sám z webu, novou verzi kvůli němu dělat netřeba.
- **Upozornění v iOS appce zatím nechodí.** Na iPhonu ve WebView nefunguje Web Push. Appka si od Applu vezme push
  token a předá ho stránce (událost `honest-native-push`), ale Honest OS ho zatím neumí uložit ani na něj poslat.
  Chybí serverová část v honestlead:
  1. tabulka na tokeny (např. `lab.chat_expo_tokens`: osoba, token, platforma, kdy) a trasa, kam appka token pošle
     (HonestOS 2 zachytí `honest-native-push` a uloží ho pod přihlášenou osobu),
  2. nový kanál v `apps/honest-os/src/lib/chat/notify-server.ts` vedle Web Push: odeslání přes
     `https://exp.host/--/api/v2/push/send` (bez textu zprávy, jen „Nová zpráva v Honest Chatu“ a adresa konverzace),
  3. úklid neplatných tokenů (odpověď `DeviceNotRegistered`).
  Do té doby je na iPhonu lepší Honest OS přidaný na plochu (tam Web Push funguje) a appka z TestFlightu je jen
  pohodlnější okno. Telegram a e-mailové připomínky fungují v obou případech.
- Přihlášení v appce je oddělené od Safari: v appce se přihlásíš jednou zvlášť.
