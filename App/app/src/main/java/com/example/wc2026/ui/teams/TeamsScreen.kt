package com.carldong.fifa.worldcup2026.ui.teams

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carldong.fifa.worldcup2026.data.*
import com.carldong.fifa.worldcup2026.theme.*
import com.carldong.fifa.worldcup2026.ui.components.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

// ─── Main Screen ──────────────────────────────────────────────────────────────

@Composable
fun TeamsScreen(
    pendingTeamId: String? = null,
    onPendingConsumed: () -> Unit = {},
    homeResetToken: Int = 0,
    onPlayerClick: (Int) -> Unit = {},
    onPlayerFullClick: (com.carldong.fifa.worldcup2026.data.Player) -> Unit = {},
    vm: TeamsViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var lastHomeResetToken by rememberSaveable { mutableIntStateOf(homeResetToken) }
    val teamsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    LaunchedEffect(homeResetToken) {
        if (homeResetToken != lastHomeResetToken) {
            lastHomeResetToken = homeResetToken
            vm.selectTeam(null)
        }
    }

    // Handle pending navigation from other tabs
    LaunchedEffect(pendingTeamId, state.isLoading) {
        if (!state.isLoading && pendingTeamId != null) {
            vm.selectTeamById(pendingTeamId)
            onPendingConsumed()
        }
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Blue)
        }
        return
    }
    state.error?.let { err ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: $err", color = Red)
        }
        return
    }

    val filtered = remember(state.teams, state.searchQuery, state.sort) {
        var list = state.teams
        val q = state.searchQuery.trim().lowercase()
        if (q.isNotEmpty()) {
            // Normalize diacritics for matching so "Turkey" matches "Türkiye", "Curacao" matches "Curaçao"
            fun String.norm(): String = try {
                java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
                    .replace(Regex("[^\\p{ASCII}]"), "").lowercase()
            } catch (_: Exception) { lowercase() }
            val qNorm = q.norm()
            list = list.filter { t ->
                t.name.norm().contains(qNorm) ||
                t.group.lowercase().contains(q) ||
                t.confederation.norm().contains(qNorm)
            }
        }
        list
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.fillMaxSize()) {
            TeamsNavBar()
            TeamsSearchBar(state.searchQuery, vm::setSearch)
            TeamsSortControl(state.sort, vm::setSort)
            HorizontalDivider(thickness = 0.5.dp, color = Separator)
            TeamsListContent(teams = filtered, sort = state.sort, listState = teamsListState, onTeamClick = vm::selectTeam)
        }

        AnimatedVisibility(
            visible = state.selectedTeam != null,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(260)
            )
        ) {
            state.selectedTeam?.let { team ->
                TeamDetail(
                    team = team,
                    roster = state.teamRoster,
                    isRosterLoading = state.isRosterLoading,
                    onClose = { vm.selectTeam(null) },
                    onPlayerClick = onPlayerClick,
                    onPlayerFullClick = onPlayerFullClick,
                    vm = vm
                )
            }
        }
    }

    if (state.selectedTeam != null) {
        BackHandler { vm.selectTeam(null) }
    }
}

// ─── NavBar ───────────────────────────────────────────────────────────────────

@Composable
private fun TeamsNavBar() {
    Surface(color = WCSurface, shadowElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "Teams",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    letterSpacing = (-0.4).sp,
                    color = Label1
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = Separator)
        }
    }
}

// ─── Search bar ───────────────────────────────────────────────────────────────

@Composable
private fun TeamsSearchBar(query: String, onQuery: (String) -> Unit) {
    Surface(color = WCSurface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x1F787880))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Text field + placeholder — weight(1f) takes remaining space
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        "Search teams…",
                        fontSize = 15.sp,
                        color = Label3,
                        lineHeight = 20.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { onQuery(it.replace("\n", "")) },
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = Label1,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(Blue),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    maxLines = 1
                )
            }
            // Clear button — only visible when query non-empty
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0x60787880))
                        .clickable { onQuery("") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─── Sort control (only RANK and NAME) ───────────────────────────────────────

@Composable
private fun TeamsSortControl(sort: TeamSort, onSort: (TeamSort) -> Unit) {
    Surface(color = WCSurface) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0x1F787880))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TeamSort.entries.forEach { s ->
                val isOn = sort == s
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (isOn) WCSurface else Color.Transparent)
                        .clickable { onSort(s) }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (s) {
                            TeamSort.RANK  -> "FIFA Rank"
                            TeamSort.GROUP -> "Group"
                            TeamSort.NAME  -> "Name"
                        },
                        fontSize = 13.sp,
                        fontWeight = if (isOn) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isOn) Label1 else Label2
                    )
                }
            }
        }
    }
}

// ─── Team list ────────────────────────────────────────────────────────────────

@Composable
private fun TeamsListContent(
    teams: List<Team>,
    sort: TeamSort,
    listState: LazyListState,
    onTeamClick: (Team) -> Unit
) {
    when (sort) {
        TeamSort.RANK -> {
            val ranked = remember(teams) { teams.sortedBy { it.fifaRank } }
            val tiers = remember(ranked) {
                listOf(
                    "Top 16"         to ranked.filter { it.fifaRank <= 16 },
                    "Ranked 17 – 40" to ranked.filter { it.fifaRank in 17..40 },
                    "Ranked 41 – 80" to ranked.filter { it.fifaRank in 41..80 },
                    "Ranked 81+"     to ranked.filter { it.fifaRank > 80 }
                ).filter { it.second.isNotEmpty() }
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                tiers.forEach { (label, list) ->
                    item(key = "hd_$label") { SectionHeader(label) }
                    items(list, key = { it.id }) { t ->
                        TeamRow(team = t, showGroup = true) { onTeamClick(t) }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
        TeamSort.GROUP -> {
            val sorted = remember(teams) { teams.sortedWith(compareBy({ it.group }, { it.fifaRank })) }
            val byGroup = remember(sorted) { sorted.groupBy { it.group } }
            val groups = remember(byGroup) { byGroup.keys.sorted() }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                groups.forEach { grp ->
                    val list = byGroup[grp] ?: return@forEach
                    item(key = "hd_$grp") { SectionHeader("Group $grp") }
                    items(list, key = { it.id }) { t ->
                        TeamRow(team = t, showGroup = false) { onTeamClick(t) }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
        TeamSort.NAME -> {
            val sorted = remember(teams) { teams.sortedBy { it.name } }
            val grouped = remember(sorted) { sorted.groupBy { it.name.first().uppercaseChar() } }
            val keys = remember(grouped) { grouped.keys.sorted() }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                keys.forEach { letter ->
                    val list = grouped[letter] ?: return@forEach
                    item(key = "hd_$letter") { SectionHeader(letter.toString()) }
                    items(list, key = { it.id }) { t ->
                        TeamRow(team = t, showGroup = true) { onTeamClick(t) }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        label.uppercase(),
        modifier = Modifier
            .fillMaxWidth()
            .background(Bg)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Label3,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun TeamRow(team: Team, showGroup: Boolean, onClick: () -> Unit) {
    val ovr = remember(team.name) { teamOpta(team.name).ovr }
    val ovrClr = when {
        ovr >= 85 -> Color(0xFFB8860B)
        ovr >= 80 -> Blue
        ovr >= 75 -> Color(0xFF1C7A3E)
        else      -> Gray
    }
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .background(WCSurface)
                .clickable { onClick() }
                .padding(start = 12.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val flagFile = team.flagFile.ifEmpty { team.imageFile }
            CountryFlag(
                flagFile = flagFile,
                modifier = Modifier.width(28.dp).height(19.dp).clip(RoundedCornerShape(3.dp))
            )
            Spacer(Modifier.width(10.dp))
            // Name + sub-line (group · confederation)
            Column(Modifier.weight(1f)) {
                Text(
                    team.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (team.isWithdrawn) Label2 else Label1,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sub = if (showGroup) "Group ${team.group} · ${team.confederation}" else team.confederation
                Text(sub, fontSize = 11.sp, color = Label3)
            }
            Spacer(Modifier.width(8.dp))
            // OVR badge — always fixed-width for vertical alignment
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(ovrClr.copy(alpha = 0.12f))
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (ovr > 0) "$ovr" else "–",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = ovrClr, textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.width(8.dp))
            // Rank + bar — fixed 40dp width for consistent alignment across all sort modes
            Column(
                modifier = Modifier.width(40.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    "#${team.fifaRank}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = rankBarColor(team.fifaRank),
                    maxLines = 1
                )
                RankBar(team.fifaRank, maxWidth = 28.dp)
            }
            Spacer(Modifier.width(6.dp))
            Text("›", fontSize = 13.sp, color = Label1.copy(alpha = 0.2f))
        }
        HorizontalDivider(
            thickness = 0.5.dp, color = Separator,
            modifier = Modifier.padding(start = 50.dp)
        )
    }
}

// ─── Team Detail ──────────────────────────────────────────────────────────────

@Composable
private fun TeamDetail(
    team: Team,
    roster: List<RosterPlayer>,
    isRosterLoading: Boolean,
    onClose: () -> Unit,
    onPlayerClick: (Int) -> Unit = {},
    onPlayerFullClick: (com.carldong.fifa.worldcup2026.data.Player) -> Unit = {},
    vm: TeamsViewModel = viewModel()
) {
    val primaryColor = remember(team.primaryColor) {
        try { Color(android.graphics.Color.parseColor(team.primaryColor)) }
        catch (_: Exception) { Color(0xFF333333) }
    }
    val opta = remember(team.name) { teamOpta(team.name) }
    val history = remember(team.name) { teamHistory(team.name) }
    val titles = remember(team.name) { teamTitles(team.name) }
    // Always use Team.csv OVR (consistent with list page)
    val teamOvr = opta.ovr

    // Hero colors
    val zoneAColor = remember(primaryColor) {
        val r = (primaryColor.red * 255).toInt()
        val g = (primaryColor.green * 255).toInt()
        val b = (primaryColor.blue * 255).toInt()
        Color(
            red = ((255 * 0.75f + r * 0.25f) / 255f).coerceIn(0f, 1f),
            green = ((245 * 0.75f + g * 0.25f) / 255f).coerceIn(0f, 1f),
            blue = ((232 * 0.75f + b * 0.25f) / 255f).coerceIn(0f, 1f)
        )
    }
    val zoneBColor = remember(primaryColor) {
        Color(
            red = (28f / 255f * 0.35f + primaryColor.red * 0.65f).coerceIn(0f, 1f),
            green = (28f / 255f * 0.35f + primaryColor.green * 0.65f).coerceIn(0f, 1f),
            blue = (46f / 255f * 0.35f + primaryColor.blue * 0.65f).coerceIn(0f, 1f)
        )
    }

    var squadFilter by remember { mutableStateOf("All") }
    val filteredRoster = remember(roster, squadFilter) {
        (if (squadFilter == "All") roster else roster.filter { it.pos == squadFilter })
            .sortedByDescending { it.ovr }
    }

    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize()) {
            // Nav bar
            Surface(color = WCSurface.copy(alpha = 0.95f)) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onClose) {
                            Text("‹  Teams", color = Blue, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            team.abbr,
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            color = Label1, letterSpacing = (-0.3).sp
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Separator)
                }
            }

            LazyColumn(Modifier.fillMaxSize()) {
                item {

                // ── Hero Zone A: team badge + name ──
                Box(
                    Modifier.fillMaxWidth().background(zoneAColor)
                        .padding(top = 28.dp, bottom = 22.dp, start = 20.dp, end = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Team badge (emblem) - 100x100
                        Box(
                            Modifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TeamBadge(
                                imageFile = teamBadgeFile(team.name),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Text(
                            team.name,
                            fontSize = 24.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp, color = Color(0xFF1A1A1A),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ── Hero Zone B: 4-col grid ──
                Row(
                    Modifier.fillMaxWidth().background(zoneBColor)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Col 1: flag + abbr — pushed down slightly
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Box(Modifier.size(width = 52.dp, height = 35.dp).clip(RoundedCornerShape(5.dp))) {
                                CountryFlag(team.flagFile.ifEmpty { team.imageFile }, Modifier.fillMaxSize())
                            }
                            Text(
                                team.abbr,
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.75f), letterSpacing = 0.4.sp
                            )
                        }
                    }
                    // Col 2: FIFA rank tile
                    DarkTileHero(label = "FIFA", value = "${team.fifaRank}", modifier = Modifier.width(56.dp))
                    // Col 3: WC titles tile
                    DarkTileHero(label = "TITLES", value = "$titles", modifier = Modifier.width(56.dp))
                    // Col 4: OVR + stars
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            val ovrColor3 = when {
                                teamOvr >= 90 -> Color(0xFFB8860B)
                                teamOvr >= 85 -> Blue
                                teamOvr >= 80 -> Color(0xFF34C759)
                                else -> Gray
                            }
                            Text(
                                "$teamOvr",
                                fontSize = 38.sp, fontWeight = FontWeight.ExtraBold,
                                color = ovrColor3, letterSpacing = (-1.5).sp, lineHeight = 40.sp
                            )
                            TeamStarsRow(rank = team.fifaRank)
                        }
                    }
                }

                // ── Attributes (Radar + bars) ──
                Surface(Modifier.fillMaxWidth(), color = WCSurface) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "ATTRIBUTES",
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = Label3, letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TeamRadarChart(stats = opta, primaryColor = primaryColor, modifier = Modifier.size(220.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                // ── WC History ──
                if (history.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "World Cup History",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp, color = Label1,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
                    )
                    Surface(
                        Modifier.fillMaxWidth(),
                        color = WCSurface
                    ) {
                        Column {
                            history.forEachIndexed { idx, h ->
                                WCHistoryRow(entry = h)
                                if (idx < history.lastIndex) {
                                    HorizontalDivider(thickness = 0.5.dp, color = Separator)
                                }
                            }
                        }
                    }
                }

                // ── Squad ──
                Spacer(Modifier.height(8.dp))
                Text(
                    "Official 26-Man Squad",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp, color = Label1,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
                )

                // Position filter chips — full width
                val posFilters = listOf("All", "GK", "DF", "MF", "FW")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    posFilters.forEach { pos ->
                        val isOn = squadFilter == pos
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isOn) Blue else Color(0x1F787880))
                                .clickable { squadFilter = pos }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                pos,
                                fontSize = 13.sp,
                                fontWeight = if (isOn) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isOn) Color.White else Label2
                            )
                        }
                    }
                }

                }

                if (isRosterLoading) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().background(WCSurface).padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Blue,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Loading squad...", fontSize = 13.sp, color = Label3)
                            }
                        }
                    }
                } else {
                    itemsIndexed(filteredRoster, key = { _, player -> "${player.playerId}_${player.name}" }) { idx, player ->
                        Box(Modifier.fillMaxWidth().background(WCSurface)) {
                            SquadPlayerRow(
                                player = player,
                                displayNumber = player.number ?: (idx + 1),
                                numberLabelColor = primaryColor,
                                onClick = {
                                    val full = vm.buildPlayerForNav(player, team.name)
                                    if (full != null) {
                                        onPlayerFullClick(full)
                                    } else if (player.playerId > 0) {
                                        onPlayerClick(player.playerId)
                                    } else {
                                        onPlayerClick(0)
                                    }
                                }
                            )
                        }
                        if (idx < filteredRoster.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp, color = Separator,
                                modifier = Modifier.padding(start = 56.dp)
                            )
                        }
                    }

                    if (filteredRoster.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().background(WCSurface).padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No squad data available", fontSize = 13.sp, color = Label3)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun TeamAttrBar(name: String, value: Int, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            modifier = Modifier.width(36.dp),
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = Label3, letterSpacing = 0.3.sp
        )
        Box(
            Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                .background(Color(0x1F787880))
        ) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(value / 100f)
                    .clip(RoundedCornerShape(3.dp)).background(color)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "$value",
            modifier = Modifier.width(26.dp),
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = color, textAlign = TextAlign.End
        )
    }
}

@Composable
private fun DarkTileHero(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                label,
                fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(0.4f), letterSpacing = 0.7.sp
            )
            Text(
                value,
                fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = Color.White, letterSpacing = (-0.5).sp
            )
        }
    }
}

@Composable
private fun TeamStarsRow(rank: Int) {
    val total = when {
        rank <= 5  -> 5
        rank <= 15 -> 4
        rank <= 30 -> 3
        rank <= 60 -> 2
        else       -> 1
    }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..5) {
            Canvas(Modifier.size(18.dp)) {
                val filled = i <= total
                val starColor = if (filled) Color(0xFFFFD700) else Color(0xFF2C2C2E)
                drawPath(
                    path = starPath(size.width, size.height),
                    color = starColor
                )
            }
        }
    }
}

private fun starPath(w: Float, h: Float): Path {
    val path = Path()
    val cx = w / 2f; val cy = h / 2f
    val outerR = w / 2f; val innerR = w * 0.35f / 2f
    for (i in 0 until 10) {
        val angle = (PI / 2 + i * PI / 5).toFloat()
        val r = if (i % 2 == 0) outerR else innerR
        val x = cx + r * cos(angle); val y = cy - r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

@Composable
private fun TeamRadarChart(stats: TeamStats, primaryColor: Color, modifier: Modifier = Modifier) {
    val cr = primaryColor.red; val cg = primaryColor.green; val cb = primaryColor.blue

    Canvas(modifier = modifier) {
        val cx = size.width / 2f; val cy = size.height / 2f
        val r = size.width * 0.30f        // smaller radius → room for labels
        val labelR = r + 20.dp.toPx()    // axis label distance
        val n = 6
        val step = (2f * PI / n).toFloat()
        val startAngle = (-PI / 2f).toFloat()

        val axes = listOf("PAC", "SHO", "PAS", "DRI", "DEF", "PHY")
        val vals = listOf(stats.pac, stats.sho, stats.pas, stats.dri, stats.def, stats.phy)

        fun pt(i: Int, frac: Float): Pair<Float, Float> {
            val a = startAngle + i * step
            return Pair(cx + cos(a) * r * frac, cy + sin(a) * r * frac)
        }

        // Background rings
        for (ri in 1..4) {
            val frac = ri / 4f
            val hexPath = Path()
            for (i in 0 until n) {
                val (x, y) = pt(i, frac)
                if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
            }
            hexPath.close()
            drawPath(hexPath, Color(0x14000000), style = Stroke(if (ri == 4) 1.2.dp.toPx() else 0.8.dp.toPx()))
        }

        // Axis spokes
        for (i in 0 until n) {
            val (x, y) = pt(i, 1f)
            drawLine(Color(0x18000000), Offset(cx, cy), Offset(x, y), 0.8.dp.toPx())
        }

        // Data polygon
        val dataPath = Path()
        for (i in 0 until n) {
            val (x, y) = pt(i, vals[i] / 100f)
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        drawPath(dataPath, Color(cr, cg, cb, 0.15f))
        drawPath(dataPath, Color(cr, cg, cb, 0.85f), style = Stroke(2.dp.toPx()))

        // Vertex dots
        for (i in 0 until n) {
            val (x, y) = pt(i, vals[i] / 100f)
            drawCircle(Color(cr, cg, cb, 0.18f), 4.5.dp.toPx(), Offset(x, y))
            drawCircle(Color(cr, cg, cb, 1f), 2.8.dp.toPx(), Offset(x, y))
            drawCircle(Color.White, 2.8.dp.toPx(), Offset(x, y), style = Stroke(1.2.dp.toPx()))
        }

        // Axis name labels + value labels via native canvas
        val nc = drawContext.canvas.nativeCanvas

        // Axis labels
        val lblPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(217,
                (cr * 255).toInt(), (cg * 255).toInt(), (cb * 255).toInt())
            textSize = 10.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        for (i in 0 until n) {
            val a = startAngle + i * step
            val lx = cx + cos(a) * labelR
            val ly = cy + sin(a) * labelR
            val fm = lblPaint.fontMetrics
            nc.drawText(axes[i], lx, ly - (fm.ascent + fm.descent) / 2f, lblPaint)
        }

        // Value labels at actual data point positions (slightly outward)
        val bgPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(235, 255, 255, 255); isAntiAlias = true
        }
        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(100,
                (cr * 255).toInt(), (cg * 255).toInt(), (cb * 255).toInt())
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 0.8.dp.toPx(); isAntiAlias = true
        }
        val valPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255,
                (cr * 255).toInt(), (cg * 255).toInt(), (cb * 255).toInt())
            textSize = 9.5.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val pw = 22.dp.toPx(); val ph = 14.dp.toPx(); val rr = 4.dp.toPx()
        for (i in 0 until n) {
            val v = vals[i]
            if (v <= 0) continue
            val frac = (v / 100f + 0.13f).coerceAtMost(0.96f)
            val a = startAngle + i * step
            val vx = cx + cos(a) * r * frac
            val vy = cy + sin(a) * r * frac
            val rect = android.graphics.RectF(vx - pw/2, vy - ph/2, vx + pw/2, vy + ph/2)
            nc.drawRoundRect(rect, rr, rr, bgPaint)
            nc.drawRoundRect(rect, rr, rr, borderPaint)
            val fm = valPaint.fontMetrics
            nc.drawText("$v", vx, vy - (fm.ascent + fm.descent) / 2f, valPaint)
        }
    }
}

@Composable
private fun WCHistoryRow(entry: WCHistoryEntry) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "${entry.year}",
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = Label1, modifier = Modifier.width(40.dp)
        )
        Box(Modifier.size(width = 22.dp, height = 15.dp).clip(RoundedCornerShape(2.dp))) {
            CountryFlag(entry.hostFlagFile, Modifier.fillMaxSize())
        }
        Text(
            histResultText(entry.result),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = histResultColor(entry.result),
            modifier = Modifier.weight(1f)
        )
        histBadge(entry.result)
    }
}

private fun histResultText(r: String) = when (r) {
    "Champion"   -> "Champion"
    "Runner-up"  -> "Runner-up"
    "3rd"        -> "3rd Place"
    "4th"        -> "4th Place"
    "SF"         -> "Semi-final"
    "QF"         -> "Quarter-final"
    "R16"        -> "Round of 16"
    "R32"        -> "Round of 32"
    "GS"         -> "Group Stage"
    "DNQ"        -> "Did Not Qualify"
    else         -> r
}

private fun histResultColor(r: String) = when (r) {
    "Champion"  -> Color(0xFFB8860B)
    "Runner-up" -> Color(0xFF8E8E93)
    "3rd"       -> Color(0xFFCD7F32)
    else        -> Color(0x4D3C3C43)
}

@Composable
private fun histBadge(result: String) {
    val badgeWidth = 108.dp
    when (result) {
        "Champion" -> Row(
            Modifier
                .size(width = badgeWidth, height = 30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x26DAA520))
                .padding(start = 9.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            AssetImage("pic/wc.png", Modifier.size(15.dp))
            Text(
                "Champion",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB8860B)
            )
        }
        "Runner-up" -> Box(
            Modifier.size(width = badgeWidth, height = 30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x1E787880)),
            contentAlignment = Alignment.Center
        ) {
            Text("2nd Place", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E93))
        }
        "3rd" -> Box(
            Modifier.size(width = badgeWidth, height = 30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x1ECD7F32)),
            contentAlignment = Alignment.Center
        ) {
            Text("3rd Place", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCD7F32))
        }
        else -> Spacer(Modifier.size(0.dp))
    }
}
@Composable
private fun SquadPlayerRow(
    player: RosterPlayer,
    displayNumber: Int? = player.number,
    numberLabelColor: Color = Color(0xFF007AFF),
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 10.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Jersey number label — team primary color background, white digit, centered
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(numberLabelColor.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayNumber?.toString() ?: "",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }

        // Avatar circle (36dp)
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(Color(0x1F787880)),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (player.avatarFile.isNotEmpty()) {
                PlayerAvatar(player.avatarFile, Modifier.fillMaxSize())
            } else {
                Box(Modifier.size(13.dp).clip(CircleShape).background(Color(0xFFC7C7CC)).align(Alignment.TopCenter).offset(y = 6.dp))
                Box(Modifier.fillMaxWidth().height(18.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(Color(0xFFC7C7CC)))
            }
        }

        // Name + club
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    player.name,
                    fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = Label1, letterSpacing = (-0.2).sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (player.captain) {
                    Spacer(Modifier.width(4.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0x26B8860B))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text("CAP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB8860B), letterSpacing = 0.3.sp)
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 1.dp)
            ) {
                val clubFile = player.clubLogoFile.ifEmpty { clubToLogoFile(player.club) }
                if (clubFile.isNotEmpty()) {
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x1F787880)),
                        contentAlignment = Alignment.Center
                    ) {
                        ClubLogo(clubFile, Modifier.fillMaxSize())
                    }
                }
                Text(player.club, fontSize = 11.5.sp, color = Label3, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // Pos badge
        PosBadge(player.pos, modifier = Modifier.width(28.dp))

        // OVR
        val ovr = player.ovr
        Text(
            if (ovr > 0) "$ovr" else "—",
            fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = if (ovr > 0) ovrColor(ovr) else Label3,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.End
        )

        // Chevron — always shown (all players are tappable)
        Text("›", fontSize = 13.sp, color = Label1.copy(alpha = 0.2f), modifier = Modifier.width(12.dp))
    }
}


