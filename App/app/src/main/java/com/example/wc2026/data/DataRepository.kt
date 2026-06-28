package com.carldong.fifa.worldcup2026.data

import android.content.Context
import com.carldong.fifa.worldcup2026.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.Locale

interface DataRepository {
    suspend fun getTeams(): List<Team>
    suspend fun getMatches(): List<Match>
    suspend fun getVenues(): List<Venue>
    suspend fun refreshMatches(): Result<Unit>
    fun observeMatchVersion(): StateFlow<Long>
    fun getGroupStandings(teams: List<Team>, matches: List<Match>): Map<String, List<TeamStanding>>
    suspend fun getRoster(countryName: String): List<RosterPlayer>
    suspend fun getRosterImages(countryName: String): List<RosterPlayer>
    fun buildPlayerForNav(rp: RosterPlayer, countryName: String): Player?
}

private object MatchSyncState {
    var remoteMatches: List<Match>? = null
    val version = MutableStateFlow(0L)

    fun publish(matches: List<Match>) {
        remoteMatches = matches
        version.value = version.value + 1
    }
}

private object AssetDataCache {
    var teams: List<Team>? = null
    var localMatches: List<Match>? = null
    var venues: List<Venue>? = null
}

class DefaultDataRepository(private val context: Context) : DataRepository {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val matchCacheFile: File
        get() = File(context.filesDir, "match_results_cache.json")

    private suspend fun readAsset(filename: String): String = withContext(Dispatchers.IO) {
        context.assets.open("data/$filename").bufferedReader().use { it.readText() }
            .removePrefix("﻿")  // strip UTF-8 BOM written by PowerShell 5.1
    }

    override suspend fun getTeams(): List<Team> {
        AssetDataCache.teams?.let { return it }
        return json.decodeFromString<List<Team>>(readAsset("teams.json")).also { AssetDataCache.teams = it }
    }

    private suspend fun getLocalMatches(): List<Match> {
        AssetDataCache.localMatches?.let { return it }
        return json.decodeFromString<List<Match>>(readAsset("matches.json")).also { AssetDataCache.localMatches = it }
    }

    override suspend fun getMatches(): List<Match> {
        MatchSyncState.remoteMatches?.let { return it }
        val matches = getCachedMatches() ?: getLocalMatches()
        val teams = getTeams()
        val standings = getGroupStandings(teams, matches)
        return resolveKnockoutTeams(matches, teams, standings)
    }

    override suspend fun getVenues(): List<Venue> {
        AssetDataCache.venues?.let { return it }
        return json.decodeFromString<List<Venue>>(readAsset("venues.json")).also { AssetDataCache.venues = it }
    }

    override fun observeMatchVersion(): StateFlow<Long> = MatchSyncState.version

    override suspend fun refreshMatches(): Result<Unit> = runCatching {
        val url = BuildConfig.RESULTS_CSV_URL.trim()
        if (url.isEmpty() || url == "https://YOUR_VERCEL_PROJECT.vercel.app/data/results.csv") {
            return@runCatching
        }
        val csv = fetchText(url)
        val teams = getTeams()
        val baseMatches = getCachedMatches() ?: getLocalMatches()
        val parsed = applyResultsCsv(csv, baseMatches, teams)
        val standings = getGroupStandings(teams, parsed)
        val resolved = resolveKnockoutTeams(parsed, teams, standings)
        if (resolved != baseMatches) {
            saveCachedMatches(resolved)
            MatchSyncState.publish(resolved)
        } else if (MatchSyncState.remoteMatches == null) {
            MatchSyncState.publish(resolved)
        }
    }

    private suspend fun getCachedMatches(): List<Match>? = withContext(Dispatchers.IO) {
        val file = matchCacheFile
        if (!file.exists()) return@withContext null
        runCatching {
            json.decodeFromString<List<Match>>(file.readText(Charsets.UTF_8))
                .takeIf { it.isNotEmpty() }
        }.getOrNull()
    }?.let { cached ->
        val cachedById = cached.associateBy { it.id }
        getLocalMatches().map { base ->
            val cachedMatch = cachedById[base.id] ?: return@map base
            if (base.stage != "GS" && (isKnockoutPathLabel(base.team1Name) || isKnockoutPathLabel(base.team2Name))) {
                base.copy(
                    status = cachedMatch.status,
                    homeScore = cachedMatch.homeScore,
                    awayScore = cachedMatch.awayScore,
                    penaltyHomeScore = cachedMatch.penaltyHomeScore,
                    penaltyAwayScore = cachedMatch.penaltyAwayScore
                )
            } else {
                cachedMatch
            }
        }
    }

    private suspend fun saveCachedMatches(matches: List<Match>) = withContext(Dispatchers.IO) {
        matchCacheFile.writeText(json.encodeToString(matches), Charsets.UTF_8)
    }

    private suspend fun fetchText(url: String): String = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            requestMethod = "GET"
            setRequestProperty("Accept", "text/csv,text/plain,*/*")
        }
        try {
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                throw IllegalStateException("HTTP ${connection.responseCode}")
            }
            stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                .removePrefix("\uFEFF")
        } finally {
            connection.disconnect()
        }
    }

    private var rosterCache: Map<String, List<RosterPlayer>>? = null
    private var rosterImageCache: Map<String, List<RosterPlayer>>? = null
    private val rosterMutex = Mutex()
    private val rosterImageMutex = Mutex()

    override suspend fun getRoster(countryName: String): List<RosterPlayer> {
        // Normalize team.name (e.g. "USA") to roster key (e.g. "United States")
        val key = normalizeCountry(countryName)
        val cache = rosterCache
        if (cache != null) return cache[key] ?: emptyList()
        return rosterMutex.withLock {
            val lockedCache = rosterCache
            if (lockedCache != null) {
                lockedCache[key] ?: emptyList()
            } else {
                val loaded = parseRoster(includeImages = true)
                rosterCache = loaded
                loaded[key] ?: emptyList()
            }
        }
    }

    override suspend fun getRosterImages(countryName: String): List<RosterPlayer> {
        val key = normalizeCountry(countryName)
        val cache = rosterImageCache
        if (cache != null) return cache[key] ?: emptyList()
        return rosterImageMutex.withLock {
            val lockedCache = rosterImageCache
            if (lockedCache != null) {
                lockedCache[key] ?: emptyList()
            } else {
                val loaded = parseRoster(includeImages = true)
                rosterImageCache = loaded
                loaded[key] ?: emptyList()
            }
        }
    }

    // Normalize a name for fuzzy matching: strip replacement chars + diacritics → ASCII lowercase
    private fun String.normForMatch(): String = try {
        val stripped = this.replace("�", "")
        java.text.Normalizer.normalize(stripped, java.text.Normalizer.Form.NFD)
            .replace(Regex("[^a-zA-Z\\s]"), "")
            .lowercase().trim()
    } catch (_: Exception) { this.lowercase().trim() }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            dp[i][j] = if (a[i-1] == b[j-1]) dp[i-1][j-1]
            else 1 + minOf(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
        }
        return dp[a.length][b.length]
    }

    private val playerDataByNorm: Map<String, Player> by lazy {
        PlayerData.all.associateBy { it.full.normForMatch() }
    }

    private val playerAssetFiles: Set<String> by lazy {
        try { context.assets.list("pic/player")?.toSet() ?: emptySet() }
        catch (_: Exception) { emptySet() }
    }

    private val clubAssetFiles: Set<String> by lazy {
        try { context.assets.list("pic/club")?.toSet() ?: emptySet() }
        catch (_: Exception) { emptySet() }
    }

    private val playerAssetByNorm: Map<String, String> by lazy {
        playerAssetFiles.filter { it.endsWith(".png", ignoreCase = true) }.associate { file ->
            val base = file.removeSuffix(".png")
            val namePart = if ("_" in base && !base.startsWith("fm_", ignoreCase = true)) {
                base.substringAfter("_")
            } else {
                base.dropWhile { it.isDigit() }
            }
            namePart.normForMatch() to "pic/player/$file"
        }
    }

    // Extended player data from player.csv
    private data class PlayerCsvEntry(
        val csvId: Int, val ovr: Int,
        val pac: Int, val sho: Int, val pas: Int, val dri: Int, val def: Int, val phy: Int,
        val detailedPos: String, val height: String, val weight: String, val foot: String,
        val country: String, val avatarFile: String,
        val clubName: String, val clubId: String, val clubLogoFile: String
    )

    private val manualPlayerCsvByNorm: Map<String, PlayerCsvEntry> by lazy {
        listOf(
            manualPlayer("Nicolás González", 14181375, 78, 82, 75, 72, 79, 48, 72, "AMR", "180 cm", "72 kg", "L", "Argentina", "pic/player/1164_Nico Gonzalez.png", "Atlético Madrid"),
            manualPlayer("Nicolas Gonzalez", 14181375, 78, 82, 75, 72, 79, 48, 72, "AMR", "180 cm", "72 kg", "L", "Argentina", "pic/player/1164_Nico Gonzalez.png", "Atlético Madrid"),
            manualPlayer("Rayan", 2000218544, 66, 78, 70, 55, 65, 35, 64, "RW", "185 cm", "78 kg", "L", "Brazil", "https://img.fminside.net/facesfm26/2000218544.png", "Bournemouth"),
            manualPlayer("Mamadou Sarr", 2000107103, 66, 60, 30, 65, 40, 70, 80, "CB", "194 cm", "83 kg", "R", "Senegal", "pic/player/1905_Mamadou Sarr.png", "Chelsea"),
            manualPlayer("Ayyoub Bouaddi", 2000381365, 65, 66, 40, 75, 65, 70, 68, "CM", "185 cm", "75 kg", "R", "Morocco", "", "Lille"),
            manualPlayer("Issa Diop", 204884, 75, 51, 35, 57, 53, 76, 78, "CB", "194 cm", "92 kg", "R", "Morocco", "", "Fulham"),
            manualPlayer("Munir Mohamedi", 190584, 72, 70, 70, 68, 72, 35, 70, "GK", "190 cm", "89 kg", "R", "Morocco", "", "RS Berkane"),
            manualPlayer("Ahmed Reda Tagnaouti", 240771, 68, 67, 66, 64, 68, 32, 67, "GK", "192 cm", "82 kg", "R", "Morocco", "", "AS FAR"),
            manualPlayer("Gessime Yassine", 2000301518, 63, 72, 63, 58, 67, 30, 54, "RW", "178 cm", "70 kg", "R", "Morocco", "", "Strasbourg")
        ).associateBy { it.first.normForMatch() }.mapValues { it.value.second }
    }

    private fun manualPlayer(
        name: String,
        id: Int,
        ovr: Int,
        pac: Int,
        sho: Int,
        pas: Int,
        dri: Int,
        def: Int,
        phy: Int,
        detailedPos: String,
        height: String,
        weight: String,
        foot: String,
        country: String,
        avatarFile: String,
        clubName: String
    ): Pair<String, PlayerCsvEntry> = name to PlayerCsvEntry(
        csvId = id,
        ovr = ovr,
        pac = pac,
        sho = sho,
        pas = pas,
        dri = dri,
        def = def,
        phy = phy,
        detailedPos = detailedPos,
        height = height,
        weight = weight,
        foot = foot,
        country = country,
        avatarFile = avatarFile,
        clubName = clubName,
        clubId = "",
        clubLogoFile = clubLogoFile("", clubName)
    )

    private val playerCsvByNorm: Map<String, PlayerCsvEntry> by lazy {
        val map = mutableMapOf<String, PlayerCsvEntry>()
        try {
            val lines = context.assets.open("data/player.csv")
                .bufferedReader(Charsets.UTF_8).readLines()
            // Player_ID,Player_Name,Player_URL,Country_Name,Country_ID,Country_URL,
            //   Club_Name,Club_ID,Club_URL,Position,OVR,PAC,SHO,PAS,DRI,DEF,PHY,
            //   Prefer_Foot,Height,KG,Club_Level
            for (line in lines.drop(1)) {
                if (line.isBlank()) continue
                val parts = line.split(",")
                if (parts.size < 17) continue
                val rawId = parts[0].trim()
                val id   = stablePlayerId(rawId, map.size)
                val name = parts[1].trim()
                val playerUrl = parts[2].trim()
                val country = parts[3].trim()
                val clubName = if (parts.size > 6) parts[6].trim() else ""
                val clubId = if (parts.size > 7) parts[7].trim() else ""
                val clubUrl = if (parts.size > 8) parts[8].trim() else ""
                val dpos = parts[9].trim()
                val ovr  = parts[10].trim().toIntOrNull() ?: 0
                val pac  = parts[11].trim().toIntOrNull() ?: 0
                val sho  = parts[12].trim().toIntOrNull() ?: 0
                val pas  = parts[13].trim().toIntOrNull() ?: 0
                val dri  = parts[14].trim().toIntOrNull() ?: 0
                val def  = parts[15].trim().toIntOrNull() ?: 0
                val phy  = parts[16].trim().toIntOrNull() ?: 0
                val foot = if (parts.size > 17) parts[17].trim() else "R"
                val heightCm = if (parts.size > 18) parts[18].trim() else ""
                val kg   = if (parts.size > 19) parts[19].trim() else ""
                if (name.isEmpty() || ovr == 0) continue
                val norm = name.normForMatch()
                val avatarFile = playerAvatarFile(rawId, name).ifEmpty { normalizeUrl(playerUrl) }
                val clubLogoFile = clubLogoFile(clubId, clubName).ifEmpty { normalizeUrl(clubUrl) }
                map[norm] = PlayerCsvEntry(
                    csvId = id, ovr = ovr, pac = pac, sho = sho, pas = pas,
                    dri = dri, def = def, phy = phy, detailedPos = dpos,
                    height = if (heightCm.isNotEmpty()) "$heightCm cm" else "",
                    weight = if (kg.isNotEmpty()) "$kg kg" else "",
                    foot = foot.take(1), country = country, avatarFile = avatarFile,
                    clubName = clubName, clubId = clubId, clubLogoFile = clubLogoFile
                )
            }
        } catch (_: Exception) {}
        map
    }

    // Name→photo for key players whose photos are in app assets
    private fun stablePlayerId(rawId: String, index: Int): Int {
        rawId.toIntOrNull()?.let { return it }
        val hash = rawId.hashCode() and 0x7fffffff
        return if (hash != 0) hash else 1_000_000 + index
    }

    private fun playerAvatarFile(rawId: String, name: String): String {
        val safeName = name.replace("/", "")
        val candidates = listOf(
            "$rawId.png",
            "${rawId}_${safeName}.png",
            "${rawId}$safeName.png",
            "$safeName.png"
        ).filter { it.isNotBlank() && it != ".png" }
        candidates.firstOrNull { playerAssetFiles.contains(it) }?.let { return "pic/player/$it" }

        val normName = name.normForMatch()
        val byName = playerAssetFiles.firstOrNull { file ->
            val base = file.removeSuffix(".png")
            val namePart = if ("_" in base && !base.startsWith("fm_", ignoreCase = true)) {
                base.substringAfter("_")
            } else {
                base.dropWhile { it.isDigit() }
            }
            namePart.normForMatch() == normName
        }
        return byName?.let { "pic/player/$it" }.orEmpty()
    }

    private fun clubLogoFile(clubId: String, clubName: String): String {
        val safeClub = clubName.replace("/", "")
        val candidates = listOf(
            "$clubId$safeClub.png",
            "$clubId.png",
            "$safeClub.png"
        ).filter { it.isNotBlank() && it != ".png" }
        candidates.firstOrNull { clubAssetFiles.contains(it) }?.let { return it }

        val normClub = clubName.normForMatch()
        return clubAssetFiles.firstOrNull { file ->
            val base = file.removeSuffix(".png")
            base.dropWhile { it.isDigit() }.normForMatch() == normClub || base.normForMatch() == normClub
        }.orEmpty()
    }

    private fun normalizeUrl(url: String): String = when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("http://") || url.startsWith("https://") -> url
        else -> ""
    }

    private val extraPhotos: Map<String, String> = mapOf(
        // Tab3 previously added
        "achraf hakimi"           to "pic/player/12_Achraf Hakimi.png",
        "virgil van dijk"         to "pic/player/8_Virgil van Dijk.png",
        "granit xhaka"            to "pic/player/144_Granit Xhaka.png",
        "alphonso davies"         to "pic/player/170_Alphonso Davies.png",
        "edin dzeko"              to "pic/player/571_Edin Džeko.png",
        "edin džeko"              to "pic/player/571_Edin Džeko.png",
        "hakan calhanoglu"        to "pic/player/93_Hakan Çalhanoğlu.png",
        "hakan calhanoğlu"        to "pic/player/93_Hakan Çalhanoğlu.png",
        "raphinha"                to "pic/player/11_Raphinha.png",
        "rayan"                   to "https://r2.thesportsdb.com/images/media/player/thumb/sipwl81771748374.jpg/medium",
        "rodri"                   to "pic/player/6_Rodri.png",
        "lamine yamal"            to "pic/player/14_Lamine Yamal.png",
        "vitinha"                 to "pic/player/15_Vitinha.png",
        "bruno fernandes"         to "pic/player/63_Bruno Fernandes.png",
        "son heung-min"           to "pic/player/200104Son Heung-min.png",
        "son heungmin"            to "pic/player/200104Son Heung-min.png",
        "andy robertson"          to "pic/player/216267Andy Robertson.png",
        "andrew robertson"        to "pic/player/216267Andy Robertson.png",
        // France squad
        "nicolas gonzalez"         to "pic/player/Nicolas Gonzalez.png",
        "nicolas gonz谩lez"         to "pic/player/Nicolas Gonzalez.png",
        "nico gonzalez"            to "pic/player/1164_Nico Gonzalez.png",
        "jose manuel lopez"        to "pic/player/Jose Manuel Lopez.png",
        "jos茅 manuel l贸pez"        to "pic/player/Jose Manuel Lopez.png",
        "mike maignan"            to "pic/player/70_Mike Maignan.png",
        "william saliba"          to "pic/player/64_William Saliba.png",
        "marcus thuram"           to "pic/player/127_Marcus Thuram.png",
        "dayot upamecano"         to "pic/player/154_Dayot Upamecano.png",
        "adrien rabiot"           to "pic/player/234_Adrien Rabiot.png",
        "eduardo camavinga"       to "pic/player/235_Eduardo Camavinga.png",
        "kingsley coman"          to "pic/player/301_Kingsley Coman.png",
        "khephren thuram"         to "pic/player/450_Khéphren Thuram.png",
        "khéphren thuram"         to "pic/player/450_Khéphren Thuram.png",
        "youssouf fofana"         to "pic/player/498_Youssouf Fofana.png",
        "brice samba"             to "pic/player/605_Brice Samba.png",
        "lucas digne"             to "pic/player/625_Lucas Digne.png",
        "malo gusto"              to "pic/player/850_Malo Gusto.png",
        "wesley fofana"           to "pic/player/929_Wesley Fofana.png",
        "ousmane dembele"         to "pic/player/5_Ousmane Dembélé.png",
        "ousmane dembélé"         to "pic/player/5_Ousmane Dembélé.png"
    )

    private fun findPlayer(csvName: String): Player? {
        PlayerData.byFullName[csvName]?.let { return it }
        val normCsv = csvName.normForMatch()
        if (normCsv.length < 4) return null
        return playerDataByNorm.entries
            .filter { (normFull, _) -> levenshtein(normCsv, normFull) <= 3 }
            .minByOrNull { (normFull, _) -> levenshtein(normCsv, normFull) }
            ?.value
    }

    private fun findPlayerExact(csvName: String): Player? {
        PlayerData.byFullName[csvName]?.let { return it }
        return playerDataByNorm[csvName.normForMatch()]
    }

    private fun findPlayerCsv(name: String): PlayerCsvEntry? {
        val norm = name.normForMatch()
        return manualPlayerCsvByNorm[norm]
            ?: playerCsvByNorm[norm]
            ?: playerCsvByNorm.entries
            .filter { (k, _) -> levenshtein(norm, k) <= 3 }
            .minByOrNull { (k, _) -> levenshtein(norm, k) }
            ?.value
    }

    private fun findPlayerCsvExact(name: String): PlayerCsvEntry? {
        val norm = name.normForMatch()
        return manualPlayerCsvByNorm[norm] ?: playerCsvByNorm[norm]
    }

    private fun findOvrAndAvatar(name: String): Pair<Int, String> {
        val norm = name.normForMatch()
        val extraPhoto = extraPhotos[norm] ?: extraPhotos.entries
            .firstOrNull { (k, _) -> levenshtein(norm, k) <= 2 }?.value ?: ""
        val csvEntry = findPlayerCsv(name)
        val ovr = csvEntry?.ovr ?: 0
        val avatar = if (extraPhoto.isNotEmpty()) {
            extraPhoto
        } else {
            csvEntry?.avatarFile.orEmpty().ifEmpty { playerAssetByNorm[norm].orEmpty() }
        }
        return ovr to avatar
    }

    private fun estimatedPlayerStats(rp: RosterPlayer, countryName: String): PlayerCsvEntry {
        val teamStats = teamOpta(countryName)
        val posOffset = when (rp.pos) {
            "GK" -> -7
            "DF" -> -6
            "MF" -> -5
            "FW" -> -5
            else -> -6
        }
        val ovr = (rp.ovr.takeIf { it > 0 } ?: (teamStats.ovr + posOffset)).coerceIn(55, 84)
        val pos = when (rp.pos) {
            "GK" -> "GK"
            "DF" -> "CB"
            "MF" -> "CM"
            "FW" -> "ST"
            else -> rp.pos.ifEmpty { "CM" }
        }
        val pac = when (rp.pos) {
            "GK" -> teamStats.pac.coerceAtMost(70)
            "DF" -> teamStats.pac
            "MF" -> teamStats.pac
            "FW" -> (teamStats.pac + 3).coerceAtMost(90)
            else -> teamStats.pac
        }.coerceIn(45, 90)
        val sho = when (rp.pos) {
            "GK" -> teamStats.sho.coerceAtMost(60)
            "DF" -> (teamStats.sho - 6)
            "MF" -> teamStats.sho
            "FW" -> (teamStats.sho + 5)
            else -> teamStats.sho
        }.coerceIn(35, 90)
        val pas = when (rp.pos) {
            "GK" -> teamStats.pas.coerceAtMost(70)
            "DF" -> (teamStats.pas - 2)
            "MF" -> (teamStats.pas + 3)
            "FW" -> teamStats.pas
            else -> teamStats.pas
        }.coerceIn(45, 90)
        val dri = when (rp.pos) {
            "GK" -> teamStats.dri.coerceAtMost(70)
            "DF" -> (teamStats.dri - 4)
            "MF" -> (teamStats.dri + 2)
            "FW" -> (teamStats.dri + 3)
            else -> teamStats.dri
        }.coerceIn(45, 90)
        val def = when (rp.pos) {
            "GK" -> teamStats.def.coerceAtMost(50)
            "DF" -> (teamStats.def + 8)
            "MF" -> teamStats.def
            "FW" -> (teamStats.def - 10)
            else -> teamStats.def
        }.coerceIn(25, 90)
        val phy = when (rp.pos) {
            "GK" -> teamStats.phy
            "DF" -> (teamStats.phy + 3)
            "MF" -> teamStats.phy
            "FW" -> (teamStats.phy + 1)
            else -> teamStats.phy
        }.coerceIn(45, 90)

        return PlayerCsvEntry(
            csvId = stablePlayerId("${countryName}:${rp.name}", rp.name.length),
            ovr = ovr,
            pac = pac,
            sho = sho,
            pas = pas,
            dri = dri,
            def = def,
            phy = phy,
            detailedPos = pos,
            height = "",
            weight = "",
            foot = "R",
            country = countryName,
            avatarFile = playerAvatarFile("", rp.name).ifEmpty { playerAssetByNorm[rp.name.normForMatch()].orEmpty() },
            clubName = rp.club,
            clubId = "",
            clubLogoFile = rp.clubLogoFile
        )
    }

    private fun fallbackRosterOvr(countryName: String, pos: String): Int {
        val base = teamOpta(countryName).ovr
        val offset = when (pos) {
            "GK" -> -7
            "DF" -> -6
            "MF" -> -5
            "FW" -> -5
            else -> -6
        }
        return (base + offset).coerceIn(55, 84)
    }

    /** Build a full Player from roster + player.csv for navigation to player detail. */
    override fun buildPlayerForNav(rp: RosterPlayer, countryName: String): Player? {
        val norm = rp.name.normForMatch()
        val entry = manualPlayerCsvByNorm[norm]
            ?: playerCsvByNorm[norm]
            ?: estimatedPlayerStats(rp, countryName)

        val flagFile = "$countryName.png"
        val photo = extraPhotos[norm].orEmpty()

        val detPos = entry.detailedPos.uppercase()
        val pos = when {
            detPos == "GK" -> "GK"
            detPos in listOf("CB","LB","RB","LWB","RWB") -> "DF"
            detPos in listOf("CDM","CM","CAM","LM","RM") -> "MF"
            detPos in listOf("LW","RW","ST","CF","LF","RF","FW") -> "FW"
            else -> rp.pos
        }
        // Abbreviated name: "Brice Samba" → "B. Samba"
        val parts = rp.name.trim().split(" ")
        val shortName = if (parts.size >= 2)
            "${parts.first().first()}. ${parts.drop(1).joinToString(" ")}"
        else rp.name

        return Player(
            id = entry.csvId,
            name = shortName,
            full = rp.name,
            country = countryName,
            flagFile = flagFile,
            cc = teamCode(countryName),
            club = rp.club.ifEmpty { entry.clubName },
            clubCC = rp.clubCountry,
            pos = pos,
            age = rp.age,
            ovr = entry.ovr.takeIf { it > 0 } ?: fallbackRosterOvr(countryName, rp.pos),
            spd = entry.pac,
            atk = entry.sho,
            pas = entry.pas,
            dri = entry.dri,
            def = entry.def,
            phy = entry.phy,
            valueMEUR = rp.valueMEUR,
            height = entry.height,
            weight = entry.weight,
            foot = entry.foot.ifEmpty { "R" },
            caps = rp.caps,
            cGoals = rp.goals,
            cAssists = 0,
            avatarFile = photo.ifEmpty { entry.avatarFile.ifEmpty { rp.avatarFile } },
            clubLogoFile = clubLogoFile("", rp.club).ifEmpty { entry.clubLogoFile },
            detailedPos = entry.detailedPos
        )
    }

    // Normalize country name from roster.csv → teams.json team name
    private fun normalizeCountry(raw: String): String = when (raw.trim()) {
        "Czech Republic", "Czechia"          -> "Czech"
        "Bosnia and Herzegovina",
        "Bosnia & Herzegovina"               -> "Bosnia & Herzegovina"
        "Turkey"                             -> "Türkiye"
        // team.json uses "USA"; roster.csv uses "United States" → stored as "United States"
        "United States", "USA"               -> "United States"
        "Ivory Coast", "Côte d'Ivoire"       -> "Ivory Coast"
        "DR Congo", "Congo DR"               -> "DR Congo"
        "Cape Verde"                         -> "Cape Verde"
        "New Zealand"                        -> "New Zealand"
        "Saudi Arabia"                       -> "Saudi Arabia"
        "South Africa"                       -> "South Africa"
        "South Korea", "Korea Republic"      -> "South Korea"
        else -> raw.trim()
    }

    private suspend fun parseRoster(includeImages: Boolean): Map<String, List<RosterPlayer>> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, MutableList<RosterPlayer>>()
        try {
            val lines = context.assets.open("data/roster.csv")
                .bufferedReader(Charsets.UTF_8).readLines()
            if (lines.isEmpty()) return@withContext result
            val header = parseCsvLine(lines.first()).map { normalizeHeader(it.removePrefix("\uFEFF")) }
            // header: National Team,No,Pos,Player,Captain,Age,Caps,Goals,Value,Club,Club Country,...
            for (line in lines.drop(1)) {
                if (line.isBlank()) continue
                val parts = parseCsvLine(line)
                val row = header.mapIndexed { index, key -> key to parts.getOrElse(index) { "" } }.toMap()
                val country = normalizeCountry(row.value("National Team", "country"))
                val jerseyNo = row.value("No", "number").toIntOrNull()
                val pos = row.value("Pos", "position").uppercase()
                val playerName = row.value("Player", "name")
                val isCaptain = row.value("Captain").equals("Yes", ignoreCase = true)
                val age = row.value("Age").toIntOrNull() ?: 0
                val caps = row.value("Caps").toIntOrNull() ?: 0
                val goals = row.value("Goals").toIntOrNull() ?: 0
                val club = row.value("Club")
                val clubCountry = row.value("Club Country", "club_country", "clubcountry")
                val rosterValue = parseMarketValue(row.value("Value", "value", "ValueMEUR", "valueMEUR", "Market Value", "market_value"))
                if (country.isEmpty() || playerName.isEmpty()) continue
                val normPos = when {
                    pos == "GK" -> "GK"
                    pos in listOf("DF","CB","LB","RB","LWB","RWB") -> "DF"
                    pos in listOf("MF","CM","CDM","CAM","LM","RM") -> "MF"
                    pos in listOf("FW","ST","LW","RW","CF")        -> "FW"
                    else -> pos.take(2)
                }
                val normName = if (includeImages) playerName.normForMatch() else ""
                val knownPlayer = if (includeImages) findPlayerExact(playerName) else null
                val csvEntry = if (includeImages) findPlayerCsvExact(playerName) else null
                val assetAvatar = if (includeImages) playerAssetByNorm[normName].orEmpty() else ""
                val fallbackAvatar = if (includeImages) extraPhotos[normName].orEmpty() else ""
                val estimatedOvr = fallbackRosterOvr(country, normPos)
                val rp = RosterPlayer(
                    name = knownPlayer?.full ?: playerName,
                    pos = normPos,
                    age = age,
                    caps = caps,
                    goals = goals,
                    club = club,
                    clubCountry = clubCountry,
                    ovr = knownPlayer?.ovr ?: csvEntry?.ovr ?: estimatedOvr,
                    valueMEUR = rosterValue,
                    avatarFile = knownPlayer?.avatarFile?.ifEmpty {
                        fallbackAvatar.ifEmpty { csvEntry?.avatarFile.orEmpty().ifEmpty { assetAvatar } }
                    } ?: fallbackAvatar.ifEmpty { csvEntry?.avatarFile.orEmpty().ifEmpty { assetAvatar } },
                    clubLogoFile = csvEntry?.clubLogoFile.orEmpty(),
                    playerId = csvEntry?.csvId ?: knownPlayer?.id ?: 0,
                    number = jerseyNo,
                    captain = isCaptain
                )
                result.getOrPut(country) { mutableListOf() }.add(rp)
            }
        } catch (_: Exception) {}
        result
    }

    private fun applyResultsCsv(
        csv: String,
        baseMatches: List<Match>,
        teams: List<Team>
    ): List<Match> {
        val rows = csv.lineSequence()
            .filter { it.isNotBlank() }
            .map { parseCsvLine(it) }
            .toList()
        if (rows.size < 2) return baseMatches

        val header = rows.first().map { normalizeHeader(it) }
        val baseById = baseMatches.associateBy { it.id }
        val parsedById = rows.drop(1).mapNotNull { row ->
            val cells = header.mapIndexed { index, key -> key to row.getOrElse(index) { "" }.trim() }.toMap()
            val id = cells.value("match_id", "matchid", "id").ifEmpty { return@mapNotNull null }
            val base = baseById[id] ?: return@mapNotNull null
            val score1 = cells.intValue("score1")
            val score2 = cells.intValue("score2")
            val pen1 = cells.intValue("penalty1")
            val pen2 = cells.intValue("penalty2")
            val team1Name = cells.value("team1")
            val team2Name = cells.value("team2")
            val team1 = findTeam(team1Name, teams)
            val team2 = findTeam(team2Name, teams)
            val keepBaseTeam1 = base.stage != "GS" && isKnockoutPathLabel(base.team1Name)
            val keepBaseTeam2 = base.stage != "GS" && isKnockoutPathLabel(base.team2Name)
            base.copy(
                team1Id = if (keepBaseTeam1) base.team1Id else team1?.id ?: base.team1Id,
                team1Name = if (keepBaseTeam1) base.team1Name else team1?.name ?: team1Name.ifEmpty { base.team1Name },
                team2Id = if (keepBaseTeam2) base.team2Id else team2?.id ?: base.team2Id,
                team2Name = if (keepBaseTeam2) base.team2Name else team2?.name ?: team2Name.ifEmpty { base.team2Name },
                status = matchStatus(score1, score2),
                homeScore = score1,
                awayScore = score2,
                penaltyHomeScore = pen1,
                penaltyAwayScore = pen2
            )
        }.associateBy { it.id }

        return baseMatches.map { parsedById[it.id] ?: it }
    }

    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> {
                    cell.append('"')
                    i++
                }
                c == '"' -> quoted = !quoted
                c == ',' && !quoted -> {
                    out.add(cell.toString())
                    cell.clear()
                }
                else -> cell.append(c)
            }
            i++
        }
        out.add(cell.toString())
        return out
    }

    private fun normalizeHeader(value: String): String =
        value.removePrefix("\uFEFF").trim().lowercase(Locale.US).replace(Regex("[\\s_()\\-]"), "")

    private fun parseMarketValue(raw: String): Int =
        raw.replace(Regex("[€$£,]"), "")
            .replace("M", "", ignoreCase = true)
            .trim()
            .toDoubleOrNull()
            ?.toInt() ?: 0

    private fun Map<String, String>.value(vararg keys: String): String {
        for (key in keys) {
            this[normalizeHeader(key)]?.let { return it }
        }
        return ""
    }

    private fun Map<String, String>.intValue(vararg keys: String): Int? =
        value(*keys).takeIf { it.isNotBlank() && it != "-" && it != "—" }?.toIntOrNull()

    private fun matchStatus(score1: Int?, score2: Int?): String {
        return if (score1 != null && score2 != null) "ft" else "upcoming"
    }

    private fun String.normKey(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace("&", "and")
        .replace(Regex("[^a-zA-Z0-9]+"), "")
        .lowercase(Locale.US)

    private fun findTeam(label: String, teams: List<Team>): Team? {
        val key = label.normKey()
        if (key.isBlank()) return null
        val aliases = mapOf(
            "usa" to "unitedstates",
            "unitedstates" to "usa",
            "czechia" to "czech",
            "czechrepublic" to "czech",
            "turkiye" to "turkiye",
            "türkiye".normKey() to "turkiye",
            "curacao" to "curacao",
            "curaçao".normKey() to "curacao",
            "bosniaandherzegovina" to "bosniaandherzegovina",
            "drcongo" to "drcongo",
            "congodr" to "drcongo"
        )
        return teams.firstOrNull { it.name.normKey() == key || it.id.normKey() == key }
            ?: aliases[key]?.let { alias -> teams.firstOrNull { it.name.normKey() == alias } }
    }

    private fun resolveKnockoutTeams(
        matches: List<Match>,
        teams: List<Team>,
        standings: Map<String, List<TeamStanding>>
    ): List<Match> {
        val completedGroups = matches
            .filter { it.stage == "GS" && it.group.length == 1 }
            .groupBy { it.group }
            .filterValues { groupMatches -> groupMatches.isNotEmpty() && groupMatches.all { it.homeScore != null && it.awayScore != null } }
            .keys
        val allGroupsComplete = standings.keys.all { it in completedGroups }
        val thirdSeedPool = standings.values.mapNotNull { group ->
            group.getOrNull(2)?.takeIf { allGroupsComplete }
        }.sortedWith(
            compareByDescending<TeamStanding> { it.points }
                .thenByDescending { it.goalDiff }
                .thenByDescending { it.goalsFor }
                .thenBy { it.team.fifaRank }
        )
        val usedThirds = mutableSetOf<String>()
        val resolved = mutableMapOf<String, Match>()

        return matches.map { match ->
            if (match.stage == "GS") {
                resolved[match.id] = match
                match
            } else {
                val team1 = resolvePath(match.team1Name, standings, completedGroups, thirdSeedPool, usedThirds, resolved)
                    ?: findTeam(match.team1Name, teams)
                val team2 = resolvePath(match.team2Name, standings, completedGroups, thirdSeedPool, usedThirds, resolved)
                    ?: findTeam(match.team2Name, teams)
                val updated = match.copy(
                    team1Id = team1?.id ?: match.team1Id,
                    team1Name = team1?.name ?: match.team1Name,
                    team2Id = team2?.id ?: match.team2Id,
                    team2Name = team2?.name ?: match.team2Name
                )
                resolved[updated.id] = updated
                updated
            }
        }
    }

    private fun resolvePath(
        label: String,
        standings: Map<String, List<TeamStanding>>,
        completedGroups: Set<String>,
        thirdSeedPool: List<TeamStanding>,
        usedThirds: MutableSet<String>,
        resolvedMatches: Map<String, Match>
    ): Team? {
        val text = label.trim()
        Regex("(?i)winner\\s+group\\s+([A-L])").find(text)?.groupValues?.getOrNull(1)?.let { group ->
            if (group.uppercase() !in completedGroups) return null
            return standings[group.uppercase()]?.getOrNull(0)?.team
        }
        Regex("(?i)runner[- ]up\\s+group\\s+([A-L])").find(text)?.groupValues?.getOrNull(1)?.let { group ->
            if (group.uppercase() !in completedGroups) return null
            return standings[group.uppercase()]?.getOrNull(1)?.team
        }
        Regex("(?i)^\\s*([1-4])([A-L])\\s*$").find(text)?.groupValues?.let { values ->
            val rank = values[1].toInt()
            val group = values[2].uppercase()
            if (group.uppercase() !in completedGroups) return null
            return standings[group]?.getOrNull(rank - 1)?.team
        }
        Regex("(?i)best\\s+3rd.*groups?\\s+(.+)").find(text)?.groupValues?.getOrNull(1)?.let { groupsText ->
            val allowed = Regex("[A-L]").findAll(groupsText.uppercase()).map { it.value }.toSet()
            return thirdSeedPool.firstOrNull { it.team.group in allowed && usedThirds.add(it.team.id) }?.team
        }
        Regex("(?i)winner\\s+of\\s+\\[?match\\s*(\\d+)\\]?").find(text)?.groupValues?.getOrNull(1)?.let { num ->
            return winnerOfMatch(resolvedMatches["M${num.padStart(3, '0')}"])
        }
        Regex("(?i)^\\s*winner\\s+M(\\d{3})\\s*$").find(text)?.groupValues?.getOrNull(1)?.let { num ->
            return winnerOfMatch(resolvedMatches["M$num"])
        }
        Regex("(?i)^\\s*loser\\s+M(\\d{3})\\s*$").find(text)?.groupValues?.getOrNull(1)?.let { num ->
            return loserOfMatch(resolvedMatches["M$num"])
        }
        return null
    }

    private fun isKnockoutPathLabel(label: String): Boolean {
        val text = label.trim()
        return Regex("(?i)^\\s*[1-4][A-L]\\s*$").matches(text) ||
            Regex("(?i)^(winner|loser)\\s+M\\d{3}$").matches(text) ||
            Regex("(?i)^(winner|loser)\\s+of\\s+\\[?match\\s*\\d+\\]?$").matches(text) ||
            Regex("(?i)^(winner|runner[- ]up)\\s+group\\s+[A-L]$").matches(text) ||
            Regex("(?i)^best\\s+3rd").containsMatchIn(text)
    }

    private fun winnerOfMatch(match: Match?): Team? {
        return resultTeam(match, winner = true)
    }

    private fun loserOfMatch(match: Match?): Team? {
        return resultTeam(match, winner = false)
    }

    private fun resultTeam(match: Match?, winner: Boolean): Team? {
        if (match == null || match.status != "ft") return null
        val hs = match.homeScore ?: return null
        val as_ = match.awayScore ?: return null
        val homeWins = when {
            hs > as_ -> true
            hs < as_ -> false
            match.penaltyHomeScore != null && match.penaltyAwayScore != null ->
                match.penaltyHomeScore > match.penaltyAwayScore
            else -> return null
        }
        val useHome = if (winner) homeWins else !homeWins
        val teamId = if (useHome) match.team1Id else match.team2Id
        val teamName = if (useHome) match.team1Name else match.team2Name
        return Team(
            id = teamId,
            name = teamName,
            group = "",
            confederation = "",
            fifaRank = 999,
            imageFile = "",
            flagFile = ""
        ).takeIf { it.id.isNotEmpty() && it.name.isNotEmpty() }
    }

    override fun getGroupStandings(
        teams: List<Team>,
        matches: List<Match>
    ): Map<String, List<TeamStanding>> {
        val groups = teams.map { it.group }.filter { it.length == 1 }.distinct().sorted()
        return groups.associateWith { group ->
            val groupTeams = teams.filter { it.group == group }
            val groupMatches = matches.filter { it.group == group && it.stage == "GS" }
            groupTeams.map { team ->
                val played = groupMatches.filter {
                    (it.team1Id == team.id || it.team2Id == team.id) && it.homeScore != null
                }
                var w = 0; var d = 0; var l = 0; var gf = 0; var ga = 0
                played.forEach { m ->
                    val hs = m.homeScore ?: 0; val as_ = m.awayScore ?: 0
                    val isHome = m.team1Id == team.id
                    val tg = if (isHome) hs else as_; val og = if (isHome) as_ else hs
                    gf += tg; ga += og
                    when { tg > og -> w++; tg == og -> d++; else -> l++ }
                }
                TeamStanding(team, played.size, w, d, l, gf, ga, w * 3 + d)
            }.sortedWith(
                compareByDescending<TeamStanding> { it.points }
                    .thenByDescending { it.goalDiff }
                    .thenByDescending { it.goalsFor }
                    .thenBy { it.team.fifaRank }
            )
        }
    }
}

