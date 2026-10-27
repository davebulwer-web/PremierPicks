package com.premierpicks.engine

import kotlin.random.Random

/**
 * A human-vs-computer match driven one action at a time, so the app can pause
 * between the computer's moves and animate each card it flips.
 */
class SoloMatch(
    val humanSide: Side = Side.HOME,
    val cardHelp: Boolean = true,
    private val rng: Random = Random.Default,
) {
    var state: MatchState = MatchEngine.kickOff(rng)
        private set

    val computerSide: Side get() = humanSide.opponent

    val isPreview: Boolean get() = state.phase == Phase.PREVIEW
    val isOver: Boolean get() = state.phase == Phase.FULL_TIME
    val isHalfTime: Boolean get() = state.phase == Phase.HALF_TIME

    private val inPlay: Boolean get() = state.phase == Phase.PICKING || state.phase == Phase.PENALTY

    val isHumanTurn: Boolean get() = inPlay && state.turn.side == humanSide
    val isComputerTurn: Boolean get() = inPlay && state.turn.side == computerSide
    val isHumanPenalty: Boolean get() = isHumanTurn && state.phase == Phase.PENALTY

    /**
     * Card help before a half: the player sees [count] cards (where their buzzer stopped)
     * and the computer gets its own preview. Returns the cards the player may look at.
     */
    fun preview(count: Int): List<Int> {
        check(isPreview) { "Not before a half" }
        val (afterHuman, shown) = MatchEngine.preview(state, humanSide, if (cardHelp) count else 0, rng)
        val cpuCount = if (cardHelp) AiPlayer.previewCount(rng) else 0
        state = MatchEngine.preview(afterHuman, computerSide, cpuCount, rng).first
        return shown
    }

    fun beginPlay() {
        state = MatchEngine.beginPlay(state)
    }

    fun flip(index: Int): List<MatchEvent> {
        check(isHumanTurn && state.phase == Phase.PICKING) { "Not the player's pick" }
        return apply(MatchEngine.flip(state, index, rng))
    }

    fun penaltyPeek(index: Int): List<MatchEvent> {
        check(isHumanPenalty) { "Not the player's penalty" }
        return apply(MatchEngine.penaltyPeek(state, index))
    }

    fun penaltyTimeUp(): List<MatchEvent> {
        check(isHumanPenalty) { "Not the player's penalty" }
        return apply(MatchEngine.penaltyMissed(state))
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
