package com.premierpicks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.premierpicks.engine.CardType
import com.premierpicks.engine.FOUL_PICKS
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
    val homeName: String,
    val awayName: String,
    val isHumanTurn: Boolean,
    val cardHelp: Boolean,
    /** Cards the player is being shown by card help right now. */
    val previewShown: Set<Int> = emptySet(),
    /** Card help is over for this half and play is about to begin. */
    val previewDone: Boolean = false,
    /** When the player's penalty hunt runs out, in [System.currentTimeMillis] terms. */
    val penaltyDeadline: Long? = null,
    val banner: Banner? = null,
) {
    fun name(side: Side) = if (side == Side.HOME) homeName else awayName

    /** Face-down cards that are showing their face for the moment: previews and penalty hunts. */
    val peeking: Set<Int> get() = previewShown + match.penaltyPeeks
}

class MatchViewModel : ViewModel() {
    private var match = SoloMatch(Side.HOME)
    private var homeName = "Home"
    private var awayName = "Away"
    private var bannerCount = 0L
    private var computerJob: Job? = null
    private var penaltyJob: Job? = null

    private val _ui = MutableStateFlow(snapshot())
    val ui: StateFlow<MatchUiState> = _ui.asStateFlow()

    /** Starts a fresh match. */
    fun start(humanSide: Side, homeName: String, awayName: String, cardHelp: Boolean) {
        computerJob?.cancel()
        penaltyJob?.cancel()
        this.homeName = homeName
        this.awayName = awayName
        match = SoloMatch(humanSide, cardHelp)
        _ui.value = snapshot()
        if (!cardHelp) skipPreview()
    }

    /** The player stopped the card-help buzzer on [count]: show them that many cards for a moment. */
    fun stopBuzzer(count: Int) {
        if (!match.isPreview || _ui.value.previewDone) return
        val shown = match.preview(count).toSet()
        _ui.value = snapshot().copy(previewShown = shown, previewDone = true)
        viewModelScope.launch {
            delay(PREVIEW_MS)
            match.beginPlay()
            publish(emptyList())
            runComputer()
        }
    }

    fun flip(index: Int) {
        val state = match.state
        when {
            match.isHumanPenalty -> {
                publish(match.penaltyPeek(index))
                runComputer()
            }
            match.isHumanTurn && state.phase == Phase.PICKING -> {
                publish(match.flip(index))
                if (match.isHumanPenalty) startPenaltyClock()
                runComputer()
            }
        }
    }

    fun startSecondHalf() {
        if (!match.isHalfTime) return
        match.startSecondHalf()
        _ui.value = snapshot()
        if (!match.cardHelp) skipPreview()
    }

    private fun skipPreview() {
        match.preview(0)
        match.beginPlay()
        publish(emptyList())
        runComputer()
    }

    private fun startPenaltyClock() {
        penaltyJob?.cancel()
        val deadline = System.currentTimeMillis() + PENALTY_MS
        _ui.value = _ui.value.copy(penaltyDeadline = deadline)
        penaltyJob = viewModelScope.launch {
            delay(PENALTY_MS)
            if (match.isHumanPenalty) {
                publish(match.penaltyTimeUp())
                runComputer()
            }
        }
    }

    /** Plays the computer's moves with pauses so the player can follow each one. */
    private fun runComputer() {
        if (computerJob?.isActive == true) return
        computerJob = viewModelScope.launch {
            while (match.isComputerTurn) {
                delay(if (match.state.phase == Phase.PENALTY) PENALTY_PEEK_MS else FLIP_MS)
                publish(match.computerStep())
            }
        }
    }

    private fun publish(events: List<MatchEvent>) {
        val banner = bannerFor(events)
        val keepDeadline = if (match.isHumanPenalty) _ui.value.penaltyDeadline else null
        _ui.value = snapshot().copy(
            banner = banner ?: _ui.value.banner,
            penaltyDeadline = keepDeadline,
            previewDone = match.state.phase != Phase.PREVIEW,
        )
        if (banner != null) {
            viewModelScope.launch {
                delay(BANNER_MS)
                if (_ui.value.banner?.id == banner.id) _ui.value = _ui.value.copy(banner = null)
            }
        }
    }

    private fun snapshot() = MatchUiState(
        match = match.state,
        humanSide = match.humanSide,
        homeName = homeName,
        awayName = awayName,
        isHumanTurn = match.isHumanTurn,
        cardHelp = match.cardHelp,
    )

    /** Picks the most important thing that just happened to show as a banner. */
    private fun bannerFor(events: List<MatchEvent>): Banner? {
        val event = events
            .filterNot { it is MatchEvent.TurnChanged || it is MatchEvent.HalfTime || it is MatchEvent.FullTime }
            // The player needs a clear view of the board to hunt for their penalty.
            .filterNot { it is MatchEvent.PenaltyAwarded && it.side == match.humanSide }
            .maxByOrNull { priority(it) } ?: return null
        val id = ++bannerCount
        return when (event) {
            is MatchEvent.Goal -> Banner(
                id,
                if (event.fromPenalty) "PENALTY SCORED!" else "GOAL!",
                if (event.side == match.humanSide) "You score" else "${teamName(event.side)} score",
            )
            is MatchEvent.PenaltyAwarded -> Banner(id, "PENALTY!", "${teamName(event.side)} hunt for a goal")
            is MatchEvent.PenaltyMissed -> Banner(id, "PENALTY MISSED", if (event.side == match.humanSide) "Out of time" else null)
            is MatchEvent.Foul -> Banner(id, "FOUL!", "${who(event.side.opponent)} get $FOUL_PICKS picks")
            is MatchEvent.FreeKick -> Banner(id, "FREE KICK!", "${who(event.side)} pick again")
            is MatchEvent.Substitution -> Banner(id, "SUBSTITUTION", "Swapped for ${describe(event.replacement)}")
            is MatchEvent.Miss -> Banner(id, COMMENTARY[(id % COMMENTARY.size).toInt()])
            else -> null
        }
    }

    private fun priority(event: MatchEvent): Int = when (event) {
        is MatchEvent.Goal -> 5
        is MatchEvent.PenaltyMissed -> 4
        is MatchEvent.PenaltyAwarded -> 3
        is MatchEvent.Foul -> 2
        // Above the card it turned into, unless that card did something bigger.
        is MatchEvent.Substitution -> 1
        is MatchEvent.FreeKick -> 1
        else -> 0
    }

    private fun describe(type: CardType) = when (type) {
        CardType.GOAL -> "a goal"
        CardType.PENALTY -> "a penalty"
        CardType.MISS -> "a near miss"
        CardType.FOUL -> "a foul"
        CardType.YELLOW -> "a free kick"
        CardType.SUBSTITUTE, CardType.WHISTLE -> "another card"
    }

    private fun who(side: Side) = if (side == match.humanSide) "You" else teamName(side)

    private fun teamName(side: Side) = if (side == Side.HOME) homeName else awayName

    companion object {
        const val PREVIEW_MS = 3_000L
        const val PENALTY_MS = 3_000L
        const val FLIP_MS = 800L
        const val PENALTY_PEEK_MS = 350L
        const val BANNER_MS = 1_200L
        private val COMMENTARY = listOf("Just wide!", "Saved!", "Off the post!", "Over the bar!")
    }
}
