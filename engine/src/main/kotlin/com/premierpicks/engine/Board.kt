package com.premierpicks.engine

import kotlin.random.Random

enum class CardType {
    GOAL,
    PENALTY,
    HINT,
    MISS,
    SUBSTITUTE,
    FOUL,
    YELLOW,
    RED,
}

data class Card(val type: CardType, val revealed: Boolean = false)

object BoardFactory {
    const val SIZE = 18

    /** Chance that each of the two discipline cards is a straight red rather than a yellow. */
    const val RED_CARD_CHANCE = 0.25

    /**
     * Builds a freshly shuffled 18-card board for one half.
     *
     * Fixed cards: 1 penalty, 1 hint, 2 substitutes, 2 fouls, 2 discipline (yellow or red).
     * That leaves 10 slots for goals (2 or 3) and misses (the rest, 7 or 8).
     */
    fun newBoard(rng: Random): List<Card> {
        val goals = if (rng.nextBoolean()) 2 else 3
        val types = buildList {
            repeat(goals) { add(CardType.GOAL) }
            add(CardType.PENALTY)
            add(CardType.HINT)
            repeat(2) { add(CardType.SUBSTITUTE) }
            repeat(2) { add(CardType.FOUL) }
            repeat(2) { add(if (rng.nextDouble() < RED_CARD_CHANCE) CardType.RED else CardType.YELLOW) }
            repeat(SIZE - size) { add(CardType.MISS) }
        }
        return types.shuffled(rng).map { Card(it) }
    }
}
