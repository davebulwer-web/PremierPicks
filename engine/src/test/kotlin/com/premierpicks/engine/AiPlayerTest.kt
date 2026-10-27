package com.premierpicks.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiPlayerTest {
    private val base = MatchEngine.kickOff(Random(0)).copy(turn = Turn(Side.AWAY))

    @Test
    fun `plays safe when level or ahead`() {
        val ahead = base.copy(away = SideState(goals = 2))
        repeat(200) { seed ->
            assertTrue(AiPlayer.chooseBudget(base, Random(seed)) in 1..2)
            assertTrue(AiPlayer.chooseBudget(ahead, Random(seed)) in 1..2)
        }
    }

    @Test
    fun `gambles when losing late in the second half`() {
        val late = base.copy(
            half = 2,
            home = SideState(goals = 1),
            board = base.board.mapIndexed { i, c -> c.copy(revealed = i < 10) },
        )
        repeat(200) { seed -> assertTrue(AiPlayer.chooseBudget(late, Random(seed)) in 4..6) }
    }

    @Test
    fun `respects a reduced max after a sending-off`() {
        val late = base.copy(
            half = 2,
            home = SideState(goals = 1),
            away = SideState(reds = 2),
            board = base.board.mapIndexed { i, c -> c.copy(revealed = i < 10) },
        )
        repeat(200) { seed -> assertTrue(AiPlayer.chooseBudget(late, Random(seed)) <= 4) }
    }

    @Test
    fun `takes a hinted goal when one is showing`() {
        val goal = base.board.indexOfFirst { it.type == CardType.GOAL }
        val hinted = base.copy(highlightedGoals = setOf(goal))
        assertEquals(goal, AiPlayer.chooseCard(hinted, Random(9)))
    }

    @Test
    fun `simulated matches always reach full time on the whistle`() {
        repeat(300) { seed ->
            val end = MatchSimulator.simulate(Random(seed))
            assertEquals(Phase.FULL_TIME, end.phase)
            assertEquals(2, end.half)
            assertTrue(end.board.any { it.type == CardType.WHISTLE && it.revealed })
            assertTrue(end.home.goals in 0..8 && end.away.goals in 0..8)
        }
    }
}
