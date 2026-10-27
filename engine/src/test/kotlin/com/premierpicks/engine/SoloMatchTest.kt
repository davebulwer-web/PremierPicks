package com.premierpicks.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SoloMatchTest {
    @Test
    fun `each half opens with card help for both sides`() {
        val match = SoloMatch(Side.HOME, rng = Random(1))
        assertTrue(match.isPreview)
        val shown = match.preview(3)
        assertEquals(3, shown.size)
        assertEquals(shown.toSet(), match.state.knownHome)
        assertTrue(match.state.knownAway.size in 1..6)
        match.beginPlay()
        assertTrue(match.isHumanTurn)
    }

    @Test
    fun `without card help nobody previews`() {
        val match = SoloMatch(Side.HOME, cardHelp = false, rng = Random(1))
        assertTrue(match.preview(5).isEmpty())
        assertTrue(match.state.knownAway.isEmpty())
    }

    @Test
    fun `player cannot move during the computer's turn`() {
        val match = SoloMatch(Side.AWAY, rng = Random(1))
        match.preview(1)
        match.beginPlay()
        assertTrue(match.isComputerTurn)
        assertFailsWith<IllegalStateException> { match.flip(0) }
    }

    @Test
    fun `a whole match can be played through to full time`() {
        repeat(200) { seed ->
            val rng = Random(seed)
            val match = SoloMatch(if (seed % 2 == 0) Side.HOME else Side.AWAY, rng = Random(seed))
            var halfTimes = 0
            while (!match.isOver) {
                when {
                    match.isPreview -> { match.preview(rng.nextInt(1, 7)); match.beginPlay() }
                    match.isHalfTime -> { halfTimes++; match.startSecondHalf() }
                    match.isComputerTurn -> match.computerStep()
                    match.isHumanPenalty -> {
                        val unseen = match.state.faceDown - match.state.penaltyPeeks.toSet()
                        if (unseen.isEmpty() || rng.nextInt(4) == 0) match.penaltyTimeUp() else match.penaltyPeek(unseen.random(rng))
                    }
                    else -> match.flip(match.state.faceDown.random(rng))
                }
            }
            assertEquals(1, halfTimes)
            assertEquals(2, match.state.half)
        }
    }
}
