# FIFA World Cup 2026

Android app for following the FIFA World Cup 2026. The app shows group standings, fixtures, knockout bracket updates, team details, player rankings, and match results refreshed from a Vercel-hosted CSV file.

## Snapshots
<img width="398" height="876" alt="1" src="https://github.com/user-attachments/assets/c8a0ad13-a873-4ad1-b477-209c0896dd85" />
<img width="398" height="876" alt="2" src="https://github.com/user-attachments/assets/a6f59e5c-df4f-413c-a399-3ffccb78b9b0" />
<img width="398" height="876" alt="3" src="https://github.com/user-attachments/assets/08dfad18-7c5f-4fc2-af89-d0cd5e8eba7d" />
<img width="398" height="876" alt="4" src="https://github.com/user-attachments/assets/afa36a88-2083-42ad-be66-a8a86cefb98e" />
<img width="398" height="876" alt="5" src="https://github.com/user-attachments/assets/293af422-46b8-415a-baa2-2beedaaf6aba" />
<img width="398" height="876" alt="6" src="https://github.com/user-attachments/assets/e8d0a12f-3ea5-43f3-8068-ac07c2a58fe5" />
<img width="398" height="876" alt="7" src="https://github.com/user-attachments/assets/ab4e89e3-0e77-4bce-a545-e2d32c315d57" />

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
