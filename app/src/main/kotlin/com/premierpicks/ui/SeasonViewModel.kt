package com.premierpicks.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.premierpicks.engine.DEFAULT_TEAMS
import com.premierpicks.engine.Fixture
import com.premierpicks.engine.Season
import com.premierpicks.engine.SeasonSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.random.Random

data class SeasonUiState(
    /** Null until the player starts a season. */
    val season: Season? = null,
    /** The fixture being played right now, if a match is in progress. */
    val playing: Fixture? = null,
    /** Club names for the next new season; the player can rename them. */
    val teamNames: List<String> = DEFAULT_TEAMS,
    /** Bumped on every change, because [Season] itself is mutable. */
    val version: Int = 0,
)

/** Owns the current season and saves it after every round so it survives the app closing. */
class SeasonViewModel(app: Application) : AndroidViewModel(app) {
    private val saveFile = File(app.filesDir, "season.txt")
    private val namesFile = File(app.filesDir, "teams.txt")
    private val rng = Random.Default

    private val _ui = MutableStateFlow(SeasonUiState(season = load(), teamNames = loadNames()))
    val ui: StateFlow<SeasonUiState> = _ui.asStateFlow()

    fun newSeason(team: String, settings: SeasonSettings) {
        update(Season.new(team, _ui.value.teamNames, settings), playing = null)
    }

    /** Renames a club for future seasons. Returns false if the name is blank or already taken. */
    fun renameTeam(index: Int, name: String): Boolean {
        val clean = name.replace("\t", " ").trim()
        val names = _ui.value.teamNames
        if (clean.isEmpty() || names.withIndex().any { (i, n) -> i != index && n.equals(clean, ignoreCase = true) }) return false
        val renamed = names.toMutableList().also { it[index] = clean }
        namesFile.writeText(renamed.joinToString("\n"))
        _ui.value = _ui.value.copy(teamNames = renamed, version = _ui.value.version + 1)
        return true
    }

    fun resetTeamNames() {
        namesFile.delete()
        _ui.value = _ui.value.copy(teamNames = DEFAULT_TEAMS, version = _ui.value.version + 1)
    }

    fun abandonSeason() {
        saveFile.delete()
        _ui.value = _ui.value.copy(season = null, playing = null, version = _ui.value.version + 1)
    }

    /** Starts the player's match, or simulates the round straight away if they rest. */
    fun playRound(): Fixture? {
        val season = _ui.value.season ?: return null
        val fixture = season.playerFixture
        if (fixture == null) {
            season.completeRound(null, null, rng)
            update(season, playing = null)
        } else {
            update(season, playing = fixture)
        }
        return fixture
    }

    fun finishMatch(homeGoals: Int, awayGoals: Int) {
        val season = _ui.value.season ?: return
        season.completeRound(homeGoals, awayGoals, rng)
        update(season, playing = null)
    }

    private fun update(season: Season, playing: Fixture?) {
        saveFile.writeText(season.encode())
        _ui.value = _ui.value.copy(season = season, playing = playing, version = _ui.value.version + 1)
    }

    private fun load(): Season? = try {
        if (saveFile.exists()) Season.decode(saveFile.readText()) else null
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun loadNames(): List<String> {
        val names = if (namesFile.exists()) namesFile.readLines().map { it.trim() }.filter { it.isNotEmpty() } else emptyList()
        return if (names.size == DEFAULT_TEAMS.size && names.toSet().size == names.size) names else DEFAULT_TEAMS
    }
}
