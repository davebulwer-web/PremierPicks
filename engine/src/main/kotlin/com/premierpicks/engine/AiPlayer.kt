package com.premierpicks.engine

import kotlin.random.Random

/**
 * The computer manager. It remembers the cards it has seen (card help and penalty hunts)
 * and uses them: it takes goals it knows about, avoids the whistle unless ending the half
 * suits it, and steers clear of fouls and misses.
 */
object AiPlayer {

    /** Where the computer's card-help buzzer stops. */
    fun previewCount(rng: Random): Int = rng.nextInt(1, MAX_PREVIEW + 1)

    fun chooseCard(state: MatchState, rng: Random): Int {
        val me = state.turn.side
        val ahead = state.side(me).goals > state.side(me.opponent).goals
        val known = state.known(me)
        val candidates = state.faceDown
        // Lower is better. Unknown cards sit in the middle.
        fun rank(index: Int): Int {
            if (index !in known) return 4
            return when (state.board[index].type) {
                CardType.GOAL -> 0
                CardType.WHISTLE -> if (ahead) 1 else 9
                CardType.YELLOW -> 2
                CardType.PENALTY -> 3
                CardType.SUBSTITUTE -> 5
                CardType.MISS -> 6
                CardType.FOUL -> 8
            }
        }
        val best = candidates.minOf { rank(it) }
        return candidates.filter { rank(it) == best }.random(rng)
    }

    /** One move during the computer's own penalty: look at a card, or give up when time runs out. */
    fun penaltyStep(state: MatchState, rng: Random): StepResult {
        val me = state.turn.side
        val unseen = state.faceDown - state.penaltyPeeks.toSet()
        val knownGoal = unseen.firstOrNull { it in state.known(me) && state.board[it].type == CardType.GOAL }
        val outOfTime = state.penaltyPeeks.size >= MAX_PENALTY_PEEKS ||
            (state.penaltyPeeks.isNotEmpty() && rng.nextDouble() < GIVE_UP_CHANCE)
        return when {
            knownGoal != null -> MatchEngine.penaltyPeek(state, knownGoal)
            outOfTime || unseen.isEmpty() -> MatchEngine.penaltyMissed(state)
            else -> MatchEngine.penaltyPeek(state, (unseen - state.known(me)).ifEmpty { unseen }.random(rng))
        }
    }

    /** Takes a single action for the side whose turn it is. */
    fun step(state: MatchState, rng: Random): StepResult = when (state.phase) {
        Phase.PICKING -> MatchEngine.flip(state, chooseCard(state, rng), rng)
        Phase.PENALTY -> penaltyStep(state, rng)
        else -> error("No move to make in phase ${state.phase}")
    }

    private const val MAX_PENALTY_PEEKS = 5
    private const val GIVE_UP_CHANCE = 0.3
}

object MatchSimulator {
    /** Plays a full computer-vs-computer match, used for the rest of the league's fixtures. */
    fun simulate(rng: Random, cardHelp: Boolean = true): MatchState {
        var s = MatchEngine.kickOff(rng)
        while (s.phase != Phase.FULL_TIME) {
            s = when (s.phase) {
                Phase.PREVIEW -> {
                    for (side in Side.entries) {
                        s = MatchEngine.preview(s, side, if (cardHelp) AiPlayer.previewCount(rng) else 0, rng).first
                    }
                    MatchEngine.beginPlay(s)
                }
                Phase.HALF_TIME -> MatchEngine.startSecondHalf(s, rng)
                else -> AiPlayer.step(s, rng).state
            }
        }
        return s
    }
}
