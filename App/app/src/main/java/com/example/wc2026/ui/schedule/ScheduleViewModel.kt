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
                    .map { (date, list) -> ScheduleDateGroup(date, list.sortedBy { it.localKickoff() }) }
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

