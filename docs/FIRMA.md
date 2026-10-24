# Firma dell'app (keystore e secret GitHub)

## 1. Creare il keystore (una volta sola, da conservare con cura)

```bash
keytool -genkeypair -v -keystore pd-measure-release.jks \
  -alias pdmeasure -keyalg RSA -keysize 4096 -validity 10000
```

> Se perdi il keystore non potrai più aggiornare l'app distribuita fuori da Play
> (su Play, con la firma gestita da Google, è la *chiave di caricamento*).
> Non va mai committato: `*.jks` è in `.gitignore`.

## 2. Convertirlo in Base64

Linux/macOS:
```bash
base64 -w0 pd-measure-release.jks > keystore.b64
```
Windows (PowerShell):
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("pd-measure-release.jks")) | Set-Content keystore.b64
```

## 3. Secret del repository

*Settings → Secrets and variables → Actions → New repository secret*

| Secret | Valore |
|---|---|
| `KEYSTORE_BASE64` | contenuto di `keystore.b64` |
| `KEYSTORE_PASSWORD` | password del keystore |
| `KEY_ALIAS` | `pdmeasure` (o l'alias scelto) |
| `KEY_PASSWORD` | password della chiave |

## 4. Firma in locale (facoltativa)

Gradle legge le stesse variabili d'ambiente: `PDM_KEYSTORE_PATH`, `PDM_KEYSTORE_PASSWORD`,
`PDM_KEY_ALIAS`, `PDM_KEY_PASSWORD`. Senza `PDM_KEYSTORE_PATH` la release non è firmata
e la beta usa la chiave debug.
