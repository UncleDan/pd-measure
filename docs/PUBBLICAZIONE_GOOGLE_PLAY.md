# Pubblicazione su Google Play

## Primo caricamento

1. Crea l'app su [Play Console](https://play.google.com/console): nome *PD Measure*, app gratuita.
2. Attiva la **firma dell'app di Google Play**: il keystore di `docs/FIRMA.md` diventa la *chiave di caricamento*.
3. Avvia il workflow **Play prepare** (*Actions → Play prepare → Run workflow*) indicando le note di versione.
4. Scarica l'artifact `PD-Measure-<versione>-play`: contiene l'AAB, le note `whatsnew-*`, l'icona 512 e `build-info.txt`.
5. In *Test → Test interno* crea una release e carica l'AAB.

## Scheda dello Store

- Icona: `docs/play/icon-512.png`.
- Grafica in evidenza 1024×500 e screenshot: da preparare.
- **Norme sulla privacy**: obbligatoria per l'uso della fotocamera. Pubblica `PRIVACY.md`
  (ad esempio con GitHub Pages) e inserisci l'URL.
- **Sicurezza dei dati**: nessun dato raccolto né condiviso; elaborazione solo sul dispositivo.
- Categoria suggerita: *Strumenti* (oppure *Salute e fitness*).
- Evita nella descrizione formulazioni da dispositivo medico ("diagnosi", "prescrizione"):
  l'app fornisce una misura indicativa.

## Aggiornamenti

1. Aumenta `versionName`/`versionCode` in `app/build.gradle.kts` (il build verifica la coerenza).
2. Commit, tag `vX.Y.Z`, push → `release.yml` crea la GitHub Release.
3. Avvia **Play prepare** e carica il nuovo AAB.
