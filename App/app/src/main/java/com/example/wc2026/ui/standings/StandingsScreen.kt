package com.carldong.fifa.worldcup2026.ui.standings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carldong.fifa.worldcup2026.data.*
import com.carldong.fifa.worldcup2026.theme.*
import com.carldong.fifa.worldcup2026.ui.components.*
import java.time.LocalDate
import kotlinx.coroutines.launch

// ─── Main Screen ──────────────────────────────────────────────────────────────

@Composable
fun StandingsScreen(
    onTeamClick: (String) -> Unit = {},
    onMatchClick: (String) -> Unit = {},
    vm: StandingsViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

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

    var menuOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(Bg)) {
            NavBar(
                selectedStage = state.selectedStage,
                onPillClick = { menuOpen = !menuOpen }
            )
            AnimatedContent(
                targetState = state.selectedStage,
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInVertically { it / 16 })
                        .togetherWith(fadeOut(tween(120)))
                },
                label = "stageSwitch"
            ) { stage ->
                if (stage == "GS") {
                    GroupStageContent(state, onTeamClick, onMatchClick)
                } else {
                    KnockoutContent(state, onTeamClick = onTeamClick)
                }
            }
        }

        // Tap-outside dismisses menu
        if (menuOpen) {
            Box(
                Modifier.fillMaxSize().clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { menuOpen = false }
            )
        }

        // Stage dropdown
        AnimatedVisibility(
            visible = menuOpen,
            enter = fadeIn(tween(150)) + scaleIn(
                tween(200), initialScale = 0.88f,
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
            ),
            exit = fadeOut(tween(100)) + scaleOut(
                tween(130), targetScale = 0.88f,
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
            ),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 16.dp)
        ) {
            StageDropdown(
                selectedStage = state.selectedStage,
                onSelect = { stage -> vm.selectStage(stage); menuOpen = false }
            )
        }
    }
}

// ─── Nav Bar ──────────────────────────────────────────────────────────────────

@Composable
private fun NavBar(selectedStage: String, onPillClick: () -> Unit) {
    Surface(
        color = WCSurface,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "World Cup 2026",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    letterSpacing = (-0.4).sp,
                    color = Label1
                )
                StagePill(
                    label = if (selectedStage == "GS") "Group Stage" else "Knockout Stage",
                    onClick = onPillClick
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = Separator)
        }
    }
}

@Composable
private fun StagePill(label: String, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .scale(scale.value)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1E787880))
            .clickable {
                scope.launch {
                    scale.animateTo(0.93f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessHigh))
                    scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
                }
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Blue)
            androidx.compose.foundation.Canvas(
                modifier = Modifier.size(width = 10.dp, height = 6.dp)
            ) {
                val w = size.width; val h = size.height
                val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w / 2f, h)
                        lineTo(w, 0f)
                    },
                    color = Blue,
                    style = stroke
                )
            }
        }
    }
}

@Composable
private fun StageDropdown(selectedStage: String, onSelect: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = WCSurface.copy(alpha = 0.97f),
        shadowElevation = 12.dp,
        modifier = Modifier.width(210.dp)
    ) {
        Column {
            StageOption("Group Stage", selectedStage == "GS") { onSelect("GS") }
            HorizontalDivider(thickness = 0.5.dp, color = Separator)
            StageOption("Knockout Stage", selectedStage == "KO") { onSelect("KO") }
        }
    }
}

@Composable
private fun StageOption(label: String, isActive: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label, fontSize = 15.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) Blue else Label1
        )
        if (isActive) {
            Text("✓", fontSize = 15.sp, color = Blue, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Group Stage Content ──────────────────────────────────────────────────────

@Composable
private fun GroupStageContent(state: StandingsUiState, onTeamClick: (String) -> Unit, onMatchClick: (String) -> Unit = {}) {
    val groups = state.groupStandings.keys.sorted()
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val chipScrollState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val scope = rememberCoroutineScope()

    val upcomingMatches = remember(state.matches) { computeUpcomingMatches(state.matches) }
    val hasUpcomingSection = upcomingMatches.isNotEmpty()
    val todayOffset = if (hasUpcomingSection) 1 else 0

    var selectedGroup by rememberSaveable(groups) { mutableStateOf(groups.firstOrNull() ?: "") }

    // Scroll → chip sync
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val idx = listState.firstVisibleItemIndex
        val adjustedIdx = (idx - todayOffset).coerceAtLeast(0)
        val grpIdx = (adjustedIdx / 2).coerceIn(0, groups.size - 1)
        val grp = groups.getOrNull(grpIdx) ?: return@LaunchedEffect
        if (grp != selectedGroup) {
            selectedGroup = grp
            chipScrollState.animateScrollToItem(grpIdx)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Selector chips
        Surface(color = WCSurface, shadowElevation = 0.dp) {
            Column {
                LazyRow(
                    state = chipScrollState,
                    contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groups, key = { it }) { grp ->
                        val isSelected = selectedGroup == grp
                        GroupChip(
                            label = "Group $grp",
                            isSelected = isSelected,
                            onClick = {
                                selectedGroup = grp
                                val grpIdx = groups.indexOf(grp)
                                val itemIdx = todayOffset + grpIdx * 2
                                scope.launch { listState.animateScrollToItem(maxOf(0, itemIdx)) }
                                scope.launch { chipScrollState.animateScrollToItem(grpIdx) }
                            }
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = Separator)
            }
        }

        // Main scrollable content
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            if (hasUpcomingSection) {
                item(key = "upcoming") {
                    UpcomingMatchesSection(upcomingMatches, state.teams, onMatchClick)
                }
            }

            // Group tables
            groups.forEachIndexed { gi, grp ->
                val standings = state.groupStandings[grp] ?: return@forEachIndexed
                val groupMatches = state.matches.filter { it.group == grp && it.stage == "GS" }

                item(key = "hd_$grp") {
                    GroupSectionHeader(group = grp, groupMatches = groupMatches)
                }
                item(key = "tbl_$grp") {
                    GroupTable(standings = standings, onTeamClick = onTeamClick)
                    GroupLegend()
                    if (gi < groups.size - 1) {
                        Spacer(
                            Modifier.fillMaxWidth().height(6.dp).background(Bg)
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun GroupChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .scale(scale.value)
            .width(80.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (isSelected) Blue else Color.Transparent)
            .clickable {
                scope.launch {
                    scale.animateTo(0.90f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessHigh))
                    scale.animateTo(1f, spring(dampingRatio = 0.5f))
                }
                onClick()
            }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) Color.White else Label2,
            textAlign = TextAlign.Center
        )
    }
}

// ─── Upcoming Matches ─────────────────────────────────────────────────────────

@Composable
private fun UpcomingMatchesSection(matches: List<Match>, teams: List<Team>, onCardClick: (String) -> Unit = {}) {
    Column(
        Modifier.fillMaxWidth().background(Bg)
            .padding(top = 14.dp, bottom = 0.dp)
    ) {
        Text(
            "UPCOMING MATCHES",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.7.sp,
            color = Label3,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(start = 14.dp, end = 8.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(matches, key = { it.id }) { m ->
                TodayMatchCard(m, teams, onClick = { onCardClick(m.date) })
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Separator)
    }
}

@Composable
private fun TodayMatchCard(m: Match, teams: List<Team>, onClick: () -> Unit = {}) {
    val t1 = teams.firstOrNull { it.id == m.team1Id }
    val t2 = teams.firstOrNull { it.id == m.team2Id }
    // Show "TBD" whenever a team is not yet determined
    val n1 = t1?.name ?: "TBD"
    val n2 = t2?.name ?: "TBD"
    val isLive = m.status == "live"
    val isFt   = m.status == "ft"
    val stageName = when (m.stage) {
        "GS"    -> if (m.group.isNotEmpty()) "Group ${m.group}" else "Group Stage"
        "R32"   -> "R32"; "R16" -> "R16"
        "QF"    -> "QF"; "SF" -> "SF"
        "3P"    -> "3rd"; "Final" -> "Final"
        else    -> m.stage
    }

    Card(
        modifier = Modifier.width(165.dp).clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = WCSurface)
    ) {
        Column(Modifier.padding(horizontal = 10.dp).padding(top = 11.dp, bottom = 13.dp)) {
            // Status row: time centered in left half | stage centered in right half
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (isLive) LiveDot()
                        Text(
                            text = when {
                                isLive -> m.time.ifEmpty { "LIVE" }
                                isFt   -> "FT"
                                else   -> m.time
                            },
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when { isLive -> Red; isFt -> Gray; else -> Blue }
                        )
                    }
                }
                Spacer(Modifier.width(44.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(stageName, fontSize = 10.sp, color = Label3)
                }
            }
            Spacer(Modifier.height(10.dp))
            // Teams row: each team is flag+name stacked, aligned with status row
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home team: flag circle + name
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    CardFlag(t1?.flagFile?.ifEmpty { t1.imageFile })
                    Text(
                        n1, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        color = Label2, textAlign = TextAlign.Center,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                // Score / vs
                Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
                    if (isLive || isFt) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("${m.homeScore ?: 0}", fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
                            Text(":", fontSize = 16.sp, color = Label3, fontWeight = FontWeight.Light)
                            Text("${m.awayScore ?: 0}", fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
                        }
                    } else {
                        Text("vs", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Label3)
                    }
                }
                // Away team: flag circle + name
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    CardFlag(t2?.flagFile?.ifEmpty { t2.imageFile })
                    Text(
                        n2, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        color = Label2, textAlign = TextAlign.Center,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CardFlag(flagFile: String?) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!flagFile.isNullOrEmpty()) {
            // requiredSize >> container → breaks parent constraint → flag height fills circle
            CountryFlag(
                flagFile = flagFile,
                modifier = Modifier.requiredSize(90.dp)
            )
        }
    }
}

// ─── Group Section ────────────────────────────────────────────────────────────

@Composable
private fun GroupSectionHeader(group: String, groupMatches: List<Match>) {
    val completedCount = groupMatches.count { it.homeScore != null }
    val liveCount = groupMatches.count { it.status == "live" }
    val roundLabel: String? = when {
        liveCount > 0 -> null
        completedCount == 0 -> "Matchday 1"
        completedCount <= 2 -> "Matchday 2"
        completedCount <= 4 -> "Matchday 3"
        else -> "Complete"
    }

    Row(
        Modifier.fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Group $group",
            fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp,
            color = Label1
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (liveCount > 0) {
                Row(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(Red.copy(alpha = 0.12f))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    LiveDot()
                    Text("LIVE", fontSize = 9.5.sp, color = Red, fontWeight = FontWeight.Bold)
                }
            }
            roundLabel?.let {
                Text(it, fontSize = 12.sp, color = Label3)
            }
        }
    }
}

@Composable
private fun GroupTable(standings: List<TeamStanding>, onTeamClick: (String) -> Unit) {
    Surface(
        modifier = Modifier.padding(horizontal = 14.dp),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        color = WCSurface
    ) {
        Column {
            // Header row
            Row(
                Modifier.fillMaxWidth()
                    .background(SurfaceElevated)
                    .padding(start = 10.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(3.dp))  // qualify bar placeholder
                Spacer(Modifier.width(4.dp))
                Spacer(Modifier.width(14.dp)) // pos
                Spacer(Modifier.width(4.dp))
                Spacer(Modifier.width(26.dp)) // flag
                Spacer(Modifier.width(8.dp))
                Text(
                    "TEAM", Modifier.weight(1f),
                    fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    color = Label3, letterSpacing = 0.4.sp
                )
                HeaderCell("RK", 30.dp)
                HeaderCell("P",  22.dp)
                HeaderCell("W",  22.dp)
                HeaderCell("D",  22.dp)
                HeaderCell("L",  22.dp)
                HeaderCell("GD", 26.dp)
                HeaderCell("PTS", 24.dp, bold = true)
                Spacer(Modifier.width(11.dp)) // chevron placeholder
            }
            HorizontalDivider(thickness = 0.5.dp, color = Separator)
            standings.forEachIndexed { idx, s ->
                GroupTeamRow(pos = idx + 1, s = s, onTeamClick = onTeamClick)
                if (idx < standings.lastIndex) {
                    HorizontalDivider(thickness = 0.5.dp, color = Separator)
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(
    label: String, width: Dp,
    bold: Boolean = false
) {
    Text(
        label,
        modifier = Modifier.width(width),
        fontSize = 11.sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium,
        color = if (bold) Label2 else Label3,
        textAlign = TextAlign.Center,
        letterSpacing = 0.4.sp
    )
}

@Composable
private fun GroupTeamRow(pos: Int, s: TeamStanding, onTeamClick: (String) -> Unit) {
    val isWithdrawn = s.team.isWithdrawn
    val qualifies = pos <= 2 && !isWithdrawn
    val rowBg = if (qualifies) Green.copy(alpha = 0.04f) else Color.Transparent

    Row(
        Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable { onTeamClick(s.team.id) }
            .padding(start = 10.dp, end = 12.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Qualify indicator bar
        if (qualifies) {
            Box(
                Modifier.width(3.dp).height(34.dp)
                    .clip(RoundedCornerShape(2.dp)).background(Green)
            )
        } else {
            Spacer(Modifier.width(3.dp))
        }
        Spacer(Modifier.width(4.dp))

        // Position
        Text(
            "$pos",
            modifier = Modifier.width(14.dp),
            fontSize = 11.5.sp, fontWeight = FontWeight.Medium,
            color = Label3, textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(4.dp))

        // Country flag
        val flagFile = s.team.flagFile.ifEmpty { s.team.imageFile }
        CountryFlag(
            flagFile = flagFile,
            modifier = Modifier.width(26.dp).height(18.dp).clip(RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.width(8.dp))

        // Team name
        Text(
            s.team.name,
            modifier = Modifier.weight(1f),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (isWithdrawn) Label2 else Label1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = (-0.2).sp
        )

        // FIFA rank
        Text(
            "${s.team.fifaRank}",
            modifier = Modifier.width(30.dp),
            fontSize = 11.sp, fontWeight = FontWeight.Medium,
            color = Label3, textAlign = TextAlign.Center
        )

        // Stats
        StatCell(s.played)
        StatCell(s.won)
        StatCell(s.drawn)
        StatCell(s.lost)
        GDCell(s.goalDiff)

        // Points
        Text(
            s.points.toString(),
            modifier = Modifier.width(24.dp),
            fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = Label1, textAlign = TextAlign.Center
        )

        // Chevron
        Text(
            "›",
            fontSize = 13.sp,
            color = Label1.copy(alpha = 0.2f),
            modifier = Modifier.width(11.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatCell(v: Int) {
    Text(
        v.toString(),
        modifier = Modifier.width(22.dp),
        fontSize = 13.sp, color = Label2,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun GDCell(gd: Int) {
    val text  = if (gd > 0) "+$gd" else gd.toString()
    val color = when { gd > 0 -> Color(0xFF1C7A3E); gd < 0 -> Red; else -> Label2 }
    Text(
        text,
        modifier = Modifier.width(26.dp),
        fontSize = 13.sp,
        color = color,
        fontWeight = if (gd != 0) FontWeight.Medium else FontWeight.Normal,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun GroupLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                Modifier.width(3.dp).height(11.dp)
                    .clip(RoundedCornerShape(2.dp)).background(Green)
            )
            Text("Qualify to Round of 32", fontSize = 10.5.sp, color = Label3)
        }
    }
}

// ─── Knockout Stage Content ───────────────────────────────────────────────────

private val KO_STAGES = listOf(
    "R32"   to "Round of 32",
    "R16"   to "Round of 16",
    "QF"    to "Quarter-finals",
    "SF"    to "Semi-finals",
    "3P"    to "Third Place",
    "Final" to "Final"
)

@Composable
private fun KnockoutContent(state: StandingsUiState, onTeamClick: (String) -> Unit = {}) {
    val rounds = remember(state.matches) {
        KO_STAGES.filter { (code, _) -> state.matches.any { it.stage == code } }
    }
    val stageMatchesMap = remember(state.matches, rounds) {
        rounds.associate { (code, _) -> code to state.matches.filter { it.stage == code } }
    }

    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val chipScrollState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val scope = rememberCoroutineScope()

    var selectedRound by rememberSaveable(rounds) { mutableStateOf(rounds.firstOrNull()?.first ?: "") }

    // Pre-compute item offsets for each round
    val roundStartOffsets = remember(rounds, stageMatchesMap) {
        buildMap {
            var offset = 0
            rounds.forEach { (code, _) ->
                put(code, offset)
                offset += 1 + (stageMatchesMap[code]?.size ?: 0)
            }
        }
    }

    // Scroll → chip sync
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val visIdx = listState.firstVisibleItemIndex
        var cur = rounds.firstOrNull()?.first ?: return@LaunchedEffect
        for ((code, _) in rounds) {
            val start = roundStartOffsets[code] ?: continue
            if (visIdx >= start) cur = code else break
        }
        if (cur != selectedRound) {
            selectedRound = cur
            val ri = rounds.indexOfFirst { it.first == cur }
            if (ri >= 0) chipScrollState.animateScrollToItem(ri)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Round chips
        Surface(color = WCSurface, shadowElevation = 0.dp) {
            Column {
                LazyRow(
                    state = chipScrollState,
                    contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rounds, key = { it.first }) { (code, _) ->
                        val isSelected = selectedRound == code
                        KoChip(
                            label = code,
                            isSelected = isSelected,
                            onClick = {
                                selectedRound = code
                                val itemIdx = roundStartOffsets[code] ?: 0
                                val ri = rounds.indexOfFirst { it.first == code }
                                scope.launch { listState.animateScrollToItem(itemIdx) }
                                if (ri >= 0) scope.launch { chipScrollState.animateScrollToItem(ri) }
                            }
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = Separator)
            }
        }

        // Match list
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            rounds.forEach { (code, name) ->
                val stageMatches = stageMatchesMap[code] ?: emptyList()
                val dates = stageMatches.map { it.date }.distinct().sorted()
                val dateRange = when {
                    dates.isEmpty() -> ""
                    dates.size == 1 -> formatDate(dates.first())
                    else -> "${formatDate(dates.first())} – ${formatDate(dates.last())}"
                }

                item(key = "ko_hd_$code") {
                    KoRoundHeader(name = name, dateRange = dateRange)
                }
                stageMatches.forEach { m ->
                    item(key = "ko_${m.id}") {
                        KoMatchCard(m = m, teams = state.teams)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun KoChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .scale(scale.value)
            .width(60.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (isSelected) Blue else Color.Transparent)
            .clickable {
                scope.launch {
                    scale.animateTo(0.90f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessHigh))
                    scale.animateTo(1f, spring(dampingRatio = 0.5f))
                }
                onClick()
            }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) Color.White else Label2,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun KoRoundHeader(name: String, dateRange: String) {
    Row(
        Modifier.fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            name,
            fontSize = 20.sp, fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp, color = Label1
        )
        if (dateRange.isNotEmpty()) {
            Text(dateRange, fontSize = 12.sp, color = Label3)
        }
    }
}

@Composable
private fun KoMatchCard(m: Match, teams: List<Team>) {
    val t1 = teams.firstOrNull { it.id == m.team1Id }
    val t2 = teams.firstOrNull { it.id == m.team2Id }
    // Always show "TBD" when team not yet determined (don't show qualifier descriptions)
    val n1 = t1?.name ?: "TBD"
    val n2 = t2?.name ?: "TBD"

    val isLive = m.status == "live"
    val isFt   = m.status == "ft"

    val homeScore = m.homeScore ?: 0
    val awayScore = m.awayScore ?: 0
    val hasPens = m.penaltyHomeScore != null && m.penaltyAwayScore != null
    val t1Wins = isFt && (homeScore > awayScore ||
        (homeScore == awayScore && hasPens && (m.penaltyHomeScore ?: 0) > (m.penaltyAwayScore ?: 0)))
    val t2Wins = isFt && (awayScore > homeScore ||
        (homeScore == awayScore && hasPens && (m.penaltyAwayScore ?: 0) > (m.penaltyHomeScore ?: 0)))

    val stageLabelColor = when (m.stage) {
        "Final" -> Orange
        "SF", "QF" -> Purple
        else -> Label3
    }
    val stageDisplayName = when (m.stage) {
        "R32"   -> "ROUND OF 32"; "R16" -> "ROUND OF 16"
        "QF"    -> "QUARTER-FINAL"; "SF" -> "SEMI-FINAL"
        "3P"    -> "3RD PLACE"; "Final" -> "FINAL"
        else    -> m.stage.uppercase()
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        color = WCSurface
    ) {
        Column {
            // Card header
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stageDisplayName,
                    fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold,
                    color = stageLabelColor, letterSpacing = 0.5.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isLive) LiveDot()
                    Text(
                        text = when {
                            isLive -> "LIVE"
                            isFt   -> "Full Time"
                            else   -> "${formatDate(m.date)}  ${m.time}"
                        },
                        fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold,
                        color = when { isLive -> Red; isFt -> Gray; else -> Blue }
                    )
                }
            }

            // Teams + score row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team 1
                KoTeamColumn(
                    team = t1, name = n1,
                    isWinner = t1Wins, isLoser = t2Wins && !t1Wins,
                    modifier = Modifier.weight(1f)
                )

                // Center: score or time
                Box(Modifier.width(72.dp), contentAlignment = Alignment.Center) {
                    when {
                        isLive -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    "${m.homeScore ?: 0}", fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold, color = Red,
                                    letterSpacing = (-1).sp
                                )
                                Text(":", fontSize = 18.sp, color = Label3, fontWeight = FontWeight.Light)
                                Text(
                                    "${m.awayScore ?: 0}", fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold, color = Red,
                                    letterSpacing = (-1).sp
                                )
                            }
                            Text(
                                "LIVE", fontSize = 9.5.sp,
                                color = Red, fontWeight = FontWeight.SemiBold
                            )
                        }
                        isFt -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    "$homeScore", fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (t1Wins) Label1 else Label3,
                                    letterSpacing = (-1).sp
                                )
                                Text(":", fontSize = 18.sp, color = Label3, fontWeight = FontWeight.Light)
                                Text(
                                    "$awayScore", fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (t2Wins) Label1 else Label3,
                                    letterSpacing = (-1).sp
                                )
                            }
                            if (hasPens) {
                                Text(
                                    "P ${m.penaltyHomeScore}-${m.penaltyAwayScore}",
                                    fontSize = 9.5.sp,
                                    color = Label3,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                m.time, fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp,
                                color = Label1
                            )
                            if (m.date.isNotEmpty()) {
                                Text(
                                    formatDate(m.date),
                                    fontSize = 10.sp, color = Label3
                                )
                            }
                        }
                    }
                }

                // Team 2
                KoTeamColumn(
                    team = t2, name = n2,
                    isWinner = t2Wins, isLoser = t1Wins && !t2Wins,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun KoTeamColumn(
    team: Team?, name: String,
    isWinner: Boolean, isLoser: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (team != null) {
            val flagModifier = Modifier
                .width(32.dp).height(22.dp)
                .clip(RoundedCornerShape(3.dp))
                .alpha(if (isLoser) 0.5f else 1f)
                .let {
                    if (isWinner) it.border(1.5.dp, Green, RoundedCornerShape(3.dp)) else it
                }
            CountryFlag(
                flagFile = team.flagFile.ifEmpty { team.imageFile },
                modifier = flagModifier
            )
        } else {
            Box(
                Modifier.width(32.dp).height(22.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Separator)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            name,
            fontSize = 13.sp,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
            color = if (isLoser) Label3 else Label1,
            textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            letterSpacing = (-0.2).sp
        )
        if (team != null) {
            Text(
                "#${team.fifaRank}",
                fontSize = 10.sp, color = Label3
            )
        }
    }
}

// ─── Utility ──────────────────────────────────────────────────────────────────

private fun computeUpcomingMatches(matches: List<Match>): List<Match> {
    val upcoming = matches
        .filter { it.status == "upcoming" && it.date.isNotEmpty() }
        .sortedWith(compareBy({ it.date }, { it.time }))
    val first = upcoming.firstOrNull() ?: return emptyList()
    return upcoming.filter { it.date == first.date }.take(5)
}


private fun formatDate(isoDate: String): String {
    if (isoDate.length < 10) return isoDate
    val months = listOf(
        "", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    return try {
        val month = isoDate.substring(5, 7).toInt()
        val day   = isoDate.substring(8, 10).toInt()
        "${months[month]} $day"
    } catch (_: Exception) { isoDate }
}

