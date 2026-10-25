# PD Measure

App Android per misurare la **distanza interpupillare (PD)** con la fotocamera frontale.

[![Beta](https://github.com/UncleDan/pd-measure/actions/workflows/beta.yml/badge.svg)](https://github.com/UncleDan/pd-measure/actions/workflows/beta.yml)
[![Release](https://github.com/UncleDan/pd-measure/actions/workflows/release.yml/badge.svg)](https://github.com/UncleDan/pd-measure/actions/workflows/release.yml)

- **Metodo iride** — nessun oggetto: usa come riferimento il diametro orizzontale dell'iride (11,7 mm in media, oppure il valore calibrato). Ogni tentativo è la mediana di 45 fotogrammi validi; si possono fare 1, 3 o 5 tentativi (media, con esclusione degli estremi su 5) e l'app mostra lo scarto tra i tentativi.
- **Metodo carta** — una carta formato ISO/IEC 7810 ID-1 (85,60 × 53,98 mm) appoggiata sulla fronte; si allineano due linee ai bordi della carta. Più preciso, e può **calibrare** il metodo iride per le misure successive.
- **PD da lontano** — la PD misurata a 30–50 cm è "da vicino" (occhi convergenti); viene convertita con `PD_lontano = PD_vicino × (D + 13,5) / D`, con D stimata dalla focale della fotocamera.
- **Tutti i PD**: lontano, intermedio (65 cm), lettura (40 cm) e misurata, binoculari e monoculari OD/OS. Intermedio e lettura sono riferiti al piano della lente: `PD = PD_lontano × (D − 12) / (D + 13,5)`.
- **Guida ai moduli d'ordine**: indica quale valore inserire (campo unico, due numeri OD/OS, progressive, lettura), arrotondato a 0,5 mm.
- **Privacy**: nessun permesso internet, nessuna immagine salvata. Vedi [PRIVACY.md](PRIVACY.md).

> Misura indicativa, non è un dispositivo medico.

## Tecnologia

Kotlin · Jetpack Compose (Material 3) · CameraX · MediaPipe Face Landmarker (478 punti, con iridi) · minSdk 24 · targetSdk 35.

| | |
|---|---|
| applicationId | `it.uncledan.pdmeasure` (beta: `.beta`, debug: `.debug`) |
| Logica di misura | `app/src/main/java/it/uncledan/pdmeasure/measure/` (Kotlin puro, con test JUnit) |
| Modello | `face_landmarker.task`, scaricato automaticamente da Gradle in `app/src/main/assets/` (non versionato) |

## Compilazione

```bash
./gradlew testDebugUnitTest assembleDebug
```

Serve JDK 17. Al primo build il task `downloadFaceModel` scarica il modello MediaPipe (~3,6 MB).

## Versioni

`versionName` = `MAJOR.MINOR.PATCH`, `versionCode` = `MAJOR*10000 + MINOR*100 + PATCH` (es. 1.1.0 → 10100).
I valori sono letterali in `app/build.gradle.kts` e il build fallisce se non sono coerenti.

## GitHub Actions

| Workflow | Quando | Cosa produce |
|---|---|---|
| `beta.yml` | push su `main`, pull request, manuale | test + APK beta (artifact); su `main` aggiorna la pre-release `beta` |
| `release.yml` | tag `vX.Y.Z` (= versionName), manuale | test + APK/AAB firmati; GitHub Release con APK e SHA-256 |
| `play-prepare.yml` | manuale | AAB firmato + note di versione + icona 512 per Play Console |

Secret richiesti per la firma: vedi [docs/FIRMA.md](docs/FIRMA.md).
Pubblicazione su Google Play: [docs/PUBBLICAZIONE_GOOGLE_PLAY.md](docs/PUBBLICAZIONE_GOOGLE_PLAY.md).

## Rilascio

```bash
# aggiorna versionName/versionCode in app/build.gradle.kts e CHANGELOG.md
git commit -am "Release 1.0.1"
git tag v1.0.1
git push && git push --tags
```

## Autore

Daniele Lolli (UncleDan)
