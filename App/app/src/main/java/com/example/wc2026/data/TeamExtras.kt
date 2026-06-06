package com.carldong.fifa.worldcup2026.data

data class TeamStats(
    val ovr: Int = 65,
    val pac: Int = 65,
    val sho: Int = 65,
    val pas: Int = 65,
    val dri: Int = 65,
    val def: Int = 65,
    val phy: Int = 65
)

// Backward-compat alias so call sites that still use TeamOpta compile
typealias TeamOpta = TeamStats

data class WCHistoryEntry(val year: Int, val hostFlagFile: String, val result: String)

data class TeamExtras(val opta: TeamStats, val history: List<WCHistoryEntry>)

private fun e(yr: Int, host: String, res: String) = WCHistoryEntry(yr, host, res)
private fun s(ovr:Int,pac:Int,sho:Int,pas:Int,dri:Int,def:Int,phy:Int) = TeamStats(ovr,pac,sho,pas,dri,def,phy)

val TEAM_EXTRAS: Map<String, TeamExtras> = mapOf(
    // ── WC2026 qualified teams — stats from Team.csv ──
    "ar"    to TeamExtras(s(85,74,79,80,83,65,78), listOf(
        e(2022,"Qatar.png","Champion"), e(2018,"Russia.png","R16"), e(2014,"Brazil.png","Runner-up"),
        e(2010,"South Africa.png","QF"), e(2006,"Germany.png","QF"), e(2002,"South Korea.png","GS"),
        e(1998,"France.png","QF"), e(1994,"United States.png","R16"),
        e(1990,"Italy.png","Runner-up"), e(1986,"Mexico.png","Champion"))),
    "fr"    to TeamExtras(s(87,83,67,76,83,67,79), listOf(
        e(2022,"Qatar.png","Runner-up"), e(2018,"Russia.png","Champion"), e(2014,"Brazil.png","QF"),
        e(2010,"South Africa.png","GS"), e(2006,"Germany.png","Runner-up"),
        e(2002,"South Korea.png","GS"), e(1998,"France.png","Champion"), e(1986,"Mexico.png","3rd"))),
    "br"    to TeamExtras(s(86,79,68,76,79,69,81), listOf(
        e(2022,"Qatar.png","QF"), e(2018,"Russia.png","QF"), e(2014,"Brazil.png","4th"),
        e(2010,"South Africa.png","QF"), e(2006,"Germany.png","QF"), e(2002,"South Korea.png","Champion"),
        e(1998,"France.png","Runner-up"), e(1994,"United States.png","Champion"),
        e(1990,"Italy.png","R16"), e(1986,"Mexico.png","QF"))),
    "es"    to TeamExtras(s(87,79,77,83,86,63,74), listOf(
        e(2022,"Qatar.png","QF"), e(2018,"Russia.png","R16"), e(2014,"Brazil.png","GS"),
        e(2010,"South Africa.png","Champion"), e(2006,"Germany.png","R16"),
        e(2002,"South Korea.png","QF"), e(1998,"France.png","R16"), e(1994,"United States.png","QF"))),
    "gb-eng" to TeamExtras(s(85,76,73,79,82,66,78), listOf(
        e(2022,"Qatar.png","QF"), e(2018,"Russia.png","4th"), e(2014,"Brazil.png","GS"),
        e(2010,"South Africa.png","R16"), e(2006,"Germany.png","QF"),
        e(2002,"South Korea.png","QF"), e(1998,"France.png","R16"),
        e(1990,"Italy.png","4th"), e(1986,"Mexico.png","QF"), e(1966,"England.png","Champion"))),
    "de"    to TeamExtras(s(86,74,67,79,79,67,79), listOf(
        e(2022,"Qatar.png","GS"), e(2018,"Russia.png","GS"), e(2014,"Brazil.png","Champion"),
        e(2010,"South Africa.png","3rd"), e(2006,"Germany.png","3rd"),
        e(2002,"South Korea.png","Runner-up"), e(1998,"France.png","QF"),
        e(1994,"United States.png","QF"), e(1990,"Italy.png","Champion"), e(1986,"Mexico.png","Runner-up"))),
    "us"    to TeamExtras(s(79,80,66,71,78,62,73), listOf(
        e(2022,"Qatar.png","R16"), e(2018,"Russia.png","DNQ"), e(2014,"Brazil.png","R16"),
        e(2010,"South Africa.png","R16"), e(2002,"South Korea.png","QF"),
        e(1994,"United States.png","R16"), e(1990,"Italy.png","GS"))),
    "jp"    to TeamExtras(s(79,78,68,72,78,64,72), listOf(
        e(2022,"Qatar.png","R16"), e(2018,"Russia.png","R16"), e(2014,"Brazil.png","GS"),
        e(2010,"South Africa.png","R16"), e(2006,"Germany.png","GS"),
        e(2002,"South Korea.png","QF"), e(1998,"France.png","GS"))),
    "ma"    to TeamExtras(s(81,78,72,74,79,57,74), listOf(
        e(2022,"Qatar.png","SF"), e(2018,"Russia.png","GS"), e(2014,"Brazil.png","GS"))),
    "pt"    to TeamExtras(s(86,75,74,82,83,65,77), listOf(
        e(2022,"Qatar.png","QF"), e(2018,"Russia.png","R16"), e(2014,"Brazil.png","GS"),
        e(2010,"South Africa.png","R16"), e(2006,"Germany.png","4th"))),
    "nl"    to TeamExtras(s(85,77,68,77,79,78,79), listOf(
        e(2022,"Qatar.png","QF"), e(2014,"Brazil.png","3rd"),
        e(2010,"South Africa.png","Runner-up"), e(2006,"Germany.png","R16"),
        e(1998,"France.png","4th"), e(1994,"United States.png","QF"))),
    "kr"    to TeamExtras(s(77,74,67,70,75,54,71), listOf(
        e(2022,"Qatar.png","R16"), e(2018,"Russia.png","GS"), e(2014,"Brazil.png","GS"),
        e(2010,"South Africa.png","R16"), e(2006,"Germany.png","GS"),
        e(2002,"South Korea.png","SF"), e(1998,"France.png","GS"))),
    "mx"    to TeamExtras(s(78,75,75,72,77,51,74), listOf(
        e(2022,"Qatar.png","GS"), e(2018,"Russia.png","R16"), e(2014,"Brazil.png","R16"),
        e(2010,"South Africa.png","R16"), e(2006,"Germany.png","R16"),
        e(2002,"South Korea.png","R16"), e(1998,"France.png","R16"), e(1994,"United States.png","R16"))),
    "hr"    to TeamExtras(s(81,72,71,75,80,64,75), listOf(
        e(2022,"Qatar.png","3rd"), e(2018,"Russia.png","Runner-up"),
        e(2014,"Brazil.png","GS"), e(2006,"Germany.png","QF"), e(1998,"France.png","3rd"))),
    "ca"    to TeamExtras(s(77,79,66,67,72,56,77), listOf(
        e(2022,"Qatar.png","GS"), e(1986,"Mexico.png","GS"))),
    "sn"    to TeamExtras(s(80,76,69,72,77,58,76), listOf(
        e(2022,"Qatar.png","QF"), e(2018,"Russia.png","GS"), e(2002,"South Korea.png","QF"))),
    "be"    to TeamExtras(s(83,73,77,80,82,54,74), listOf(
        e(2022,"Qatar.png","GS"), e(2018,"Russia.png","3rd"),
        e(2014,"Brazil.png","QF"), e(2002,"South Korea.png","R16"))),
    "tr"    to TeamExtras(s(81,77,71,75,78,64,74), listOf(
        e(2002,"South Korea.png","3rd"), e(1954,"Switzerland.png","3rd"))),
    "ch"    to TeamExtras(s(80,75,68,72,77,67,79), listOf(
        e(2022,"Qatar.png","R16"), e(2018,"Russia.png","R16"), e(2014,"Brazil.png","R16"))),
    "au"    to TeamExtras(s(72,74,62,66,68,62,71), listOf(
        e(2022,"Qatar.png","R16"), e(2018,"Russia.png","GS"),
        e(2014,"Brazil.png","GS"), e(2006,"Germany.png","R16"))),
    "co"    to TeamExtras(s(79,75,70,72,77,64,76), listOf(
        e(2018,"Russia.png","R16"), e(2014,"Brazil.png","QF"),
        e(1994,"United States.png","R16"), e(1990,"Italy.png","R16"))),
    "uy"    to TeamExtras(s(76,72,68,73,74,70,76), listOf(
        e(2022,"Qatar.png","GS"), e(2018,"Russia.png","R16"),
        e(2014,"Brazil.png","GS"), e(2010,"South Africa.png","SF"))),
    "sa"    to TeamExtras(s(72,76,57,64,68,60,74), listOf(
        e(2022,"Qatar.png","R16"), e(2018,"Russia.png","GS"), e(2014,"Brazil.png","GS"))),
    "eg"    to TeamExtras(s(79,75,66,70,73,55,74), listOf(
        e(2018,"Russia.png","GS"), e(1990,"Italy.png","R16"))),
    "at"    to TeamExtras(s(80,75,62,72,75,71,74), listOf(
        e(1998,"France.png","GS"), e(1990,"Italy.png","GS"), e(1982,"Spain.png","GS"))),
    "ng"    to TeamExtras(s(74,80,66,67,75,65,76), listOf(
        e(2018,"Russia.png","GS"), e(2014,"Brazil.png","R16"), e(2010,"South Africa.png","GS"))),
    "ec"    to TeamExtras(s(68,69,60,65,68,62,68), listOf(
        e(2022,"Qatar.png","GS"), e(2014,"Brazil.png","GS"), e(2006,"Germany.png","GS"))),
    "ir"    to TeamExtras(s(78,74,78,74,78,38,68), listOf(
        e(2018,"Russia.png","GS"), e(2014,"Brazil.png","GS"), e(2006,"Germany.png","GS"),
        e(1998,"France.png","GS"), e(1978,"Argentina.png","GS"))),
    "tn"    to TeamExtras(s(73,72,61,66,71,57,69), listOf(
        e(2022,"Qatar.png","GS"), e(2018,"Russia.png","GS"), e(2006,"Germany.png","GS"))),
    "py"    to TeamExtras(s(76,73,64,65,72,58,71), listOf(
        e(2010,"South Africa.png","QF"), e(2006,"Germany.png","GS"), e(2002,"South Korea.png","R16"))),
    "ua"    to TeamExtras(s(70,72,66,69,70,68,71), listOf(
        e(2006,"Germany.png","QF"))),
    "pl"    to TeamExtras(s(71,72,68,70,72,68,72), listOf(
        e(2022,"Qatar.png","R16"), e(2018,"Russia.png","GS"), e(2006,"Germany.png","R16"))),
    // ── WC2026 teams not previously included — stats from Team.csv ──
    "cz"    to TeamExtras(s(78,70,71,74,75,56,78), emptyList()),
    "za"    to TeamExtras(s(65,74,44,52,59,57,69), emptyList()),
    "qa"    to TeamExtras(s(62,68,55,60,64,56,66), listOf(
        e(2022,"Qatar.png","GS"))),
    "ba"    to TeamExtras(s(75,67,62,64,70,56,75), listOf(
        e(2022,"Qatar.png","GS"))),
    "sc"    to TeamExtras(s(78,72,65,72,75,72,75), emptyList()),
    "ht"    to TeamExtras(s(67,70,53,59,64,52,70), emptyList()),
    "ci"    to TeamExtras(s(80,78,58,69,74,69,78), emptyList()),
    "cw"    to TeamExtras(s(67,77,55,62,69,56,66), emptyList()),
    "se"    to TeamExtras(s(79,78,64,70,76,63,76), emptyList()),
    "nz"    to TeamExtras(s(71,67,58,64,67,60,76), emptyList()),
    "cv"    to TeamExtras(s(72,72,56,63,69,50,68), emptyList()),
    "no"    to TeamExtras(s(81,74,73,73,77,59,79), emptyList()),
    "jo"    to TeamExtras(s(75,79,29,46,52,73,83), emptyList()),
    "cd"    to TeamExtras(s(76,76,58,65,71,61,76), emptyList()),
    "uz"    to TeamExtras(s(76,82,55,62,70,59,71), emptyList()),
    "gh"    to TeamExtras(s(77,75,65,70,75,58,73), emptyList()),

    // ── Teams not in WC2026 — estimated stats ──
    "it"    to TeamExtras(s(83,74,76,80,78,79,74), listOf(
        e(2022,"Qatar.png","DNQ"), e(2018,"Russia.png","DNQ"), e(2014,"Brazil.png","GS"),
        e(2010,"South Africa.png","GS"), e(2006,"Germany.png","Champion"),
        e(2002,"South Korea.png","R16"), e(1998,"France.png","QF"),
        e(1994,"United States.png","Runner-up"), e(1990,"Italy.png","3rd"))),
    "pe"    to TeamExtras(s(70,72,64,66,70,66,74), listOf(
        e(2018,"Russia.png","GS"), e(1982,"Spain.png","GS"))),
    "cl"    to TeamExtras(s(73,76,63,66,72,65,70), listOf(
        e(2014,"Brazil.png","QF"), e(2010,"South Africa.png","R16"), e(1962,"Chile.png","3rd"))),
    "dz"    to TeamExtras(s(68,70,62,65,67,64,68), listOf(
        e(2014,"Brazil.png","R16"), e(2010,"South Africa.png","GS"), e(1982,"Spain.png","GS"))),
    "ge"    to TeamExtras(s(64,66,60,63,65,62,65), listOf(
        e(2024,"Germany.png","R16"))),
    "ve"    to TeamExtras(s(60,62,56,59,61,58,62), emptyList()),
    "bo"    to TeamExtras(s(55,58,50,54,56,56,60), listOf(
        e(1994,"United States.png","GS"))),
    "jm"    to TeamExtras(s(59,64,54,56,60,55,62), listOf(
        e(1998,"France.png","GS"))),
    "hn"    to TeamExtras(s(58,62,53,57,59,55,62), listOf(
        e(2014,"Brazil.png","GS"), e(2010,"South Africa.png","GS"), e(1982,"Spain.png","GS"))),
    "pa"    to TeamExtras(s(61,65,55,61,67,54,72), listOf(
        e(2018,"Russia.png","GS"))),
    "ro"    to TeamExtras(s(67,68,62,65,67,65,68), listOf(
        e(1998,"France.png","QF"), e(1994,"United States.png","QF"), e(1990,"Italy.png","QF"))),
    "al"    to TeamExtras(s(62,64,58,61,63,60,64), emptyList()),
    "iq"    to TeamExtras(s(58,62,55,57,60,55,60), listOf(
        e(1986,"Mexico.png","GS"))),
    "kw"    to TeamExtras(s(52,60,48,53,56,51,58), listOf(
        e(1982,"Spain.png","GS"))),
)

// Map team name (from teams.json) → TEAM_EXTRAS key — covers all 48 WC2026 teams
fun teamCode(name: String): String = when (name) {
    // ── WC2026 teams ─────────────────────────────────────────────────────
    "Argentina"                  -> "ar"
    "France"                     -> "fr"
    "Brazil"                     -> "br"
    "Spain"                      -> "es"
    "England"                    -> "gb-eng"
    "Germany"                    -> "de"
    "USA", "United States"       -> "us"
    "Japan"                      -> "jp"
    "Morocco"                    -> "ma"
    "Portugal"                   -> "pt"
    "Netherlands"                -> "nl"
    "South Korea"                -> "kr"
    "Senegal"                    -> "sn"
    "Mexico"                     -> "mx"
    "Croatia"                    -> "hr"
    "Australia"                  -> "au"
    "Canada"                     -> "ca"
    "Colombia"                   -> "co"
    "Uruguay"                    -> "uy"
    "Saudi Arabia"               -> "sa"
    "Egypt"                      -> "eg"
    "Austria"                    -> "at"
    "Switzerland"                -> "ch"
    "Belgium"                    -> "be"
    "Türkiye", "Turkey"          -> "tr"
    "Ecuador"                    -> "ec"
    "Iran"                       -> "ir"
    "Algeria"                    -> "dz"
    "Tunisia"                    -> "tn"
    "Paraguay"                   -> "py"
    "Ukraine"                    -> "ua"
    "Poland"                     -> "pl"
    "Iraq"                       -> "iq"
    "Panama"                     -> "pa"
    // ── WC2026 teams not in original TEAM_EXTRAS ─────────────────────────
    "Czech", "Czech Republic", "Czechia" -> "cz"
    "South Africa"               -> "za"
    "Qatar"                      -> "qa"
    "Bosnia & Herzegovina",
    "Bosnia and Herzegovina"     -> "ba"
    "Scotland"                   -> "sc"
    "Haiti"                      -> "ht"
    "Ivory Coast"                -> "ci"
    "Curaçao", "Curacao"         -> "cw"
    "Sweden"                     -> "se"
    "New Zealand"                -> "nz"
    "Cape Verde"                 -> "cv"
    "Norway"                     -> "no"
    "Jordan"                     -> "jo"
    "DR Congo"                   -> "cd"
    "Uzbekistan"                 -> "uz"
    "Ghana"                      -> "gh"
    // ── Non-WC2026 teams in TEAM_EXTRAS ──────────────────────────────────
    "Nigeria"                    -> "ng"
    "Italy"                      -> "it"
    "Chile"                      -> "cl"
    "Peru"                       -> "pe"
    "Georgia"                    -> "ge"
    "Venezuela"                  -> "ve"
    "Bolivia"                    -> "bo"
    "Jamaica"                    -> "jm"
    "Honduras"                   -> "hn"
    "Romania"                    -> "ro"
    "Albania"                    -> "al"
    "Kuwait"                     -> "kw"
    else                         -> name.lowercase().take(2)
}

// WC titles from Team.csv (total all-time wins, not just what's in history)
private val TEAM_TITLES_CSV: Map<String, Int> = mapOf(
    "br" to 5, "de" to 4, "it" to 4, "ar" to 3, "fr" to 2, "uy" to 2,
    "gb-eng" to 1, "es" to 1
)

fun teamOpta(name: String): TeamStats = TEAM_EXTRAS[teamCode(name)]?.opta ?: TeamStats()
fun teamHistory(name: String): List<WCHistoryEntry> = TEAM_EXTRAS[teamCode(name)]?.history ?: emptyList()
fun teamTitles(name: String): Int = TEAM_TITLES_CSV[teamCode(name)] ?: 0

