package com.premierpicks.engine

import kotlin.random.Random

enum class CardType {
    GOAL,
    PENALTY,
    HINT,
    MISS,
    SUBSTITUTE,
    /** "Lying down player": an injury that ends your turn and hands unspent picks over. */
    FOUL,
    YELLOW,
    RED,
    /** Half-time / full-time whistle: flipping it ends the half there and then. */
    WHISTLE,
}

/** [substituted] marks a card that was a substitute until it was flipped. */
data class Card(val type: CardType, val revealed: Boolean = false, val substituted: Boolean = false)

object BoardFactory {
    const val SIZE = 18

    /** Chance that each of the two discipline cards is a straight red rather than a yellow. */
    const val RED_CARD_CHANCE = 0.25

    /**
     * Builds a freshly shuffled 18-card board for one half.
     *
     * Fixed cards: 1 penalty, 1 hint, 1 whistle, 2 substitutes, 2 fouls, 2 discipline
     * (yellow or red). That leaves 9 slots for goals (2 or 3) and misses (the rest, 6 or 7).
     */
    fun newBoard(rng: Random): List<Card> {
        val goals = if (rng.nextBoolean()) 2 else 3
        val types = buildList {
            repeat(goals) { add(CardType.GOAL) }
            add(CardType.PENALTY)
            add(CardType.HINT)
            add(CardType.WHISTLE)
            repeat(2) { add(CardType.SUBSTITUTE) }
            repeat(2) { add(CardType.FOUL) }
            repeat(2) { add(disciplineCard(rng)) }
            repeat(SIZE - size) { add(CardType.MISS) }
        }
        return types.shuffled(rng).map { Card(it) }
    }

    private fun disciplineCard(rng: Random) = if (rng.nextDouble() < RED_CARD_CHANCE) CardType.RED else CardType.YELLOW

    /** What a substitute turns into: any card except another substitute or the whistle. */
    fun substituteFor(rng: Random): CardType {
        val pool = listOf(
            CardType.GOAL, CardType.GOAL, CardType.PENALTY, CardType.HINT,
            CardType.FOUL, CardType.FOUL, CardType.YELLOW, CardType.YELLOW,
        ) + List(6) { CardType.MISS }
        val pick = pool.random(rng)
        return if (pick == CardType.YELLOW) disciplineCard(rng) else pick
    }
}
