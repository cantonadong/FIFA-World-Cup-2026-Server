package com.carldong.fifa.worldcup2026.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.carldong.fifa.worldcup2026.R
import com.carldong.fifa.worldcup2026.data.DefaultDataRepository
import com.carldong.fifa.worldcup2026.data.Player
import com.carldong.fifa.worldcup2026.theme.*
import com.carldong.fifa.worldcup2026.ui.players.PlayersScreen
import com.carldong.fifa.worldcup2026.ui.schedule.ScheduleScreen
import com.carldong.fifa.worldcup2026.ui.standings.StandingsScreen
import com.carldong.fifa.worldcup2026.ui.teams.TeamsScreen
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class Tab(
    val label: String,
    @DrawableRes val icon: Int
) {
    Standings("Standings", R.drawable.ic_tab_standings),
    Schedule("Schedule", R.drawable.ic_tab_schedule),
    Teams("Teams", R.drawable.ic_tab_teams),
    Players("Players", R.drawable.ic_tab_players),
}

@Composable
fun AppScaffold() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val refreshThresholdPx = 100f
    var pullDistance by remember { mutableFloatStateOf(0f) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(Tab.Standings) }
    var pendingTeamId by remember { mutableStateOf<String?>(null) }
    var pendingMatchDate by remember { mutableStateOf<LocalDate?>(null) }
    var pendingPlayerId by remember { mutableStateOf<Int?>(null) }
    var pendingPlayer by remember { mutableStateOf<Player?>(null) }
    var returnToTeamsFromPlayer by remember { mutableStateOf(false) }
    var homeResetToken by remember { mutableIntStateOf(0) }
    val refreshEnabled = selectedTab == Tab.Standings || selectedTab == Tab.Schedule
    fun startRefresh() {
        if (isRefreshing || !refreshEnabled) return
        isRefreshing = true
        scope.launch {
            DefaultDataRepository(context).refreshMatches()
            isRefreshing = false
        }
    }
    LaunchedEffect(refreshEnabled) {
        if (!refreshEnabled) pullDistance = 0f
    }
    val refreshConnection = remember(refreshThresholdPx, isRefreshing, refreshEnabled) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!refreshEnabled) return Offset.Zero
                if (source == NestedScrollSource.UserInput && available.y > 0f && !isRefreshing) {
                    pullDistance = (pullDistance + available.y).coerceAtMost(refreshThresholdPx * 1.8f)
                } else if (available.y < 0f) {
                    pullDistance = (pullDistance + available.y).coerceAtLeast(0f)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (refreshEnabled && !isRefreshing && pullDistance >= refreshThresholdPx) {
                    startRefresh()
                }
                pullDistance = 0f
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (refreshEnabled && !isRefreshing && pullDistance >= refreshThresholdPx) {
                    startRefresh()
                }
                pullDistance = 0f
                return Velocity.Zero
            }
        }
    }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            AppTabBar(
                selected = selectedTab,
                onSelect = {
                    pendingTeamId = null
                    pendingMatchDate = null
                    pendingPlayerId = null
                    pendingPlayer = null
                    returnToTeamsFromPlayer = false
                    selectedTab = it
                    homeResetToken++
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(refreshConnection)
                .pointerInput(isRefreshing) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                        if (refreshEnabled && !isRefreshing && pullDistance >= refreshThresholdPx) {
                            startRefresh()
                        }
                        pullDistance = 0f
                    }
                }
        ) {
            val contentOffset = if (!refreshEnabled) 0f else if (isRefreshing) 52f else pullDistance.coerceAtMost(refreshThresholdPx * 1.25f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = contentOffset }
            ) {
                when (selectedTab) {
                    Tab.Standings -> StandingsScreen(
                        onTeamClick = { id -> pendingTeamId = id; selectedTab = Tab.Teams },
                        onMatchClick = { isoDate ->
                            try { pendingMatchDate = LocalDate.parse(isoDate); selectedTab = Tab.Schedule }
                            catch (_: Exception) {}
                        }
                    )
                    Tab.Schedule  -> ScheduleScreen(
                        pendingDate = pendingMatchDate,
                        onPendingConsumed = { pendingMatchDate = null },
                        homeResetToken = homeResetToken
                    )
                    Tab.Teams     -> TeamsScreen(
                        pendingTeamId = pendingTeamId,
                        onPendingConsumed = { pendingTeamId = null },
                        homeResetToken = homeResetToken,
                        onPlayerClick = { playerId ->
                            pendingPlayerId = playerId
                            pendingPlayer = null
                            returnToTeamsFromPlayer = true
                            selectedTab = Tab.Players
                        },
                        onPlayerFullClick = { player ->
                            pendingPlayer = player
                            pendingPlayerId = null
                            returnToTeamsFromPlayer = true
                            selectedTab = Tab.Players
                        }
                    )
                    Tab.Players   -> PlayersScreen(
                        pendingPlayerId = pendingPlayerId,
                        pendingPlayer = pendingPlayer,
                        onPendingConsumed = { pendingPlayerId = null; pendingPlayer = null },
                        homeResetToken = homeResetToken,
                        onDetailClosed = {
                            if (returnToTeamsFromPlayer) {
                                returnToTeamsFromPlayer = false
                                selectedTab = Tab.Teams
                            }
                        }
                    )
                }
            }
            PullRefreshIndicator(
                pullDistance = if (refreshEnabled) pullDistance else 0f,
                threshold = refreshThresholdPx,
                isRefreshing = refreshEnabled && isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter).zIndex(2f)
            )
        }
    }
}

@Composable
private fun PullRefreshIndicator(
    pullDistance: Float,
    threshold: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = (pullDistance / threshold).coerceIn(0f, 1f)
    val visible = isRefreshing || pullDistance > 6f
    val height by animateFloatAsState(
        targetValue = if (isRefreshing) 52f else 52f * progress,
        animationSpec = tween(160),
        label = "pullRefreshHeight"
    )
    val releaseReady = progress >= 1f
    val infinite = rememberInfiniteTransition(label = "refreshSpin")
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refreshSpin"
    )

    AnimatedVisibility(visible = visible, modifier = modifier.fillMaxWidth()) {
        Surface(
            color = WCSurface.copy(alpha = 0.96f),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .alpha(if (isRefreshing) 1f else progress.coerceAtLeast(0.35f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Blue
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = if (releaseReady) Blue else Label3,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(progress * 180f)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        isRefreshing -> "Updating match data..."
                        releaseReady -> "Release to update data"
                        else -> "Pull down to refresh"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRefreshing || releaseReady) Blue else Label3
                )
            }
        }
    }
}

@Composable
private fun AppTabBar(
    selected: Tab,
    onSelect: (Tab) -> Unit
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column {
        HorizontalDivider(thickness = 0.5.dp, color = Separator)
        // 56dp 的 tab 区域 + 导航条高度的底部空白，分开处理以确保图标在 56dp 内水平垂直居中
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(WCSurface.copy(alpha = 0.96f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
            ) {
                Tab.entries.forEach { tab ->
                    val isSelected = tab == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onSelect(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(top = 11.dp, bottom = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(tab.icon),
                                contentDescription = tab.label,
                                modifier = Modifier.size(24.dp),
                                tint = if (isSelected) Blue else Gray
                            )
                            Text(
                                text = tab.label,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) Blue else Gray
                            )
                        }
                    }
                }
            }
            // 导航条底部安全区域
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navBarPadding)
            )
        }
    }
}

