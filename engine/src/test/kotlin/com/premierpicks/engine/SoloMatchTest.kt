package com.premierpicks.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SoloMatchTest {
    @Test
    fun `player kicks off at home and the computer cannot move first`() {
        val match = SoloMatch(Side.HOME, Random(1))
        assertTrue(match.isHumanTurn)
        assertFailsWith<IllegalStateException> { match.computerStep() }
    }

    @Test
    fun `player cannot move during the computer's turn`() {
        val match = SoloMatch(Side.AWAY, Random(1))
        assertTrue(match.isComputerTurn)
        assertFailsWith<IllegalStateException> { match.chooseBudget(1) }
    }

    @Test
    fun `a whole match can be played through to full time`() {
        repeat(100) { seed ->
            val rng = Random(seed)
            val match = SoloMatch(Side.HOME, Random(seed))
            var halfTimes = 0
            while (!match.isOver) {
                when {
                    match.isHalfTime -> { halfTimes++; match.startSecondHalf() }
                    match.isComputerTurn -> match.computerStep()
                    match.state.phase == Phase.AWAITING_BUDGET ->
                        match.chooseBudget(rng.nextInt(1, match.state.side(Side.HOME).maxPicks + 1))
                    else -> match.flip(match.state.unrevealed.random(rng))
                }
            }
            assertEquals(1, halfTimes)
            assertEquals(2, match.state.half)
        }
    }
}
