package com.premierpicks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.premierpicks.engine.MatchEvent
import com.premierpicks.engine.MatchState
import com.premierpicks.engine.Phase
import com.premierpicks.engine.Side
import com.premierpicks.engine.SoloMatch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Banner(val id: Long, val title: String, val detail: String? = null)

data class MatchUiState(
    val match: MatchState,
    val humanSide: Side,
    val isHumanTurn: Boolean,
    val banner: Banner? = null,
)

class MatchViewModel : ViewModel() {
    private var match = SoloMatch(Side.HOME)
    private var bannerCount = 0L
    private var computerJob: Job? = null

    private val _ui = MutableStateFlow(snapshot(banner = null))
    val ui: StateFlow<MatchUiState> = _ui.asStateFlow()

    fun chooseBudget(picks: Int) {
        if (!match.isHumanTurn || match.state.phase != Phase.AWAITING_BUDGET) return
        match.chooseBudget(picks)
        publish(emptyList())
    }

    fun flip(index: Int) {
        if (!match.isHumanTurn || match.state.phase != Phase.FLIPPING) return
        publish(match.flip(index))
        runComputer()
    }

    fun startSecondHalf() {
        if (!match.isHalfTime) return
        match.startSecondHalf()
        publish(emptyList())
        runComputer()
    }

    fun newMatch() {
        computerJob?.cancel()
        match = SoloMatch(Side.HOME)
        publish(emptyList())
    }

    /** Plays the computer's moves with pauses so the player can follow each one. */
    private fun runComputer() {
        if (computerJob?.isActive == true) return
        computerJob = viewModelScope.launch {
            while (match.isComputerTurn) {
                delay(if (match.state.phase == Phase.AWAITING_BUDGET) THINK_MS else FLIP_MS)
                publish(match.computerStep())
            }
        }
    }

    private fun publish(events: List<MatchEvent>) {
        val banner = bannerFor(events)
        _ui.value = snapshot(banner ?: _ui.value.banner)
        if (banner != null) {
            viewModelScope.launch {
                delay(BANNER_MS)
                if (_ui.value.banner?.id == banner.id) _ui.value = _ui.value.copy(banner = null)
            }
        }
    }

    private fun snapshot(banner: Banner?) = MatchUiState(
        match = match.state,
        humanSide = match.humanSide,
        isHumanTurn = match.isHumanTurn,
        banner = banner,
    )

    /** Picks the most important thing that just happened to show as a banner. */
    private fun bannerFor(events: List<MatchEvent>): Banner? {
        val event = events
            .filterNot { it is MatchEvent.TurnChanged || it is MatchEvent.HalfTime || it is MatchEvent.FullTime }
            .maxByOrNull { priority(it) } ?: return null
        val id = ++bannerCount
        return when (event) {
            is MatchEvent.Goal -> Banner(
                id,
                if (event.fromPenalty) "PENALTY SCORED!" else "GOAL!",
                if (event.side == match.humanSide) "You score" else "CPU scores",
            )
            is MatchEvent.PenaltySaved -> Banner(id, "PENALTY SAVED", "The keeper guesses right")
            is MatchEvent.SentOff -> Banner(id, "SENT OFF!", "${who(event.side)} can now pick at most ${event.newMaxPicks}")
            is MatchEvent.Booked -> Banner(id, if (event.red) "RED CARD" else "YELLOW CARD", who(event.side))
            is MatchEvent.Foul -> Banner(
                id,
                "FOUL!",
                if (event.forfeitedPicks == 0) "Turn over" else "${event.forfeitedPicks} picks go to ${who(event.side.opponent)}",
            )
            is MatchEvent.Hint -> Banner(id, "HINT!", "Goals are highlighted")
            is MatchEvent.Substitution -> Banner(id, "Substitution")
            is MatchEvent.Miss -> Banner(id, if (id % 2 == 0L) "Shot wide" else "Saved!")
            else -> null
        }
    }

    private fun priority(event: MatchEvent): Int = when (event) {
        is MatchEvent.Goal -> 6
        is MatchEvent.PenaltySaved -> 5
        is MatchEvent.SentOff -> 4
        is MatchEvent.Foul -> 3
        is MatchEvent.Booked -> 2
        is MatchEvent.Hint -> 1
        else -> 0
    }

    private fun who(side: Side) = if (side == match.humanSide) "You" else "CPU"

    private companion object {
        const val THINK_MS = 900L
        const val FLIP_MS = 800L
        const val BANNER_MS = 1_400L
    }
}
