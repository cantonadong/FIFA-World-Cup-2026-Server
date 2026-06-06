package com.carldong.fifa.worldcup2026.ui.standings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carldong.fifa.worldcup2026.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StandingsUiState(
    val teams: List<Team> = emptyList(),
    val matches: List<Match> = emptyList(),
    val venues: List<Venue> = emptyList(),
    val groupStandings: Map<String, List<TeamStanding>> = emptyMap(),
    val selectedStage: String = "GS",
    val selectedKoRound: String = "R32",
    val isLoading: Boolean = true,
    val error: String? = null
)

class StandingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = DefaultDataRepository(app)
    private val _state = MutableStateFlow(StandingsUiState())
    val state: StateFlow<StandingsUiState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            repo.observeMatchVersion().collect {
                if (!_state.value.isLoading) load()
            }
        }
    }

    private fun load() = viewModelScope.launch {
        try {
            val teams = repo.getTeams()
            val matches = repo.getMatches()
            val venues = repo.getVenues()
            val standings = repo.getGroupStandings(teams, matches)
            _state.value = StandingsUiState(
                teams = teams,
                matches = matches,
                venues = venues,
                groupStandings = standings,
                isLoading = false
            )
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = e.message) }
        }
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        repo.refreshMatches()
        load()
    }

    fun selectStage(stage: String) {
        _state.update { it.copy(selectedStage = stage) }
    }

    fun selectKoRound(round: String) {
        _state.update { it.copy(selectedKoRound = round) }
    }
}

