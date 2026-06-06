package com.carldong.fifa.worldcup2026.data

import kotlinx.serialization.Serializable

@Serializable
data class Team(
    val id: String,
    val name: String,
    val group: String,
    val confederation: String,
    val fifaRank: Int,
    val imageFile: String,
    val flagFile: String = "",
    val primaryColor: String = "#333333",
    val status: String = "Active",
    val worldCupDebut: Boolean = false
) {
    val isWithdrawn: Boolean get() = status == "Withdrawn"
    val abbr: String get() {
        val words = name.trim().split(Regex("\\s+"))
        return when {
            words.size == 1 -> name.take(3).uppercase()
            else -> words.take(3).map { it.first().uppercaseChar() }.joinToString("")
        }
    }
}

@Serializable
data class Venue(
    val id: String,
    val name: String,
    val fifaName: String = "",
    val city: String,
    val country: String
)

@Serializable
data class Match(
    val id: String,
    val stage: String,
    val stageName: String,
    val group: String = "",
    val matchday: Int? = null,
    val date: String,
    val time: String,
    val team1Id: String = "",
    val team1Name: String = "",
    val team2Id: String = "",
    val team2Name: String = "",
    val venueId: String,
    val status: String = "upcoming",
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val penaltyHomeScore: Int? = null,
    val penaltyAwayScore: Int? = null,
    val notes: String = ""
)

data class Player(
    val id: Int,
    val name: String,
    val full: String,
    val country: String,
    val flagFile: String,
    val cc: String,
    val club: String,
    val clubCC: String,
    val pos: String,
    val age: Int,
    val ovr: Int, val spd: Int, val atk: Int, val pas: Int,
    val dri: Int, val def: Int, val phy: Int,
    val valueMEUR: Int,
    val height: String, val weight: String, val foot: String,
    val caps: Int, val cGoals: Int, val cAssists: Int,
    val avatarFile: String = "",
    val clubLogoFile: String = "",
    val detailedPos: String = ""
)

data class RosterPlayer(
    val name: String,
    val pos: String,
    val age: Int,
    val caps: Int,
    val goals: Int,
    val club: String,
    val clubCountry: String,
    val ovr: Int = 0,
    val valueMEUR: Int = 0,
    val avatarFile: String = "",
    val clubLogoFile: String = "",
    val playerId: Int = 0,
    val number: Int? = null,
    val captain: Boolean = false
)

data class TeamStanding(
    val team: Team,
    val played: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val points: Int = 0
) {
    val goalDiff: Int get() = goalsFor - goalsAgainst
}

