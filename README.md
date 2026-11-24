# Honest: mobilní appka Honest Chatu (Honest OS)

Appka pro tým Honest Lead: otevře Honest OS (https://os.honestlead.cz) rovnou na Honest Chatu a doručí upozornění
na novou zprávu i se zavřenou appkou. Náhrada interního WhatsAppu.

- **Instalace a návod:** https://valouseko.github.io/honest-app/ (GitHub Pages ze složky `docs/`)
- **Stálý odkaz na poslední APK:** https://github.com/valouseko/honest-app/releases/latest/download/honest.apk

## Jak to funguje
- **Android** (`android/`): Trusted Web Activity (TWA) nad `https://os.honestlead.cz/#lab-chat`, stejný základ jako
  Bubblewrap od Googlu (`com.google.androidbrowserhelper`). Uvnitř běží Chrome, takže přihlášení, chat, přílohy
  i Web Push jsou přesně ty z webu. Upozornění doručuje Chrome a díky `DelegationService` je ukáže jako upozornění
  appky Honest, i když je appka zavřená. Žádný Firebase, žádný vlastní server.
- **Spárování s webem:** `/.well-known/assetlinks.json` na os.honestlead.cz (v repu honestlead,
  `apps/honest-os/public/.well-known/`) obsahuje otisk podpisového klíče ze `signing/fingerprint-sha256.txt`.
  Bez něj appka funguje, jen ukáže lištu s adresou.
- **iPhone:** dnes Honest OS přidaný na plochu (Web Push z iOS 16.4). Připravený Expo projekt pro TestFlight
  v `ios-expo/`, postup v [docs/IOS-TESTFLIGHT.md](docs/IOS-TESTFLIGHT.md).

## Nová verze
Skoro nikdy netřeba: obsah se mění na webu. Novou APK jen při změně nativního obalu (`android/`):
1. Změna v `android/`, commit a push do `main`.
2. GitHub Actions (`.github/workflows/android.yml`) postaví APK, ověří podpis a vydá release `v1.0.<číslo běhu>`
   s `honest.apk` a `version.json`. Ručně: Actions › Android › Run workflow.
3. Appka při spuštění (nejvýš jednou za 6 h) zkontroluje `version.json` a ukáže upozornění „Nová verze Honest“.
   Nová verze se instaluje přes starou, nic se neztratí.

## Podpisový klíč (nikdy neztratit)
- GitHub secrets repa: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (PKCS12, alias `honest`).
- Záloha jen lokálně: `personal-os/functions/honest-app/release.keystore` + `.env` (gitignore, Syncthing).
- Jiný klíč = novou verzi nejde nainstalovat přes starou a nesedí `assetlinks.json`. Repo je veřejné (APK se stahuje
  bez přihlášení), proto v něm nikdy nic tajného.
