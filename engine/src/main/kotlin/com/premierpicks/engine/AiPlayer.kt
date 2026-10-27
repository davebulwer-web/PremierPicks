package com.premierpicks.engine

import kotlin.random.Random

/** Risk-based computer opponent: cautious when level or ahead, reckless when chasing late on. */
object AiPlayer {

    fun chooseBudget(state: MatchState, rng: Random): Int {
        val me = state.turn.side
        val diff = state.side(me).goals - state.side(me.opponent).goals
        val late = state.half == 2 && state.unrevealed.size <= BoardFactory.SIZE / 2
        val wanted = when {
            diff >= 0 -> rng.nextInt(1, 3) // 1..2
            late -> rng.nextInt(4, 7) // 4..6
            else -> rng.nextInt(2, 5) // 2..4
        }
        return wanted.coerceIn(1, state.side(me).maxPicks)
    }

    /** Takes any goal a hint has exposed, otherwise a random face-down card. */
    fun chooseCard(state: MatchState, rng: Random): Int {
        val hinted = state.highlightedGoals.filter { !state.board[it].revealed }
        return if (hinted.isNotEmpty()) hinted.first() else state.unrevealed.random(rng)
    }

    /** Plays whole turns until the match needs input from someone other than [side]. */
    fun playTurn(state: MatchState, side: Side, rng: Random): StepResult {
        var s = state
        val events = mutableListOf<MatchEvent>()
        while (s.turn.side == side && s.phase in setOf(Phase.AWAITING_BUDGET, Phase.FLIPPING)) {
            if (s.phase == Phase.AWAITING_BUDGET) {
                s = MatchEngine.chooseBudget(s, chooseBudget(s, rng))
            } else {
                val step = MatchEngine.flip(s, chooseCard(s, rng), rng)
                s = step.state
                events += step.events
            }
        }
        return StepResult(s, events)
    }
}

object MatchSimulator {
    /** Plays a full AI-vs-AI match, used for the rest of the league's fixtures. */
    fun simulate(rng: Random): MatchState {
        var s = MatchEngine.kickOff(rng)
        while (s.phase != Phase.FULL_TIME) {
            s = when (s.phase) {
                Phase.HALF_TIME -> MatchEngine.startSecondHalf(s, rng)
                else -> AiPlayer.playTurn(s, s.turn.side, rng).state
            }
        }
        return s
    }
}
