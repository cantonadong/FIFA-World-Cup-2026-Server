# FIFA World Cup 2026 App

I built a mobile app for the FIFA World Cup 2026, focused on match data, standings, schedules, teams, and player details.

## Highlights

- Live match result updates from a Vercel-hosted CSV data source
- Automatic group standings calculation based on match results
- Knockout-stage matchup updates driven by group results
- Team detail pages with World Cup history, squad list, player ratings, avatars, and club info
- Player detail pages with national team stats, physical profile, club badge, league info, and market value from roster data
- Pull-to-refresh support for standings and schedule data
- Signed Android release build for production installation

## Technical Highlights

- Built with Kotlin and Jetpack Compose
- CSV-driven data pipeline for easy tournament data maintenance
- Remote data sync via Vercel static hosting
- Local asset and roster caching for faster screen loading
- Dynamic standings and knockout logic generated from match results
- Android release signing with a production keystore

This project helped me combine mobile UI, sports data modeling, remote data sync, and automated tournament logic into one complete Android app.

