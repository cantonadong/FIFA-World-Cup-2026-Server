package com.carldong.fifa.worldcup2026.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carldong.fifa.worldcup2026.data.DataRepository
import com.carldong.fifa.worldcup2026.data.DefaultDataRepository
import com.carldong.fifa.worldcup2026.data.Match
import com.carldong.fifa.worldcup2026.data.Team
import com.carldong.fifa.worldcup2026.data.Venue
import com.carldong.fifa.worldcup2026.data.localKickoff
import com.carldong.fifa.worldcup2026.data.localKickoffDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ScheduleDateGroup(
    val date: LocalDate,
    val matches: List<Match>
)

data class ScheduleUiState(
    val dateGroups: List<ScheduleDateGroup> = emptyList(),
    val teamMap: Map<String, Team> = emptyMap(),
    val venueMap: Map<String, Venue> = emptyMap(),
    val matchDates: Set<LocalDate> = emptySet(),
    val selectedDate: LocalDate = LocalDate.of(2026, 6, 12),
    val isLoading: Boolean = true,
    val error: String? = null
)

class ScheduleViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: DataRepository = DefaultDataRepository(app)
    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private val officialScheduleOrder = listOf(
        "M001", "M002", "M003", "M004",
        "M008", "M007", "M005", "M006",
        "M010", "M011", "M009", "M012",
        "M014", "M016", "M013", "M015",
        "M018", "M017", "M019", "M020",
        "M023", "M022", "M021", "M024",
        "M025", "M028", "M027", "M026",
        "M032", "M030", "M029", "M031",
        "M035", "M033", "M034", "M036",
        "M038", "M039", "M037", "M040",
        "M043", "M042", "M041", "M044",
        "M047", "M045", "M046", "M048",
        "M051", "M052", "M049", "M050", "M053", "M054",
        "M055", "M056", "M057", "M058", "M059", "M060",
        "M061", "M062", "M065", "M066", "M063", "M064",
        "M067", "M068", "M071", "M072", "M069", "M070",
        "M073", "M076", "M074", "M075", "M078", "M077", "M079", "M080",
        "M082", "M081", "M084", "M083", "M085", "M088", "M086", "M087",
        "M089", "M090", "M091", "M092", "M093", "M094", "M095", "M096",
        "M097", "M098", "M099", "M100", "M101", "M102", "M103", "M104"
    ).withIndex().associate { (index, matchId) -> matchId to index }

    init {
        load()
        viewModelScope.launch {
            repo.observeMatchVersion().collect {
                if (!_uiState.value.isLoading) load()
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val teams = repo.getTeams()
                val matches = repo.getMatches()
                val venues = repo.getVenues()

                val teamMap = teams.associateBy { it.id }
                val venueMap = venues.associateBy { it.id }

                val dateGroups = matches
                    .groupBy { it.localKickoffDate() ?: LocalDate.parse(it.date) }
                    .map { (date, list) ->
                        ScheduleDateGroup(
                            date,
                            list.sortedWith(
                                compareBy<Match> { match -> officialScheduleOrder[match.id] ?: Int.MAX_VALUE }
                                    .thenBy { match -> match.localKickoff() }
                            )
                        )
                    }
                    .sortedBy { it.date }

                val matchDates = dateGroups.map { it.date }.toSet()

                val today = LocalDate.now()
                val initial = matchDates.firstOrNull { !it.isBefore(today) }
                    ?: matchDates.firstOrNull()
                    ?: LocalDate.of(2026, 6, 12)

                _uiState.update {
                    it.copy(
                        dateGroups = dateGroups,
                        teamMap = teamMap,
                        venueMap = venueMap,
                        matchDates = matchDates,
                        selectedDate = initial,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repo.refreshMatches()
            load()
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }
}

