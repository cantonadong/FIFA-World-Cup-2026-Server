package com.carldong.fifa.worldcup2026.ui.players

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carldong.fifa.worldcup2026.data.Player
import com.carldong.fifa.worldcup2026.data.PlayerData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer

enum class PlayerSort { OVR, POS }

data class PlayersUiState(
    val searchQuery: String = "",
    val sort: PlayerSort = PlayerSort.OVR,
    val posFilter: String = "ALL",
    val selectedPlayer: Player? = null,
    val players: List<Player> = PlayerData.all,
    val isLoading: Boolean = false
)

class PlayersViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(PlayersUiState())
    val state: StateFlow<PlayersUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { loadCsvPlayers() }
            if (loaded.isNotEmpty()) {
                _state.update { current ->
                    val refreshedSelection = current.selectedPlayer?.let { selected ->
                        loaded.find { it.id == selected.id }
                            ?: loaded.find { it.full.normForMatch() == selected.full.normForMatch() }
                    } ?: current.selectedPlayer
                    current.copy(players = loaded, selectedPlayer = refreshedSelection, isLoading = false)
                }
            }
        }
    }

    private data class RosterStats(
        val age: Int = 0,
        val caps: Int = 0,
        val goals: Int = 0,
        val valueMEUR: Int = 0,
        val clubCountry: String = ""
    )

    fun setSearch(q: String) = _state.update { it.copy(searchQuery = q) }
    fun setSort(s: PlayerSort) = _state.update { it.copy(sort = s, posFilter = "ALL") }
    fun setPosFilter(p: String) = _state.update { it.copy(posFilter = p) }
    fun selectPlayer(p: Player?) = _state.update { it.copy(selectedPlayer = p) }

    fun selectPlayerById(id: Int) {
        val player = _state.value.players.find { it.id == id } ?: PlayerData.all.find { it.id == id }
        if (player != null) selectPlayer(player)
    }

    fun filteredPlayers(state: PlayersUiState): List<Player> {
        var list = state.players.ifEmpty { PlayerData.all }
        val q = state.searchQuery.trim().lowercase()
        if (q.isNotEmpty()) {
            list = list.filter { p ->
                p.name.lowercase().contains(q) || p.full.lowercase().contains(q) ||
                    p.country.lowercase().contains(q) || p.club.lowercase().contains(q) ||
                    p.pos.lowercase().contains(q)
            }
        }
        if (state.sort == PlayerSort.POS && state.posFilter != "ALL") {
            list = list.filter { it.pos == state.posFilter }
        }
        return list.sortedWith(compareByDescending<Player> { it.ovr }.thenBy { it.full })
    }

    private fun loadCsvPlayers(): List<Player> {
        val assets = getApplication<Application>().assets
        val playerFiles = try {
            assets.list("pic/player")?.toSet() ?: emptySet()
        } catch (_: Exception) {
            emptySet()
        }
        val clubFiles = try {
            assets.list("pic/club")?.toSet() ?: emptySet()
        } catch (_: Exception) {
            emptySet()
        }

        return try {
            val rosterStats = loadRosterStats()
            assets.open("data/player.csv").bufferedReader(Charsets.UTF_8).readLines()
                .drop(1)
                .mapIndexedNotNull { index, line -> parseCsvPlayer(index, line, playerFiles, clubFiles, rosterStats) }
                .distinctBy { it.id }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun loadRosterStats(): Map<String, RosterStats> {
        val assets = getApplication<Application>().assets
        return try {
            assets.open("data/roster.csv").bufferedReader(Charsets.UTF_8).readLines()
                .let { lines ->
                    if (lines.isEmpty()) return emptyMap()
                    val header = parseCsvLine(lines.first()).map { normalizeHeader(it) }
                    lines.drop(1).flatMap { line ->
                        if (line.isBlank()) return@flatMap emptyList()
                        val parts = parseCsvLine(line)
                        val row = header.mapIndexed { index, key -> key to parts.getOrElse(index) { "" } }.toMap()
                        val country = row.value("National Team", "country")
                        val name = row.value("Player", "name")
                        val age = row.value("Age").toIntOrNull() ?: return@flatMap emptyList()
                        val caps = row.value("Caps").toIntOrNull() ?: 0
                        val goals = row.value("Goals").toIntOrNull() ?: 0
                        val value = parseMarketValue(row.value("Value", "value", "Market Value", "market_value"))
                        val clubCountry = row.value("Club Country", "club_country", "clubcountry")
                        val stats = RosterStats(
                            age = age,
                            caps = caps,
                            goals = goals,
                            valueMEUR = value,
                            clubCountry = clubCountry
                        )
                        listOf(rosterStatsKey(name, country) to stats, rosterNameKey(name) to stats)
                    }
                }
                .toMap()
        } catch (_: Exception) {
            emptyMap()
        }
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
        value.removePrefix("\uFEFF").trim().lowercase().replace(Regex("[\\s_()\\-]"), "")

    private fun Map<String, String>.value(vararg keys: String): String {
        for (key in keys) {
            this[normalizeHeader(key)]?.let { return it.trim() }
        }
        return ""
    }

    private fun parseMarketValue(raw: String): Int =
        raw.replace(Regex("[€$£,]"), "")
            .replace("M", "", ignoreCase = true)
            .trim()
            .toDoubleOrNull()
            ?.toInt() ?: 0

    private fun parseCsvPlayer(
        index: Int,
        line: String,
        playerFiles: Set<String>,
        clubFiles: Set<String>,
        rosterStats: Map<String, RosterStats>
    ): Player? {
        if (line.isBlank()) return null
        val parts = line.split(",")
        if (parts.size < 17) return null

        val rawId = parts[0].trim()
        val fullName = parts[1].trim()
        val playerUrl = parts.getOrNull(2)?.trim().orEmpty()
        val country = parts.getOrNull(3)?.trim().orEmpty()
        val club = parts.getOrNull(6)?.trim().orEmpty()
        val clubId = parts.getOrNull(7)?.trim().orEmpty()
        val clubUrl = parts.getOrNull(8)?.trim().orEmpty()
        val detailedPos = parts.getOrNull(9)?.trim().orEmpty()
        val ovr = parts.getOrNull(10)?.trim()?.toIntOrNull() ?: return null
        val pac = parts.getOrNull(11)?.trim()?.toIntOrNull() ?: 0
        val sho = parts.getOrNull(12)?.trim()?.toIntOrNull() ?: 0
        val pas = parts.getOrNull(13)?.trim()?.toIntOrNull() ?: 0
        val dri = parts.getOrNull(14)?.trim()?.toIntOrNull() ?: 0
        val def = parts.getOrNull(15)?.trim()?.toIntOrNull() ?: 0
        val phy = parts.getOrNull(16)?.trim()?.toIntOrNull() ?: 0
        val foot = parts.getOrNull(17)?.trim()?.take(1)?.uppercase().orEmpty()
        val height = parts.getOrNull(18)?.trim().orEmpty()
        val weight = parts.getOrNull(19)?.trim().orEmpty()
        if (rawId.isEmpty() || fullName.isEmpty() || country.isEmpty()) return null
        val nationalStats = rosterStats[rosterStatsKey(fullName, country)]
            ?: rosterStats[rosterNameKey(fullName)]
            ?: RosterStats()

        return Player(
            id = stablePlayerId(rawId, index),
            name = shortName(fullName),
            full = fullName,
            country = country,
            flagFile = "$country.png",
            cc = country.take(2).lowercase(),
            club = club,
            clubCC = nationalStats.clubCountry,
            pos = positionGroup(detailedPos),
            age = nationalStats.age,
            ovr = ovr,
            spd = pac,
            atk = sho,
            pas = pas,
            dri = dri,
            def = def,
            phy = phy,
            valueMEUR = nationalStats.valueMEUR,
            height = if (height.isNotEmpty()) "$height cm" else "",
            weight = if (weight.isNotEmpty()) "$weight kg" else "",
            foot = foot.ifEmpty { "R" },
            caps = nationalStats.caps,
            cGoals = nationalStats.goals,
            cAssists = 0,
            avatarFile = playerAvatarFile(rawId, fullName, playerFiles).ifEmpty { normalizeUrl(playerUrl) },
            clubLogoFile = clubLogoFile(clubId, club, clubFiles).ifEmpty { normalizeUrl(clubUrl) },
            detailedPos = detailedPos
        )
    }

    private fun stablePlayerId(rawId: String, index: Int): Int {
        rawId.toIntOrNull()?.let { return it }
        val hash = rawId.hashCode() and 0x7fffffff
        return if (hash != 0) hash else 1_000_000 + index
    }

    private fun shortName(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return if (parts.size >= 2) "${parts.first().first()}. ${parts.drop(1).joinToString(" ")}" else name
    }

    private fun positionGroup(raw: String): String {
        val pos = raw.uppercase().substringBefore("(").trim()
        return when {
            pos == "GK" -> "GK"
            pos in listOf("CB", "LB", "RB", "LWB", "RWB", "DF") -> "DF"
            pos in listOf("CDM", "CM", "CAM", "LM", "RM", "MC", "AMC", "DMC", "MF") -> "MF"
            pos in listOf("LW", "RW", "ST", "CF", "LF", "RF", "FW", "AML", "AMR") -> "FW"
            pos.contains("GK") -> "GK"
            pos.contains("B") || pos.contains("D") -> "DF"
            pos.contains("M") -> "MF"
            pos.contains("W") || pos.contains("ST") || pos.contains("F") -> "FW"
            else -> raw.take(2).uppercase().ifEmpty { "MF" }
        }
    }

    private fun playerAvatarFile(rawId: String, name: String, files: Set<String>): String {
        val safeName = name.replace("/", "")
        val candidates = listOf(
            "$rawId.png",
            "${rawId}_${safeName}.png",
            "${rawId}$safeName.png",
            "$safeName.png"
        )
        candidates.firstOrNull { files.contains(it) }?.let { return "pic/player/$it" }

        val normName = name.normForMatch()
        val byName = files.firstOrNull { file ->
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

    private fun clubLogoFile(clubId: String, club: String, files: Set<String>): String {
        val safeClub = club.replace("/", "")
        val candidates = listOf(
            "$clubId$safeClub.png",
            "$clubId.png",
            "$safeClub.png"
        ).filter { it.isNotBlank() && it != ".png" }
        candidates.firstOrNull { files.contains(it) }?.let { return it }

        val normClub = club.normForMatch()
        return files.firstOrNull { file ->
            val base = file.removeSuffix(".png")
            base.dropWhile { it.isDigit() }.normForMatch() == normClub || base.normForMatch() == normClub
        }.orEmpty()
    }

    private fun normalizeUrl(url: String): String = when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("http://") || url.startsWith("https://") -> url
        else -> ""
    }

    private fun rosterStatsKey(name: String, country: String): String =
        "${name.normForMatch()}|${normalizeCountryName(country).normForMatch()}"

    private fun rosterNameKey(name: String): String =
        "name:${name.normForMatch()}"

    private fun normalizeCountryName(country: String): String = when (country.trim()) {
        "Czech Republic", "Czechia", "Czech" -> "Czech Republic"
        "USA", "United States" -> "United States"
        "Turkey", "Türkiye" -> "Turkey"
        "Ivory Coast", "Côte d'Ivoire" -> "Ivory Coast"
        "South Korea", "Korea Republic" -> "South Korea"
        else -> country.trim()
    }

    private fun String.normForMatch(): String = try {
        Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("[^a-zA-Z\\s]"), "")
            .lowercase()
            .trim()
    } catch (_: Exception) {
        lowercase().trim()
    }
}

