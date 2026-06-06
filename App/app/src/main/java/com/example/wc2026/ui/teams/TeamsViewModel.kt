package com.carldong.fifa.worldcup2026.ui.teams

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carldong.fifa.worldcup2026.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TeamSort { RANK, GROUP, NAME }

data class TeamsUiState(
    val teams: List<Team> = emptyList(),
    val searchQuery: String = "",
    val sort: TeamSort = TeamSort.RANK,
    val selectedTeam: Team? = null,
    val teamRoster: List<RosterPlayer> = emptyList(),
    val isRosterLoading: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

class TeamsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = DefaultDataRepository(app)
    private val _state = MutableStateFlow(TeamsUiState())
    val state: StateFlow<TeamsUiState> = _state.asStateFlow()

    init { load() }

    private fun load() = viewModelScope.launch {
        try {
            val teams = repo.getTeams()
            _state.update { it.copy(teams = teams, isLoading = false) }
            launch { repo.getRoster("") }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = e.message) }
        }
    }

    fun setSearch(q: String) = _state.update { it.copy(searchQuery = q) }
    fun setSort(s: TeamSort) = _state.update { it.copy(sort = s) }

    fun selectTeam(t: Team?) {
        _state.update { it.copy(selectedTeam = t, teamRoster = emptyList()) }
        if (t != null) loadRoster(t.name)
    }

    fun selectTeamById(id: String) {
        val team = _state.value.teams.find { it.id == id }
        if (team != null) selectTeam(team)
    }

    /** Build a full Player object from roster entry for navigation to player detail page. */
    fun buildPlayerForNav(rp: com.carldong.fifa.worldcup2026.data.RosterPlayer, countryName: String): com.carldong.fifa.worldcup2026.data.Player? =
        repo.buildPlayerForNav(rp, countryName)

    private fun loadRoster(countryName: String) = viewModelScope.launch {
        _state.update { it.copy(isRosterLoading = true) }
        try {
            val roster = repo.getRoster(countryName)
            _state.update { it.copy(teamRoster = roster, isRosterLoading = false) }
        } catch (_: Exception) {
            _state.update { it.copy(isRosterLoading = false) }
        }
    }
}

