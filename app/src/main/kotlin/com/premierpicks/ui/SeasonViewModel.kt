package com.premierpicks.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.premierpicks.engine.Fixture
import com.premierpicks.engine.Season
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.random.Random

data class SeasonUiState(
    /** Null until the player picks a team. */
    val season: Season? = null,
    /** The fixture being played right now, if a match is in progress. */
    val playing: Fixture? = null,
    /** Bumped on every change, because [Season] itself is mutable. */
    val version: Int = 0,
)

/** Owns the current season and saves it after every round so it survives the app closing. */
class SeasonViewModel(app: Application) : AndroidViewModel(app) {
    private val saveFile = File(app.filesDir, "season.txt")
    private val rng = Random.Default

    private val _ui = MutableStateFlow(SeasonUiState(season = load()))
    val ui: StateFlow<SeasonUiState> = _ui.asStateFlow()

    fun newSeason(team: String) {
        update(Season.new(team), playing = null)
    }

    fun abandonSeason() {
        saveFile.delete()
        _ui.value = SeasonUiState(version = _ui.value.version + 1)
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
        _ui.value = SeasonUiState(season, playing, _ui.value.version + 1)
    }

    private fun load(): Season? = try {
        if (saveFile.exists()) Season.decode(saveFile.readText()) else null
    } catch (e: IllegalArgumentException) {
        null
    }
}
