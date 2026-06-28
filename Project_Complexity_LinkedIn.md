# FIFA World Cup 2026 App - Project Complexity Summary

This Android app is a data-driven World Cup experience built around real tournament data, squad data, and dynamic match processing.

## Scale Of The Data Work

- 48 teams in `Team.csv`
- 1,263 player records in `player.csv`
- 1,264 squad records in `roster.csv`
- 104 matches in `match.csv`
- 104 synced results in `results.csv`
- 16 venues in `vanue.csv`
- 62 `TeamExtras` data blocks for team history and stat packs
- 10 manual player mappings for difficult identity cases
- 82 historical World Cup result entries across team history data

## What Made It Complex

- I had to reconcile multiple data sources: teams, fixtures, venues, players, squads, images, and remote match results.
- Player identity matching was not straightforward. I handled accent marks, non-English characters, spacing differences, alternate spellings, and country-name mismatches.
- Some examples include name normalization for players like `Kylian Mbappé`, `Nicolás González`, and other non-ASCII cases.
- A large part of the work was mapping player names to the correct roster row, player rating record, avatar image, club badge, and club metadata.
- I also had to handle missing or partial data safely, especially when a player had no image or no market value.

## Tournament Logic

- Match results are pulled from an external CSV source.
- Group standings are recalculated automatically from the latest results.
- Knockout-stage pairings update from group outcomes and tournament rules.
- The app checks and renders the group stage, schedule, team detail pages, and player detail pages from the same data pipeline.

## Engineering Work Done

- Built a CSV-driven data layer for teams, matches, venues, rosters, and player profiles.
- Added name normalization and fuzzy matching so data from different files can be linked reliably.
- Added caching and background loading so the app remains usable even with large roster and image sets.
- Implemented team history, squad listing, player navigation, club data, league info, radar charts, and pull-to-refresh behavior.
- Prepared production Android release signing and versioned APK builds.

## Why This Matters

This project is not just a UI demo. It combines real-world data cleaning, cross-file mapping, tournament rules, remote sync, image handling, and mobile performance tuning into one production-style Android app.

