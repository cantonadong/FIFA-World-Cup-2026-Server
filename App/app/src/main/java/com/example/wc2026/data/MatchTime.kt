package com.carldong.fifa.worldcup2026.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val MatchSourceZone: ZoneId = ZoneId.of("America/New_York")
private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val GoogleUtcFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneId.of("UTC"))

fun Match.kickoffInstant(): Instant? = runCatching {
    LocalDate.parse(date)
        .atTime(LocalTime.parse(time))
        .atZone(MatchSourceZone)
        .toInstant()
}.getOrNull()

fun Match.localKickoff(zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime? =
    kickoffInstant()?.atZone(zone)

fun Match.localKickoffDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate? =
    localKickoff(zone)?.toLocalDate()

fun Match.localKickoffTimeLabel(zone: ZoneId = ZoneId.systemDefault()): String =
    localKickoff(zone)?.format(TimeFormatter) ?: time

fun Match.localKickoffDateLabel(zone: ZoneId = ZoneId.systemDefault()): String =
    localKickoff(zone)?.let { zdt ->
        "${zdt.month.getDisplayName(TextStyle.SHORT, Locale.US).uppercase()} ${zdt.dayOfMonth}"
    } ?: date

fun Match.googleCalendarUtcRange(durationHours: Long = 2): Pair<String, String>? {
    val start = kickoffInstant() ?: return null
    val end = start.plusSeconds(durationHours * 60 * 60)
    return GoogleUtcFormatter.format(start) to GoogleUtcFormatter.format(end)
}
