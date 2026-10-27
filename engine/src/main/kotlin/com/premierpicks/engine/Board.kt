package com.premierpicks.engine

import kotlin.random.Random

enum class CardType {
    /** The big ball in front of the net. */
    GOAL,
    /** Ball on the spot: the taker gets a few seconds to hunt for a goal card. */
    PENALTY,
    /** Shot saved or wide. */
    MISS,
    /** Yellow card, i.e. a free kick: the same side picks again. */
    YELLOW,
    /** The lying-down player: the opponent takes over with two picks. */
    FOUL,
    /** Swapped for another card, which then takes effect. */
    SUBSTITUTE,
    /** Half-time / full-time whistle: flipping it ends the half there and then. */
    WHISTLE,
}

/** [substituted] marks a card that was a substitute until it was flipped. */
data class Card(val type: CardType, val revealed: Boolean = false, val substituted: Boolean = false)

object BoardFactory {
    const val SIZE = 18

    /**
     * Relative frequency of each card on the board, besides the single whistle. The original
     * mixed its boards differently every half; these weights give roughly the scorelines
     * its results screens show (most teams scoring 1 to 4).
     */
    private val weights = mapOf(
        CardType.GOAL to 10,
        CardType.MISS to 10,
        CardType.PENALTY to 6,
        CardType.YELLOW to 4,
        CardType.FOUL to 3,
        CardType.SUBSTITUTE to 3,
    )

    /** A freshly shuffled board for one half: one whistle and 17 cards drawn from [weights]. */
    fun newBoard(rng: Random): List<Card> {
        val types = List(SIZE - 1) { draw(rng, weights) } + CardType.WHISTLE
        return types.shuffled(rng).map { Card(it) }
    }

    /** What a substitute turns into: any card except another substitute or the whistle. */
    fun substituteFor(rng: Random): CardType = draw(rng, weights - CardType.SUBSTITUTE)

    private fun draw(rng: Random, pool: Map<CardType, Int>): CardType {
        var roll = rng.nextInt(pool.values.sum())
        for ((type, weight) in pool) {
            if (roll < weight) return type
            roll -= weight
        }
        error("unreachable")
    }
}
