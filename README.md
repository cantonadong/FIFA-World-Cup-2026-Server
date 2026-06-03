# FIFA World Cup 2026

Android app for following the FIFA World Cup 2026. The app shows group standings, fixtures, knockout bracket updates, team details, player rankings, and match results refreshed from a Vercel-hosted CSV file.

## Features

- Group standings and knockout bracket generated from match results
- Pull-to-refresh match result updates from the server CSV
- Schedule, venues, team pages, squad lists, player detail pages, and player rankings
- Offline bundled tournament data with remote result updates when refreshed

## Android Package

- Package name: `com.carldong.fifa.worldcup2026`
- Version: `1.0`
- Minimum SDK: 26
- Target SDK: 36

## Project Structure

- `App/` - Android application source
- `Server/public/data/results.csv` - match result CSV used by the hosted data endpoint
- `Data/`, `Pic/`, `UI/` - source data, image assets, and design/reference files

## Result CSV

The app fetches match results from:

```text
https://fifa-world-cup-2026-server.vercel.app/data/results.csv
```

Expected CSV columns:

```csv
match_id,team1,team2,score1,score2,penalty1,penalty2
```

Use English team names to avoid display and matching issues.

## Build

From `D:\Dev\WC26\App`:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

Unsigned release output:

```text
App/app/build/outputs/apk/release/app-release-unsigned.apk
```

## Release Signing

Keep `fifa-release.jks` private. Do not commit keystores or password files.

```powershell
cd D:\Dev\WC26\App

& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\zipalign.exe" -p -f 4 app\build\outputs\apk\release\app-release-unsigned.apk app\build\outputs\apk\release\fifa-worldcup-2026-v1.0-aligned.apk

& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" sign --ks fifa-release.jks --ks-key-alias fifa2026 --out app\build\outputs\apk\release\fifa-worldcup-2026-v1.0.apk app\build\outputs\apk\release\fifa-worldcup-2026-v1.0-aligned.apk

& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --verbose app\build\outputs\apk\release\fifa-worldcup-2026-v1.0.apk
```

Upload the signed APK to GitHub Releases instead of committing it to the repository.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
