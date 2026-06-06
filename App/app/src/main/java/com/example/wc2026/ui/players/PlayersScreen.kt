package com.carldong.fifa.worldcup2026.ui.players

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carldong.fifa.worldcup2026.data.Player
import com.carldong.fifa.worldcup2026.data.PlayerData
import com.carldong.fifa.worldcup2026.theme.*
import com.carldong.fifa.worldcup2026.ui.components.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

// ─── Main Screen ──────────────────────────────────────────────────────────────

@Composable
fun PlayersScreen(
    pendingPlayerId: Int? = null,
    pendingPlayer: com.carldong.fifa.worldcup2026.data.Player? = null,
    onPendingConsumed: () -> Unit = {},
    homeResetToken: Int = 0,
    onDetailClosed: () -> Unit = {},
    vm: PlayersViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var lastHomeResetToken by rememberSaveable { mutableIntStateOf(homeResetToken) }
    val playersListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val players = remember(state.searchQuery, state.sort, state.posFilter, state.players, state.isLoading) {
        if (state.isLoading) emptyList() else vm.filteredPlayers(state)
    }

    LaunchedEffect(pendingPlayerId, pendingPlayer) {
        when {
            pendingPlayer != null -> { vm.selectPlayer(pendingPlayer); onPendingConsumed() }
            pendingPlayerId != null && pendingPlayerId > 0 -> { vm.selectPlayerById(pendingPlayerId); onPendingConsumed() }
        }
    }

    LaunchedEffect(homeResetToken) {
        if (homeResetToken != lastHomeResetToken) {
            lastHomeResetToken = homeResetToken
            vm.selectPlayer(null)
        }
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.fillMaxSize()) {
            PlayersNavBar()
            PlayersSearchBar(state.searchQuery, vm::setSearch)
            PlayersSortControl(state.sort, vm::setSort)
            AnimatedVisibility(
                visible = state.sort == PlayerSort.POS,
                enter = expandVertically(tween(200)) + fadeIn(),
                exit = shrinkVertically(tween(160)) + fadeOut()
            ) {
                PosFilterRow(state.posFilter, vm::setPosFilter)
            }
            HorizontalDivider(thickness = 0.5.dp, color = Separator)
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Blue)
                }
            } else {
                PlayersListContent(
                    players = players,
                    sort = state.sort,
                    posFilter = state.posFilter,
                    listState = playersListState,
                    onPlayerClick = vm::selectPlayer
                )
            }
        }

        AnimatedVisibility(
            visible = state.selectedPlayer != null,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(260)
            )
        ) {
            state.selectedPlayer?.let { player ->
                PlayerDetail(
                    player = player,
                    onClose = {
                        vm.selectPlayer(null)
                        onDetailClosed()
                    }
                )
            }
        }
    }

    if (state.selectedPlayer != null) {
        BackHandler {
            vm.selectPlayer(null)
            onDetailClosed()
        }
    }
}

// ─── NavBar ───────────────────────────────────────────────────────────────────

@Composable
private fun PlayersNavBar() {
    Surface(color = WCSurface, shadowElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "Players",
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
private fun PlayersSearchBar(query: String, onQuery: (String) -> Unit) {
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
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text("Search players…", fontSize = 15.sp, color = Label3, lineHeight = 20.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    textStyle = TextStyle(fontSize = 15.sp, color = Label1, lineHeight = 20.sp),
                    cursorBrush = SolidColor(Blue),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0x60787880))
                        .clickable { onQuery("") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = Color.White, lineHeight = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// ─── Sort control ─────────────────────────────────────────────────────────────

@Composable
private fun PlayersSortControl(sort: PlayerSort, onSort: (PlayerSort) -> Unit) {
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
            PlayerSort.entries.forEach { s ->
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
                        text = when (s) { PlayerSort.OVR -> "Overall"; PlayerSort.POS -> "Position" },
                        fontSize = 13.sp,
                        fontWeight = if (isOn) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isOn) Label1 else Label2
                    )
                }
            }
        }
    }
}

// ─── Position filter ──────────────────────────────────────────────────────────

private val POS_LABELS = listOf("ALL", "GK", "DF", "MF", "FW")

@Composable
private fun PosFilterRow(posFilter: String, onPos: (String) -> Unit) {
    Surface(color = WCSurface) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            POS_LABELS.forEach { pos ->
                val isOn = posFilter == pos
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isOn) Blue else Color(0x1F787880))
                        .clickable { onPos(pos) }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        pos,
                        fontSize = 12.sp,
                        fontWeight = if (isOn) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isOn) Color.White else Label2
                    )
                }
            }
        }
    }
}

// ─── Player list ──────────────────────────────────────────────────────────────

@Composable
private fun PlayersListContent(
    players: List<Player>,
    sort: PlayerSort,
    posFilter: String,
    listState: LazyListState,
    onPlayerClick: (Player) -> Unit
) {
    when (sort) {
        PlayerSort.OVR -> {
            val topPlayers = remember(players) { players.sortedByDescending { it.ovr }.take(10) }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                item(key = "hd_overall_top_10") { SectionHeader("Overall Top 10") }
                itemsIndexed(topPlayers, key = { _, p -> p.id }) { idx, p ->
                    PlayerRow(player = p, rank = idx + 1) { onPlayerClick(p) }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
        PlayerSort.POS -> {
            val posNames = mapOf(
                "ALL" to "All Positions Top 10",
                "GK" to "Goalkeepers Top 10",
                "DF" to "Defenders Top 10",
                "MF" to "Midfielders Top 10",
                "FW" to "Forwards Top 10"
            )
            val topPlayers = remember(players) { players.sortedByDescending { it.ovr }.take(10) }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                item(key = "hd_pos_$posFilter") {
                    SectionHeader(posNames[posFilter] ?: "$posFilter Top 10")
                }
                itemsIndexed(topPlayers, key = { _, p -> p.id }) { idx, p ->
                    PlayerRow(player = p, rank = idx + 1) { onPlayerClick(p) }
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
private fun PlayerRow(player: Player, rank: Int, onClick: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .background(WCSurface)
                .clickable { onClick() }
                .padding(start = 10.dp, end = 12.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                "$rank",
                modifier = Modifier.width(24.dp),
                fontSize = 12.sp, fontWeight = FontWeight.Medium,
                color = Label3, textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(8.dp))

            // Avatar with real image if available
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFF2F2F7)),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (player.avatarFile.isNotEmpty()) {
                    PlayerAvatar(player.avatarFile, Modifier.fillMaxSize())
                } else {
                    PlayerAvatarPlaceholder()
                }
            }
            Spacer(Modifier.width(8.dp))

            // Info: name + flag + country
            Column(Modifier.weight(1f)) {
                Text(
                    player.name,
                    fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = Label1, letterSpacing = (-0.2).sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    CountryFlag(
                        flagFile = player.flagFile,
                        modifier = Modifier.size(width = 14.dp, height = 10.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                    )
                    Text(player.country, fontSize = 11.sp, color = Label3, maxLines = 1)
                }
            }
            Spacer(Modifier.width(8.dp))

            // Position badge
            PosBadge(player.pos, modifier = Modifier.width(30.dp))
            Spacer(Modifier.width(8.dp))

            // OVR
            Text(
                "${player.ovr}",
                modifier = Modifier.width(30.dp),
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = ovrColor(player.ovr), textAlign = TextAlign.End
            )
            Spacer(Modifier.width(4.dp))

            // Chevron
            Text("›", fontSize = 13.sp, color = Label1.copy(alpha = 0.2f), modifier = Modifier.width(14.dp))
        }
        HorizontalDivider(thickness = 0.5.dp, color = Separator, modifier = Modifier.padding(start = 50.dp))
    }
}

@Composable
private fun PlayerAvatarPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        // Head
        Box(Modifier.size(13.dp).clip(CircleShape).background(Color(0xFFC7C7CC))
            .align(Alignment.TopCenter).offset(y = 7.dp))
        // Body
        Box(Modifier.fillMaxWidth().height(18.dp)
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .background(Color(0xFFC7C7CC)))
    }
}

// ─── Player Detail ────────────────────────────────────────────────────────────

@Composable
private fun PlayerDetail(player: Player, onClose: () -> Unit) {
    val primaryColor = countryPrimaryColor(player.country)
    val heroA = remember(primaryColor) {
        Color(
            red = ((255 * 0.75f + primaryColor.red * 255f * 0.25f) / 255f).coerceIn(0f,1f),
            green = ((245 * 0.75f + primaryColor.green * 255f * 0.25f) / 255f).coerceIn(0f,1f),
            blue = ((232 * 0.75f + primaryColor.blue * 255f * 0.25f) / 255f).coerceIn(0f,1f)
        )
    }
    val heroB = blendOnDark(primaryColor, 0.65f)

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
                            Text("‹  Players", color = Blue, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            player.name,
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = Label1, letterSpacing = (-0.3).sp
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Separator)
                }
            }

            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

                // ── Hero Zone A: avatar + full name ──
                Box(
                    Modifier.fillMaxWidth().background(heroA)
                        .padding(top = 28.dp, bottom = 22.dp, start = 20.dp, end = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Large avatar 96dp circle
                        Box(
                            Modifier.size(96.dp).clip(CircleShape)
                                .background(Color(0xFFE8DDD0)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (player.avatarFile.isNotEmpty()) {
                                PlayerAvatar(player.avatarFile, Modifier.fillMaxSize())
                            } else {
                                Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFC7C7CC)).align(Alignment.TopCenter).offset(y = 16.dp))
                                Box(Modifier.fillMaxWidth().height(50.dp)
                                    .clip(RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp))
                                    .background(Color(0xFFC7C7CC)))
                            }
                        }
                        Text(
                            player.full,
                            fontSize = 26.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A), letterSpacing = (-0.6).sp,
                            textAlign = TextAlign.Center, lineHeight = 32.sp
                        )
                    }
                }

                // ── Hero Zone B: dark background, 4-col grid ──
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(heroB).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Col 1: Country flag + name — pushed down slightly
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Box(Modifier.size(width = 58.dp, height = 40.dp).clip(RoundedCornerShape(6.dp))) {
                                CountryFlag(player.flagFile, Modifier.fillMaxSize())
                            }
                            Text(
                                player.country,
                                fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    // Col 2: Age tile
                    DarkTile(label = "AGE", value = "${player.age}", modifier = Modifier.width(56.dp).fillMaxHeight())
                    // Col 3: Pos tile — detailed position from PlayerData map
                    val detailedPos = player.detailedPos.ifEmpty { PlayerData.detailedPos[player.id] ?: player.pos }
                    DarkPosTile(pos = player.pos, detailedPos = detailedPos, modifier = Modifier.width(56.dp).fillMaxHeight())
                    // Col 4: OVR + stars
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                "${player.ovr}",
                                fontSize = 40.sp, fontWeight = FontWeight.ExtraBold,
                                color = ovrColor(player.ovr), letterSpacing = (-1.5).sp, lineHeight = 42.sp
                            )
                            PlayerStarsRow(ovr = player.ovr)
                        }
                    }
                }

                // ── Attributes ── radar only, no bars
                Surface(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    color = WCSurface
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("ATTRIBUTES", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Label3, letterSpacing = 0.5.sp, modifier = Modifier.padding(bottom = 12.dp))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            PlayerRadarChart(player = player, primaryColor = primaryColor, modifier = Modifier.size(220.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                // ── Club ──
                Surface(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    color = WCSurface
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("CLUB", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Label3, letterSpacing = 0.5.sp, modifier = Modifier.padding(bottom = 12.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            val clubFile = player.clubLogoFile.ifEmpty { clubToLogoFile(player.club) }
                            val clubCountry = player.clubCC.trim()
                            val leagueLabel = clubLeagueLabel(clubCountry)
                            val clubFlag = clubCountryFlagFile(clubCountry)
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF2F2F7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (clubFile.isNotEmpty()) {
                                        ClubLogo(clubFile, Modifier.size(42.dp).padding(2.dp))
                                    } else {
                                        Text(
                                            player.club.take(3).uppercase(),
                                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Blue
                                        )
                                    }
                                }
                                Column(
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        player.club.ifEmpty { "Free Agent" },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Label1,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (leagueLabel.isNotEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (clubFlag.isNotEmpty()) {
                                                CountryFlag(
                                                    flagFile = clubFlag,
                                                    modifier = Modifier.size(width = 13.dp, height = 9.dp)
                                                        .clip(RoundedCornerShape(1.dp))
                                                )
                                            }
                                            Text(
                                                leagueLabel,
                                                fontSize = 12.sp,
                                                color = Label3.copy(alpha = 0.55f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            if (player.valueMEUR > 0) {
                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        "€${player.valueMEUR}M",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Label1
                                    )
                                    Text("MARKET VALUE", fontSize = 9.5.sp, color = Label3.copy(alpha = 0.65f))
                                }
                            }
                        }
                    }
                }

                // ── Physical Profile ──
                Surface(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    color = WCSurface
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("PHYSICAL PROFILE", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Label3, letterSpacing = 0.5.sp, modifier = Modifier.padding(bottom = 12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricTile(player.height, "Height", Modifier.weight(1f))
                            MetricTile(player.weight, "Weight", Modifier.weight(1f))
                            MetricTile(if (player.foot == "R") "Right" else "Left", "Pref. Foot", Modifier.weight(1f))
                        }
                    }
                }

                // ── National Team Stats ──
                Surface(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    color = WCSurface
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("NATIONAL TEAM", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Label3, letterSpacing = 0.5.sp, modifier = Modifier.padding(bottom = 12.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricTile("${player.caps}", "Caps", Modifier.weight(1f))
                            MetricTile("${player.cGoals}", "Goals", Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ─── Detail sub-composables ───────────────────────────────────────────────────

@Composable
private fun DarkTile(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(0.4f), letterSpacing = 0.7.sp)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.5).sp)
        }
    }
}

@Composable
private fun DarkPosTile(pos: String, detailedPos: String = pos, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("POS", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(0.4f), letterSpacing = 0.7.sp)
            // Show detailed position (e.g. "CAM", "RB") with same size as age value
            Text(
                detailedPos,
                fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = Color.White, letterSpacing = (-0.5).sp
            )
        }
    }
}

@Composable
private fun PlayerStarsRow(ovr: Int) {
    val total = (ovr / 20f).coerceIn(0f, 5f)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..5) {
            Canvas(Modifier.size(18.dp)) {
                val filled = i.toFloat() <= total
                drawPath(
                    path = starPath5(size.width, size.height),
                    color = if (filled) Color(0xFFFFD700) else Color(0xFF2C2C2E)
                )
            }
        }
    }
}

private fun starPath5(w: Float, h: Float): Path {
    val path = Path()
    val cx = w / 2f; val cy = h / 2f
    val outerR = w / 2f; val innerR = w * 0.4f / 2f
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
private fun PlayerRadarChart(player: Player, primaryColor: Color, modifier: Modifier = Modifier) {
    val cr = primaryColor.red; val cg = primaryColor.green; val cb = primaryColor.blue

    Canvas(modifier = modifier) {
        val cx = size.width / 2f; val cy = size.height / 2f
        val r = size.width * 0.30f
        val labelR = r + 20.dp.toPx()
        val n = 6
        val step = (2f * PI / n).toFloat()
        val startAngle = (-PI / 2f).toFloat()

        val axes = listOf("PAC", "SHO", "PAS", "DRI", "DEF", "PHY")
        val vals = listOf(player.spd, player.atk, player.pas, player.dri, player.def, player.phy)

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

        // Axis name labels outside ring
        val nc = drawContext.canvas.nativeCanvas
        val lblPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(217, (cr * 255).toInt(), (cg * 255).toInt(), (cb * 255).toInt())
            textSize = 10.dp.toPx(); textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        for (i in 0 until n) {
            val a = startAngle + i * step
            val lx = cx + cos(a) * labelR; val ly = cy + sin(a) * labelR
            val fm = lblPaint.fontMetrics
            nc.drawText(axes[i], lx, ly - (fm.ascent + fm.descent) / 2f, lblPaint)
        }

        // Value labels at actual data-point positions
        val bgPaint = android.graphics.Paint().apply { color = android.graphics.Color.argb(235, 255, 255, 255); isAntiAlias = true }
        val bdPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(100, (cr * 255).toInt(), (cg * 255).toInt(), (cb * 255).toInt())
            style = android.graphics.Paint.Style.STROKE; strokeWidth = 0.8.dp.toPx(); isAntiAlias = true
        }
        val valPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, (cr * 255).toInt(), (cg * 255).toInt(), (cb * 255).toInt())
            textSize = 9.5.dp.toPx(); textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val pw = 22.dp.toPx(); val ph = 14.dp.toPx(); val rr = 4.dp.toPx()
        for (i in 0 until n) {
            val v = vals[i]; if (v <= 0) continue
            val frac = (v / 100f + 0.13f).coerceAtMost(0.96f)
            val a = startAngle + i * step
            val vx = cx + cos(a) * r * frac; val vy = cy + sin(a) * r * frac
            val rect = android.graphics.RectF(vx - pw/2, vy - ph/2, vx + pw/2, vy + ph/2)
            nc.drawRoundRect(rect, rr, rr, bgPaint); nc.drawRoundRect(rect, rr, rr, bdPaint)
            val fm = valPaint.fontMetrics
            nc.drawText("$v", vx, vy - (fm.ascent + fm.descent) / 2f, valPaint)
        }
    }
}

@Composable
private fun PlayerAttrBar(name: String, value: Int, color: Color) {
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
private fun MetricTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF2F2F7))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Label1)
        }
        Text(label, fontSize = 9.5.sp, fontWeight = FontWeight.Medium, color = Label3, letterSpacing = 0.4.sp)
    }
}

@Composable
private fun PhyTile(value: String, label: String) {
    MetricTile(value = value, label = label)
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    MetricTile(value = value, label = label, modifier = modifier)
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun countryPrimaryColor(country: String): Color = when (country) {
    "France"    -> Color(0xFF002395)
    "Argentina" -> Color(0xFF74ACDF)
    "Brazil"    -> Color(0xFF009C3B)
    "England"   -> Color(0xFFCE1124)
    "Germany"   -> Color(0xFF000000)
    "Spain"     -> Color(0xFFAA151B)
    "Nigeria"   -> Color(0xFF008751)
    "Morocco"   -> Color(0xFFC1272D)
    "Japan"     -> Color(0xFFBC002D)
    else        -> Color(0xFF333333)
}

private fun blendOnDark(primary: Color, alpha: Float): Color {
    val br = 28f / 255f; val bg = 28f / 255f; val bb = 46f / 255f
    return Color(
        red   = (br * (1 - alpha) + primary.red * alpha).coerceIn(0f,1f),
        green = (bg * (1 - alpha) + primary.green * alpha).coerceIn(0f,1f),
        blue  = (bb * (1 - alpha) + primary.blue * alpha).coerceIn(0f,1f)
    )
}

private fun clubCCToFlagFile(cc: String) = when (cc) {
    "es"     -> "Spain.png"
    "gb-eng" -> "England.png"
    "de"     -> "Germany.png"
    "it"     -> "Italy.png"
    "fr"     -> "France.png"
    "tr"     -> "Turkey.png"
    "us"     -> "United States.png"
    "pt"     -> "Portugal.png"
    "nl"     -> "Netherlands.png"
    else     -> ""
}

private fun clubCCToLeague(cc: String) = when (cc) {
    "es"     -> "La Liga · Spain"
    "gb-eng" -> "Premier League · England"
    "de"     -> "Bundesliga · Germany"
    "it"     -> "Serie A · Italy"
    "fr"     -> "Ligue 1 · France"
    "tr"     -> "Süper Lig · Turkey"
    "us"     -> "MLS · USA"
    "pt"     -> "Primeira Liga · Portugal"
    "nl"     -> "Eredivisie · Netherlands"
    else     -> cc
}

private fun clubLeagueLabel(countryOrCode: String): String {
    val key = countryOrCode.trim().lowercase()
    return when (key) {
        "", "-" -> ""
        "gb-eng", "england", "the england" -> "Premier League · England"
        "es", "spain" -> "La Liga · Spain"
        "de", "germany" -> "Bundesliga · Germany"
        "it", "italy" -> "Serie A · Italy"
        "fr", "france" -> "Ligue 1 · France"
        "tr", "turkey", "türkiye" -> "Süper Lig · Turkey"
        "us", "usa", "united states", "the united states" -> "MLS · USA"
        "pt", "portugal" -> "Primeira Liga · Portugal"
        "nl", "netherlands", "the netherlands", "holland" -> "Eredivisie · Netherlands"
        "saudi arabia" -> "Saudi Pro League · Saudi Arabia"
        "brazil" -> "Brasileirão · Brazil"
        "argentina" -> "Primera División · Argentina"
        "mexico" -> "Liga MX · Mexico"
        "belgium" -> "Pro League · Belgium"
        "scotland" -> "Premiership · Scotland"
        "greece" -> "Super League · Greece"
        "russia" -> "Premier League · Russia"
        "ukraine" -> "Premier League · Ukraine"
        "croatia" -> "HNL · Croatia"
        "switzerland" -> "Super League · Switzerland"
        "denmark" -> "Superliga · Denmark"
        "norway" -> "Eliteserien · Norway"
        "sweden" -> "Allsvenskan · Sweden"
        "czech republic", "czechia" -> "First League · Czech Republic"
        "poland" -> "Ekstraklasa · Poland"
        "japan" -> "J1 League · Japan"
        "south korea", "korea republic" -> "K League · South Korea"
        else -> countryOrCode.trim()
    }
}

private fun clubCountryFlagFile(countryOrCode: String): String {
    val key = countryOrCode.trim().lowercase()
    return when (key) {
        "gb-eng" -> "England.png"
        "es" -> "Spain.png"
        "de" -> "Germany.png"
        "it" -> "Italy.png"
        "fr" -> "France.png"
        "tr" -> "Turkey.png"
        "us", "usa" -> "United States.png"
        "pt" -> "Portugal.png"
        "nl" -> "Netherlands.png"
        else -> clubCountryToFlagFile(countryOrCode)
    }
}

private fun clubToLogoFile(club: String): String {
    val key = club.lowercase()
        .replace(".", "")
        .replace("'", "")
        .replace("-", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    return when (key) {
        "psv eindhoven" -> "PSV Eindhoven.png"
        "slavia prague", "slavia praha" -> "266Slavia Praha.png"
        "braga", "sc braga" -> "1896SC Braga.png"
        "tsg hoffenheim" -> "10029TSG Hoffenheim.png"
        "bayer leverkusen", "leverkusen" -> "32Leverkusen.png"
        "bayern munich", "fc bayern munchen", "fc bayern münchen" -> "21FC Bayern München.png"
        "manchester city", "man city" -> "10Manchester City.png"
        "manchester united", "man utd" -> "11Manchester United.png"
        "liverpool" -> "9Liverpool.png"
        "arsenal" -> "1Arsenal.png"
        "chelsea" -> "5Chelsea.png"
        "real madrid" -> "243Real Madrid.png"
        "barcelona", "fc barcelona" -> "241FC Barcelona.png"
        "atletico madrid", "atletico de madrid", "atlético de madrid" -> "240Atlético de Madrid.png"
        "juventus" -> "45Juventus.png"
        "napoli", "ssc napoli" -> "48SSC Napoli.png"
        "roma", "as roma" -> "52AS Roma.png"
        "psg", "paris sg", "paris saint germain" -> "73Paris SG.png"
        "benfica", "sl benfica" -> "234SL Benfica.png"
        "porto", "fc porto" -> "236FC Porto.png"
        "sporting cp" -> "237Sporting CP.png"
        "galatasaray" -> "325Galatasaray.png"
        "fenerbahce", "fenerbahçe" -> "326Fenerbahçe.png"
        "besiktas", "beşiktaş" -> "327Beşiktaş.png"
        "inter miami", "inter miami cf" -> "112893Inter Miami CF.png"
        else -> ""
    }
}


