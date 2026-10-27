package com.premierpicks.engine

import kotlin.random.Random

/** Most cards the card-help buzzer can let a side preview before a half. */
const val MAX_PREVIEW = 6

/** Picks the opponent gets after a foul. */
const val FOUL_PICKS = 2

enum class Side {
    HOME,
    AWAY;

    val opponent: Side get() = if (this == HOME) AWAY else HOME
}

data class SideState(val goals: Int = 0, val yellows: Int = 0)

data class Turn(val side: Side, val picksLeft: Int = 1)

enum class Phase {
    /** Before a half: each side may preview some cards ("card help"). */
    PREVIEW,
    PICKING,
    /** The side on turn is hunting for a goal card against the clock. */
    PENALTY,
    HALF_TIME,
    FULL_TIME,
}

data class MatchState(
    val half: Int,
    val board: List<Card>,
    val home: SideState,
    val away: SideState,
    val turn: Turn,
    val phase: Phase,
    /** Face-down cards each side has seen this half, from card help or penalty hunts. */
    val knownHome: Set<Int> = emptySet(),
    val knownAway: Set<Int> = emptySet(),
    /** Cards turned over so far in the current penalty hunt. */
    val penaltyPeeks: List<Int> = emptyList(),
) {
    fun side(side: Side): SideState = if (side == Side.HOME) home else away

    fun withSide(side: Side, state: SideState): MatchState =
        if (side == Side.HOME) copy(home = state) else copy(away = state)

    fun known(side: Side): Set<Int> = if (side == Side.HOME) knownHome else knownAway

    fun withKnown(side: Side, known: Set<Int>): MatchState =
        if (side == Side.HOME) copy(knownHome = known) else copy(knownAway = known)

    val faceDown: List<Int> get() = board.indices.filter { !board[it].revealed }
}

sealed interface MatchEvent {
    data class Goal(val side: Side, val fromPenalty: Boolean) : MatchEvent
    data class PenaltyAwarded(val side: Side) : MatchEvent
    data class PenaltyMissed(val side: Side) : MatchEvent
    data class Miss(val side: Side) : MatchEvent
    data class FreeKick(val side: Side) : MatchEvent
    data class Foul(val side: Side) : MatchEvent
    data class Substitution(val side: Side, val replacement: CardType) : MatchEvent
    data class TurnChanged(val side: Side, val picks: Int) : MatchEvent
    data object HalfTime : MatchEvent
    data object FullTime : MatchEvent
}

data class StepResult(val state: MatchState, val events: List<MatchEvent>)

/**
 * Pure match rules. Every function returns a new state; randomness comes only from the
 * [Random] passed in, so a seeded match is fully reproducible.
 *
 * A half: both sides may preview some cards, then they take turns flipping one card each
 * until someone flips the whistle.
 */
object MatchEngine {

    fun kickOff(rng: Random): MatchState = MatchState(
        half = 1,
        board = BoardFactory.newBoard(rng),
        home = SideState(),
        away = SideState(),
        turn = Turn(Side.HOME),
        phase = Phase.PREVIEW,
    )

    /**
     * Card help: shows [side] up to [count] face-down cards it hasn't seen yet. Returns the
     * new state and the cards shown.
     */
    fun preview(state: MatchState, side: Side, count: Int, rng: Random): Pair<MatchState, List<Int>> {
        check(state.phase == Phase.PREVIEW) { "Not before a half (phase ${state.phase})" }
        require(count in 0..MAX_PREVIEW) { "Preview must be 0..$MAX_PREVIEW, was $count" }
        val shown = (state.faceDown - state.known(side)).shuffled(rng).take(count)
        return state.withKnown(side, state.known(side) + shown) to shown
    }

    fun beginPlay(state: MatchState): MatchState {
        check(state.phase == Phase.PREVIEW) { "Not before a half (phase ${state.phase})" }
        return state.copy(phase = Phase.PICKING)
    }

    fun flip(state: MatchState, index: Int, rng: Random): StepResult {
        check(state.phase == Phase.PICKING) { "Cannot flip in phase ${state.phase}" }
        require(index in state.board.indices) { "No card at $index" }
        val card = state.board[index]
        if (card.revealed) return StepResult(state, emptyList())

        val active = state.turn.side
        val events = mutableListOf<MatchEvent>()
        val substituted = card.type == CardType.SUBSTITUTE
        val type = if (substituted) BoardFactory.substituteFor(rng) else card.type
        if (substituted) events += MatchEvent.Substitution(active, type)

        var s = state.copy(
            board = state.board.toMutableList().also { it[index] = Card(type, revealed = true, substituted = substituted) },
            turn = state.turn.copy(picksLeft = state.turn.picksLeft - 1),
        )
        val me = s.side(active)

        when (type) {
            CardType.GOAL -> {
                s = s.withSide(active, me.copy(goals = me.goals + 1))
                events += MatchEvent.Goal(active, fromPenalty = false)
            }
            CardType.MISS -> events += MatchEvent.Miss(active)
            CardType.YELLOW -> {
                s = s.withSide(active, me.copy(yellows = me.yellows + 1))
                    .copy(turn = s.turn.copy(picksLeft = s.turn.picksLeft + 1))
                events += MatchEvent.FreeKick(active)
            }
            CardType.FOUL -> {
                events += MatchEvent.Foul(active)
                return StepResult(switchTurn(s, FOUL_PICKS, events), events)
            }
            CardType.PENALTY -> {
                events += MatchEvent.PenaltyAwarded(active)
                return StepResult(s.copy(phase = Phase.PENALTY, penaltyPeeks = emptyList()), events)
            }
            CardType.WHISTLE -> return endHalf(s, events)
            CardType.SUBSTITUTE -> error("A substitute is always replaced before it takes effect")
        }
        return StepResult(afterPick(s, events), events)
    }

    /** During a penalty: turns over a face-down card. Finding a goal scores the penalty. */
    fun penaltyPeek(state: MatchState, index: Int): StepResult {
        check(state.phase == Phase.PENALTY) { "No penalty being taken (phase ${state.phase})" }
        require(index in state.board.indices && !state.board[index].revealed) { "Card $index isn't face down" }
        if (index in state.penaltyPeeks) return StepResult(state, emptyList())

        val taker = state.turn.side
        val card = state.board[index]
        if (card.type == CardType.GOAL) {
            val me = state.side(taker)
            val scored = state.withSide(taker, me.copy(goals = me.goals + 1)).copy(
                board = state.board.toMutableList().also { it[index] = card.copy(revealed = true) },
            )
            val events = mutableListOf<MatchEvent>(MatchEvent.Goal(taker, fromPenalty = true))
            return StepResult(finishPenalty(scored, events), events)
        }
        val peeked = state.copy(penaltyPeeks = state.penaltyPeeks + index)
            .withKnown(taker, state.known(taker) + index)
        return StepResult(peeked, emptyList())
    }

    /** Time's up on a penalty without finding a goal. */
    fun penaltyMissed(state: MatchState): StepResult {
        check(state.phase == Phase.PENALTY) { "No penalty being taken (phase ${state.phase})" }
        val events = mutableListOf<MatchEvent>(MatchEvent.PenaltyMissed(state.turn.side))
        return StepResult(finishPenalty(state, events), events)
    }

    /** Deals the second-half board. The away side kicks off the second half. */
    fun startSecondHalf(state: MatchState, rng: Random): MatchState {
        check(state.phase == Phase.HALF_TIME) { "Not half time (phase ${state.phase})" }
        return state.copy(
            half = 2,
            board = BoardFactory.newBoard(rng),
            turn = Turn(Side.AWAY),
            phase = Phase.PREVIEW,
            knownHome = emptySet(),
            knownAway = emptySet(),
        )
    }

    private fun finishPenalty(state: MatchState, events: MutableList<MatchEvent>): MatchState =
        afterPick(state.copy(phase = Phase.PICKING, penaltyPeeks = emptyList()), events)

    private fun afterPick(state: MatchState, events: MutableList<MatchEvent>): MatchState =
        if (state.turn.picksLeft > 0) state else switchTurn(state, 1, events)

    private fun switchTurn(state: MatchState, picks: Int, events: MutableList<MatchEvent>): MatchState {
        val next = state.turn.side.opponent
        events += MatchEvent.TurnChanged(next, picks)
        return state.copy(turn = Turn(next, picks), phase = Phase.PICKING)
    }

    private fun endHalf(state: MatchState, events: MutableList<MatchEvent>): StepResult {
        val over = if (state.half == 1) Phase.HALF_TIME else Phase.FULL_TIME
        events += if (state.half == 1) MatchEvent.HalfTime else MatchEvent.FullTime
        return StepResult(state.copy(phase = over, penaltyPeeks = emptyList()), events)
    }
}
