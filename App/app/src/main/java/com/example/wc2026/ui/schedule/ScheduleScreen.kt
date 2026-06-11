package com.carldong.fifa.worldcup2026.ui.schedule

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carldong.fifa.worldcup2026.R
import com.carldong.fifa.worldcup2026.data.Match
import com.carldong.fifa.worldcup2026.data.Team
import com.carldong.fifa.worldcup2026.data.Venue
import com.carldong.fifa.worldcup2026.data.googleCalendarUtcRange
import com.carldong.fifa.worldcup2026.data.kickoffInstant
import com.carldong.fifa.worldcup2026.data.localKickoffDateLabel
import com.carldong.fifa.worldcup2026.data.localKickoffTimeLabel
import com.carldong.fifa.worldcup2026.theme.*
import com.carldong.fifa.worldcup2026.ui.components.CountryFlag
import com.carldong.fifa.worldcup2026.ui.components.LiveDot
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val FALLBACK_SCHED_START = LocalDate.of(2026, 6, 11)
private val FALLBACK_SCHED_END = LocalDate.of(2026, 7, 19)

private fun scheduleDates(start: LocalDate, end: LocalDate): List<LocalDate> {
    val list = mutableListOf<LocalDate>()
    var d = start
    while (!d.isAfter(end)) {
        list.add(d)
        d = d.plusDays(1)
    }
    return list
}

@Composable
fun ScheduleScreen(
    pendingDate: LocalDate? = null,
    onPendingConsumed: () -> Unit = {},
    homeResetToken: Int = 0
) {
    val vm: ScheduleViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    val chipListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val contentListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }
    var lastHomeResetToken by rememberSaveable { mutableIntStateOf(homeResetToken) }
    val scheduleStart = state.matchDates.minOrNull() ?: FALLBACK_SCHED_START
    val scheduleEnd = state.matchDates.maxOrNull() ?: FALLBACK_SCHED_END

    LaunchedEffect(homeResetToken) {
        if (homeResetToken != lastHomeResetToken) {
            lastHomeResetToken = homeResetToken
            showDatePicker = false
        }
    }

    // Handle pending date navigation (from other tabs)
    LaunchedEffect(pendingDate, state.isLoading) {
        if (!state.isLoading && pendingDate != null) {
            vm.selectDate(pendingDate)
            onPendingConsumed()
            val groupIdx = state.dateGroups.indexOfFirst { it.date >= pendingDate }
            if (groupIdx >= 0) contentListState.scrollToItem(groupIdx)
            val chipIdx = ChronoUnit.DAYS.between(scheduleStart, pendingDate).toInt()
            if (chipIdx >= 0) chipListState.scrollToItem(maxOf(0, chipIdx - 2))
        }
    }

    // Initial scroll to selected date when data loads
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading && state.dateGroups.isNotEmpty()) {
            val groupIdx = state.dateGroups.indexOfFirst { it.date == state.selectedDate }
            if (groupIdx >= 0) contentListState.scrollToItem(groupIdx)
            val chipIdx = ChronoUnit.DAYS.between(scheduleStart, state.selectedDate).toInt()
            if (chipIdx >= 0) chipListState.scrollToItem(maxOf(0, chipIdx - 2))
        }
    }

    // Content scroll → chip sync
    LaunchedEffect(contentListState.firstVisibleItemIndex, state.dateGroups) {
        if (state.isLoading || state.dateGroups.isEmpty()) return@LaunchedEffect
        val group = state.dateGroups.getOrNull(contentListState.firstVisibleItemIndex)
            ?: return@LaunchedEffect
        vm.selectDate(group.date)
        val chipIdx = ChronoUnit.DAYS.between(scheduleStart, group.date).toInt().coerceAtLeast(0)
        chipListState.animateScrollToItem(maxOf(0, chipIdx - 2))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScheduleTopBar(
                selectedDate = state.selectedDate,
                onDatePickerClick = { showDatePicker = true }
            )
            DateBar(
                matchDates = state.matchDates,
                scheduleStart = scheduleStart,
                scheduleEnd = scheduleEnd,
                selectedDate = state.selectedDate,
                chipListState = chipListState,
                onChipClick = { date ->
                    vm.selectDate(date)
                    scope.launch {
                        val chipIdx = ChronoUnit.DAYS.between(scheduleStart, date).toInt()
                        if (chipIdx >= 0) chipListState.animateScrollToItem(maxOf(0, chipIdx - 2))
                        val groupIdx = state.dateGroups.indexOfFirst { it.date >= date }
                        if (groupIdx >= 0) contentListState.animateScrollToItem(groupIdx)
                    }
                }
            )
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Blue)
                }
            } else {
                LazyColumn(state = contentListState, modifier = Modifier.fillMaxSize()) {
                    items(state.dateGroups, key = { it.date.toString() }) { group ->
                        DateGroupSection(group, state.teamMap, state.venueMap)
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }

        if (showDatePicker) {
            DatePickerSheet(
                matchDates = state.matchDates,
                scheduleStart = scheduleStart,
                scheduleEnd = scheduleEnd,
                selectedDate = state.selectedDate,
                onDateSelected = { date ->
                    showDatePicker = false
                    vm.selectDate(date)
                    scope.launch {
                        val groupIdx = state.dateGroups.indexOfFirst { it.date >= date }
                        if (groupIdx >= 0) contentListState.animateScrollToItem(groupIdx)
                        val chipIdx = ChronoUnit.DAYS.between(scheduleStart, date).toInt()
                        if (chipIdx >= 0) chipListState.animateScrollToItem(maxOf(0, chipIdx - 2))
                    }
                },
                onDismiss = { showDatePicker = false }
            )
        }
    }
}

// ── Top Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun ScheduleTopBar(selectedDate: LocalDate, onDatePickerClick: () -> Unit) {
    Surface(color = WCSurface, shadowElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Schedule",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Label1,
                    letterSpacing = (-0.4).sp
                )
                DatePickerButton(selectedDate, onDatePickerClick)
            }
            HorizontalDivider(thickness = 0.5.dp, color = Separator)
        }
    }
}

@Composable
private fun DatePickerButton(selectedDate: LocalDate, onClick: () -> Unit) {
    val monthAbbr = selectedDate.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    val day = selectedDate.dayOfMonth
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "dpScale"
    )
    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1F787880))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tab_schedule),
            contentDescription = null,
            tint = Blue,
            modifier = Modifier.size(14.dp)
        )
        Text(text = "$monthAbbr $day", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Blue)
        Canvas(modifier = Modifier.size(10.dp, 6.dp)) {
            val sw = 1.3.dp.toPx()
            val path = Path().apply {
                moveTo(1.dp.toPx(), 1.dp.toPx())
                lineTo(size.width / 2, size.height - 1.dp.toPx())
                lineTo(size.width - 1.dp.toPx(), 1.dp.toPx())
            }
            drawPath(path, Blue, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

// ── Date Bar ────────────────────────────────────────────────────────────────

@Composable
private fun DateBar(
    matchDates: Set<LocalDate>,
    scheduleStart: LocalDate,
    scheduleEnd: LocalDate,
    selectedDate: LocalDate,
    chipListState: LazyListState,
    onChipClick: (LocalDate) -> Unit
) {
    val monthLabel = remember(selectedDate) {
        selectedDate.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase() + " 2026"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xECFFFFFF))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            AnimatedContent(
                targetState = monthLabel,
                transitionSpec = {
                    (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 4 } + fadeOut())
                },
                label = "monthLbl"
            ) { label ->
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Label2,
                    letterSpacing = 0.4.sp
                )
            }
        }
        LazyRow(
            state = chipListState,
            contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(scheduleDates(scheduleStart, scheduleEnd), key = { it.toString() }) { date ->
                DateChip(
                    date = date,
                    isSelected = date == selectedDate,
                    hasMatch = date in matchDates,
                    onClick = { onChipClick(date) }
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Separator)
    }
}

@Composable
private fun DateChip(
    date: LocalDate,
    isSelected: Boolean,
    hasMatch: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "chipScale"
    )
    val dayAbbr = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(3).uppercase()

    Column(
        modifier = Modifier
            .scale(scale)
            .width(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Blue else Color.Transparent)
            .alpha(if (!hasMatch && !isSelected) 0.4f else 1f)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dayAbbr,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else Label3,
            letterSpacing = 0.3.sp
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = "${date.dayOfMonth}",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else Label1,
            letterSpacing = (-0.5).sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(2.dp))
        if (hasMatch) {
            Box(
                Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White.copy(alpha = 0.65f) else Blue)
            )
        } else {
            Spacer(Modifier.size(4.dp))
        }
    }
}

// ── Date Group Section ───────────────────────────────────────────────────────

@Composable
private fun DateGroupSection(
    group: ScheduleDateGroup,
    teamMap: Map<String, Team>,
    venueMap: Map<String, Venue>
) {
    val date = group.date
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    val monthName = date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

    Column(modifier = Modifier.padding(top = 18.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "$dayName, $monthName ${date.dayOfMonth}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Label1,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = "${group.matches.size} match${if (group.matches.size > 1) "es" else ""}",
                fontSize = 12.sp,
                color = Label3
            )
        }
        group.matches.forEach { match ->
            MatchCard(
                match = match,
                team1 = if (match.team1Id.isNotEmpty()) teamMap[match.team1Id] else null,
                team2 = if (match.team2Id.isNotEmpty()) teamMap[match.team2Id] else null,
                venue = venueMap[match.venueId]
            )
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Bg)
        )
    }
}

// ── Match Card ───────────────────────────────────────────────────────────────

@Composable
private fun MatchCard(match: Match, team1: Team?, team2: Team?, venue: Venue?) {
    val context = LocalContext.current
    val isLive = match.status == "live"
    val isFt = match.status == "ft"
    val isUpcoming = match.status == "upcoming"

    val stageLabel = when (match.stage) {
        "GS"    -> "Group ${match.group}"
        "R32"   -> "Round of 32"
        "R16"   -> "Round of 16"
        "QF"    -> "Quarter-final"
        "SF"    -> "Semi-final"
        "3P"    -> "3rd Place"
        "Final" -> "Final"
        else    -> match.stageName
    }
    val stageLabelColor = when (match.stage) {
        "Final" -> Orange
        "GS"    -> Label3
        else    -> Purple
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(16.dp),
        color = WCSurface,
        shadowElevation = 1.dp,
        border = if (isLive) BorderStroke(1.dp, Red.copy(alpha = 0.22f)) else null
    ) {
        Column {
            // Row 1: stage label + badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stageLabel.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = stageLabelColor,
                    letterSpacing = 0.4.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLive) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Red.copy(alpha = 0.1f))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LiveDot()
                            Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Red)
                        }
                    }
                    if (isUpcoming) {
                        CalButton(match, team1, team2, venue)
                    }
                }
            }

            // Row 2: teams + score/time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MatchTeamBlock(team1, Modifier.weight(1f))
                MatchCenterBlock(match, Modifier.width(72.dp))
                MatchTeamBlock(team2, Modifier.weight(1f))
            }

            // Row 3: venue
            HorizontalDivider(thickness = 0.5.dp, color = Separator)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = venue != null,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (venue != null) {
                            val query = Uri.encode("${venue.name}, ${venue.city}, ${venue.country}")
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query"))
                            )
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Map pin icon matching tab2.html SVG (viewBox 10×13)
                    Canvas(Modifier.size(9.dp, 12.dp)) {
                        val sx = size.width / 10f
                        val sy = size.height / 13f
                        val path = Path().apply {
                            moveTo(5f * sx, 1f * sy)
                            cubicTo(3.07f * sx, 1f * sy, 1.5f * sx, 2.57f * sy, 1.5f * sx, 4.5f * sy)
                            cubicTo(1.5f * sx, 7.3f * sy, 5f * sx, 11.3f * sy, 5f * sx, 11.3f * sy)
                            cubicTo(5f * sx, 11.3f * sy, 8.5f * sx, 7.3f * sy, 8.5f * sx, 4.5f * sy)
                            cubicTo(8.5f * sx, 2.57f * sy, 6.93f * sx, 1f * sy, 5f * sx, 1f * sy)
                            close()
                        }
                        drawPath(path, Blue.copy(alpha = 0.12f))
                        drawPath(path, Blue, style = Stroke(1.1.dp.toPx()))
                        drawCircle(Blue, radius = 1.3f * minOf(sx, sy), center = Offset(5f * sx, 4.5f * sy))
                    }
                    Text(
                        text = venue?.name ?: "",
                        fontSize = 11.5.sp,
                        color = Blue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (venue != null) "${venue.city}, ${venue.country}" else "",
                    fontSize = 11.5.sp,
                    color = Label2,
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun MatchTeamBlock(team: Team?, modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        MatchFlag(team?.flagFile)
        Text(
            text = team?.name ?: "TBD",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = Label1,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp
        )
        Text(
            text = if (team != null) "#${team.fifaRank}" else "",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Label3,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MatchFlag(flagFile: String?) {
    Box(
        modifier = Modifier
            .size(48.dp, 32.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(0.5.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!flagFile.isNullOrEmpty()) {
            CountryFlag(
                flagFile = flagFile,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun MatchCenterBlock(match: Match, modifier: Modifier) {
    val isLive = match.status == "live"
    val isFt = match.status == "ft"
    val timeLabel = remember(match.date, match.time) { match.localKickoffTimeLabel() }
    val dateLabel = remember(match.date, match.time) { match.localKickoffDateLabel() }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (isLive || isFt) {
            val hasPens = match.penaltyHomeScore != null && match.penaltyAwayScore != null
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${match.homeScore ?: 0}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLive) Red else Label1,
                    letterSpacing = (-1).sp
                )
                Text(":", fontSize = 18.sp, fontWeight = FontWeight.Light, color = Label3)
                Text(
                    text = "${match.awayScore ?: 0}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLive) Red else Label1,
                    letterSpacing = (-1).sp
                )
            }
            if (hasPens) {
                Text(
                    text = "P ${match.penaltyHomeScore}-${match.penaltyAwayScore}",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Label3
                )
            }
        } else {
            // Time in a fixed-height box matching flag height (32dp) → aligns with flag center
            Box(modifier = Modifier.height(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = timeLabel,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Label1,
                    letterSpacing = (-0.5).sp
                )
            }
            Text(
                text = dateLabel,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = Label3,
                letterSpacing = 0.4.sp
            )
        }
    }
}

@Composable
private fun CalButton(match: Match, team1: Team?, team2: Team?, venue: Venue?) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0x1F787880))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showDialog = true },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tab_schedule),
            contentDescription = null,
            tint = Blue,
            modifier = Modifier.size(13.dp)
        )
    }

    if (showDialog) {
        val title = "${team1?.name ?: "TBD"} vs ${team2?.name ?: "TBD"}"
        val locationStr = "${venue?.name ?: ""}, ${venue?.city ?: ""}"
        val startMs = match.kickoffInstant()?.toEpochMilli() ?: 0L
        val googleRange = match.googleCalendarUtcRange()

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = WCSurface,
            shape = RoundedCornerShape(18.dp),
            title = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "Add to Calendar",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Label1,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        title,
                        fontSize = 13.sp,
                        color = Label2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        textAlign = TextAlign.Center
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Separator)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDialog = false
                                if (startMs > 0L) {
                                    val intent = Intent(Intent.ACTION_INSERT).apply {
                                        data = CalendarContract.Events.CONTENT_URI
                                        putExtra(CalendarContract.Events.TITLE, title)
                                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMs)
                                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMs + 7200000L)
                                        putExtra(CalendarContract.Events.EVENT_LOCATION, locationStr)
                                        putExtra(CalendarContract.Events.DESCRIPTION, "FIFA World Cup 2026")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF3DDC84)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_tab_schedule),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                "Add to Calendar",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Label1,
                                textAlign = TextAlign.Start
                            )
                            Text(
                                "Opens your default calendar app",
                                fontSize = 11.5.sp,
                                color = Label3,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Separator)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDialog = false
                                googleRange?.let { (startDateStr, endDateStr) ->
                                    val url = "https://www.google.com/calendar/render?action=TEMPLATE" +
                                        "&text=${Uri.encode(title)}" +
                                        "&dates=$startDateStr/$endDateStr" +
                                        "&location=${Uri.encode(locationStr)}" +
                                        "&details=${Uri.encode("FIFA World Cup 2026")}"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEA4335)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                "Google Calendar",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Label1,
                                textAlign = TextAlign.Start
                            )
                            Text(
                                "Add event on Google Calendar",
                                fontSize = 11.5.sp,
                                color = Label3,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel", color = Blue, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        )
    }
}

// ── Date Picker Bottom Sheet ─────────────────────────────────────────────────

@Composable
private fun DatePickerSheet(
    matchDates: Set<LocalDate>,
    scheduleStart: LocalDate,
    scheduleEnd: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = WCSurface
        ) {
            Column(modifier = Modifier.padding(bottom = 40.dp)) {
                Box(Modifier.fillMaxWidth().padding(top = 10.dp), Alignment.Center) {
                    Box(
                        Modifier
                            .size(36.dp, 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Label3)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(44.dp))
                    Text(
                        "Jump to Date",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Label1,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        "Done",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Blue,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss
                            )
                            .padding(4.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    val months = remember(scheduleStart, scheduleEnd) {
                        val result = mutableListOf<YearMonth>()
                        var month = YearMonth.from(scheduleStart)
                        val lastMonth = YearMonth.from(scheduleEnd)
                        while (!month.isAfter(lastMonth)) {
                            result.add(month)
                            month = month.plusMonths(1)
                        }
                        result
                    }
                    months.forEachIndexed { index, yearMonth ->
                        if (index > 0) Spacer(Modifier.height(20.dp))
                        CalendarMonth(
                            yearMonth = yearMonth,
                            matchDates = matchDates,
                            scheduleStart = scheduleStart,
                            scheduleEnd = scheduleEnd,
                            selectedDate = selectedDate,
                            onDateSelected = onDateSelected
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CalendarMonth(
    yearMonth: YearMonth,
    matchDates: Set<LocalDate>,
    scheduleStart: LocalDate,
    scheduleEnd: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDay = yearMonth.atDay(1)
    val month = yearMonth.month
    val daysInMonth = firstDay.lengthOfMonth()
    val firstDow = firstDay.dayOfWeek.value % 7  // Sun=0 … Sat=6

    Column {
        Text(
            text = month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " ${yearMonth.year}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Label1,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Row(Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { w ->
                Text(
                    w, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Label3,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val totalCells = firstDow + daysInMonth
        val rows = (totalCells + 6) / 7
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayNum = row * 7 + col - firstDow + 1
                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Spacer(Modifier.weight(1f).height(36.dp))
                    } else {
                        val date = yearMonth.atDay(dayNum)
                        val inTournament = !date.isBefore(scheduleStart) && !date.isAfter(scheduleEnd)
                        CalendarDay(
                            day = dayNum,
                            inTournament = inTournament,
                            hasMatch = date in matchDates,
                            isSelected = date == selectedDate,
                            onClick = if (inTournament) ({ onDateSelected(date) }) else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    day: Int,
    inTournament: Boolean,
    hasMatch: Boolean,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier
) {
    val baseModifier = modifier
        .height(36.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(if (isSelected) Blue else Color.Transparent)
    Box(
        modifier = if (onClick != null) baseModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ) else baseModifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$day",
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected    -> Color.White
                    !inTournament -> Label3.copy(alpha = 0.35f)
                    else          -> Label1
                }
            )
            if (hasMatch) {
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White.copy(alpha = 0.65f) else Blue)
                )
            }
        }
    }
}

