package com.premierpicks.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardTest {
    @Test
    fun `every board has 18 face-down cards and exactly one whistle`() {
        repeat(500) { seed ->
            val board = BoardFactory.newBoard(Random(seed))
            assertEquals(18, board.size)
            assertTrue(board.none { it.revealed })
            assertEquals(1, board.count { it.type == CardType.WHISTLE })
        }
    }

    @Test
    fun `boards mix every kind of card`() {
        val rng = Random(3)
        val seen = List(200) { BoardFactory.newBoard(rng) }.flatten().groupingBy { it.type }.eachCount()
        assertEquals(CardType.entries.toSet(), seen.keys)
        // Goals and misses are the commonest cards.
        assertTrue(seen.getValue(CardType.GOAL) > seen.getValue(CardType.PENALTY))
        assertTrue(seen.getValue(CardType.MISS) > seen.getValue(CardType.FOUL))
    }

    @Test
    fun `same seed deals the same board`() {
        assertEquals(BoardFactory.newBoard(Random(42)), BoardFactory.newBoard(Random(42)))
    }

    @Test
    fun `a substitute never becomes another substitute or the whistle`() {
        val rng = Random(11)
        val drawn = List(5_000) { BoardFactory.substituteFor(rng) }.toSet()
        assertTrue(CardType.SUBSTITUTE !in drawn)
        assertTrue(CardType.WHISTLE !in drawn)
        assertTrue(CardType.GOAL in drawn && CardType.FOUL in drawn)
    }
}
