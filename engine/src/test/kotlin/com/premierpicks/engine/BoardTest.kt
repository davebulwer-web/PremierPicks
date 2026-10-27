package com.premierpicks.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardTest {
    @Test
    fun `every board has 18 face-down cards in the spec's proportions`() {
        repeat(500) { seed ->
            val board = BoardFactory.newBoard(Random(seed))
            val counts = board.groupingBy { it.type }.eachCount()
            assertEquals(18, board.size)
            assertTrue(board.none { it.revealed })
            assertTrue(counts[CardType.GOAL] in 2..3)
            assertEquals(1, counts[CardType.PENALTY])
            assertEquals(1, counts[CardType.HINT])
            assertEquals(2, counts[CardType.SUBSTITUTE])
            assertEquals(2, counts[CardType.FOUL])
            assertEquals(2, (counts[CardType.YELLOW] ?: 0) + (counts[CardType.RED] ?: 0))
            assertTrue(counts[CardType.MISS] in 7..8)
        }
    }

    @Test
    fun `same seed deals the same board`() {
        assertEquals(BoardFactory.newBoard(Random(42)), BoardFactory.newBoard(Random(42)))
    }
}
