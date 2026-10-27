package com.premierpicks.engine

import kotlin.random.Random

const val MAX_PICKS = 6
const val PENALTY_SUCCESS_CHANCE = 0.8

enum class Side {
    HOME,
    AWAY;

    val opponent: Side get() = if (this == HOME) AWAY else HOME
}

/** One team's state within a single match. Discipline persists across both halves. */
data class SideState(
    val goals: Int = 0,
    val yellows: Int = 0,
    val reds: Int = 0,
    /** Picks forfeited to this side by an opponent's foul, added to its next turn. */
    val bonusPicks: Int = 0,
) {
    /** Two yellows count as one sending-off, as does each red. */
    val sendingsOff: Int get() = yellows / 2 + reds

    /** Largest budget this side may choose; each sending-off costs one, never below 1. */
    val maxPicks: Int get() = (MAX_PICKS - sendingsOff).coerceAtLeast(1)
}

data class Turn(
    val side: Side,
    /** Budget chosen by the player, or null before they confirm one. */
    val requested: Int? = null,
    val bonus: Int = 0,
    val used: Int = 0,
) {
    val total: Int get() = (requested ?: 0) + bonus
    val remaining: Int get() = total - used
}

enum class Phase { AWAITING_BUDGET, FLIPPING, HALF_TIME, FULL_TIME }

data class MatchState(
    val half: Int,
    val board: List<Card>,
    val home: SideState,
    val away: SideState,
    val turn: Turn,
    val phase: Phase,
    /** Goal positions exposed by a hint card this half; the UI highlights them. */
    val highlightedGoals: Set<Int> = emptySet(),
) {
    fun side(side: Side): SideState = if (side == Side.HOME) home else away

    fun withSide(side: Side, state: SideState): MatchState =
        if (side == Side.HOME) copy(home = state) else copy(away = state)

    val unrevealed: List<Int> get() = board.indices.filter { !board[it].revealed }
}

sealed interface MatchEvent {
    data class Goal(val side: Side, val fromPenalty: Boolean) : MatchEvent
    data class PenaltySaved(val side: Side) : MatchEvent
    data class Hint(val goalIndices: Set<Int>) : MatchEvent
    data class Miss(val side: Side) : MatchEvent
    data class Substitution(val side: Side, val replacement: CardType) : MatchEvent
    data class Foul(val side: Side, val forfeitedPicks: Int) : MatchEvent
    data class Booked(val side: Side, val red: Boolean) : MatchEvent
    data class SentOff(val side: Side, val newMaxPicks: Int) : MatchEvent
    data class TurnChanged(val side: Side, val bonusPicks: Int) : MatchEvent
    data object HalfTime : MatchEvent
    data object FullTime : MatchEvent
}

data class StepResult(val state: MatchState, val events: List<MatchEvent>)

/**
 * Pure match rules. Every function returns a new state; randomness comes only from the
 * [Random] passed in, so a seeded match is fully reproducible.
 */
object MatchEngine {

    fun kickOff(rng: Random): MatchState = MatchState(
        half = 1,
        board = BoardFactory.newBoard(rng),
        home = SideState(),
        away = SideState(),
        turn = Turn(Side.HOME),
        phase = Phase.AWAITING_BUDGET,
    )

    /** Confirms the active side's pick budget; cards cannot be flipped until this is done. */
    fun chooseBudget(state: MatchState, picks: Int): MatchState {
        check(state.phase == Phase.AWAITING_BUDGET) { "Not awaiting a budget (phase ${state.phase})" }
        val max = state.side(state.turn.side).maxPicks
        require(picks in 1..max) { "Budget must be 1..$max, was $picks" }
        return state.copy(turn = state.turn.copy(requested = picks), phase = Phase.FLIPPING)
    }

    fun flip(state: MatchState, index: Int, rng: Random): StepResult {
        check(state.phase == Phase.FLIPPING) { "Cannot flip in phase ${state.phase}" }
        require(index in state.board.indices) { "No card at $index" }
        val card = state.board[index]
        if (card.revealed) return StepResult(state, emptyList())

        val active = state.turn.side
        val events = mutableListOf<MatchEvent>()
        // A substitute is swapped for another card, which takes effect with the same pick.
        val type = if (card.type == CardType.SUBSTITUTE) BoardFactory.substituteFor(rng) else card.type
        if (card.type == CardType.SUBSTITUTE) events += MatchEvent.Substitution(active, type)
        var s = state.copy(
            board = state.board.toMutableList().also {
                it[index] = Card(type, revealed = true, substituted = card.type == CardType.SUBSTITUTE)
            },
            turn = state.turn.copy(used = state.turn.used + 1),
            highlightedGoals = state.highlightedGoals - index,
        )
        val me = s.side(active)

        when (type) {
            CardType.GOAL -> {
                s = s.withSide(active, me.copy(goals = me.goals + 1))
                events += MatchEvent.Goal(active, fromPenalty = false)
            }
            CardType.PENALTY -> {
                if (rng.nextDouble() < PENALTY_SUCCESS_CHANCE) {
                    s = s.withSide(active, me.copy(goals = me.goals + 1))
                    events += MatchEvent.Goal(active, fromPenalty = true)
                } else {
                    events += MatchEvent.PenaltySaved(active)
                }
            }
            CardType.HINT -> {
                val goals = s.unrevealed.filter { s.board[it].type == CardType.GOAL }.toSet()
                s = s.copy(highlightedGoals = goals)
                events += MatchEvent.Hint(goals)
            }
            CardType.MISS -> events += MatchEvent.Miss(active)
            CardType.SUBSTITUTE -> error("A substitute is always replaced before it takes effect")
            CardType.FOUL -> {
                val forfeited = s.turn.remaining
                val opp = s.side(active.opponent)
                s = s.withSide(active.opponent, opp.copy(bonusPicks = opp.bonusPicks + forfeited))
                events += MatchEvent.Foul(active, forfeited)
            }
            CardType.YELLOW, CardType.RED -> {
                val red = type == CardType.RED
                val booked = if (red) me.copy(reds = me.reds + 1) else me.copy(yellows = me.yellows + 1)
                s = s.withSide(active, booked)
                events += MatchEvent.Booked(active, red)
                if (booked.sendingsOff > me.sendingsOff) {
                    events += MatchEvent.SentOff(active, booked.maxPicks)
                }
            }
            CardType.WHISTLE -> return endHalf(s, events)
        }

        if (s.unrevealed.isEmpty()) {
            return endHalf(s, events)
        }
        if (type == CardType.FOUL || s.turn.remaining == 0) {
            s = switchTurn(s, events)
        }
        return StepResult(s, events)
    }

    /** Deals the second-half board. The away side kicks off the second half. */
    fun startSecondHalf(state: MatchState, rng: Random): MatchState {
        check(state.phase == Phase.HALF_TIME) { "Not half time (phase ${state.phase})" }
        return state.copy(
            half = 2,
            board = BoardFactory.newBoard(rng),
            turn = Turn(Side.AWAY),
            phase = Phase.AWAITING_BUDGET,
            highlightedGoals = emptySet(),
        )
    }

    private fun switchTurn(state: MatchState, events: MutableList<MatchEvent>): MatchState {
        val next = state.turn.side.opponent
        val nextState = state.side(next)
        events += MatchEvent.TurnChanged(next, nextState.bonusPicks)
        return state
            .withSide(next, nextState.copy(bonusPicks = 0))
            .copy(turn = Turn(next, bonus = nextState.bonusPicks), phase = Phase.AWAITING_BUDGET)
    }

    /** Bonus picks do not survive the end of a half: the new board starts level. */
    private fun endHalf(state: MatchState, events: MutableList<MatchEvent>): StepResult {
        val cleared = state.copy(
            home = state.home.copy(bonusPicks = 0),
            away = state.away.copy(bonusPicks = 0),
        )
        return if (state.half == 1) {
            events += MatchEvent.HalfTime
            StepResult(cleared.copy(phase = Phase.HALF_TIME), events)
        } else {
            events += MatchEvent.FullTime
            StepResult(cleared.copy(phase = Phase.FULL_TIME), events)
        }
    }
}
