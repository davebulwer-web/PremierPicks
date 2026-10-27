package com.premierpicks.engine

import kotlin.random.Random

/**
 * A human-vs-computer match driven one action at a time, so the app can pause
 * between the computer's moves and animate each card it flips.
 */
class SoloMatch(val humanSide: Side = Side.HOME, private val rng: Random = Random.Default) {
    var state: MatchState = MatchEngine.kickOff(rng)
        private set

    val computerSide: Side get() = humanSide.opponent

    val isOver: Boolean get() = state.phase == Phase.FULL_TIME
    val isHalfTime: Boolean get() = state.phase == Phase.HALF_TIME

    private val inPlay: Boolean get() = state.phase == Phase.AWAITING_BUDGET || state.phase == Phase.FLIPPING

    val isHumanTurn: Boolean get() = inPlay && state.turn.side == humanSide
    val isComputerTurn: Boolean get() = inPlay && state.turn.side == computerSide

    fun chooseBudget(picks: Int) {
        check(isHumanTurn) { "Not the player's turn" }
        state = MatchEngine.chooseBudget(state, picks)
    }

    fun flip(index: Int): List<MatchEvent> {
        check(isHumanTurn) { "Not the player's turn" }
        return apply(MatchEngine.flip(state, index, rng))
    }

    /** Makes the computer's next single move. */
    fun computerStep(): List<MatchEvent> {
        check(isComputerTurn) { "Not the computer's turn" }
        return apply(AiPlayer.step(state, rng))
    }

    fun startSecondHalf() {
        state = MatchEngine.startSecondHalf(state, rng)
    }

    private fun apply(result: StepResult): List<MatchEvent> {
        state = result.state
        return result.events
    }
}
