package com.premierpicks.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AiPlayerTest {
    /** Away (the computer here) to pick, on a board of misses plus the given cards. */
    private fun board(vararg cards: Pair<Int, CardType>): MatchState {
        val types = MutableList(BoardFactory.SIZE) { CardType.MISS }
        cards.forEach { (i, t) -> types[i] = t }
        return MatchEngine.kickOff(Random(0)).copy(board = types.map { Card(it) }, phase = Phase.PICKING, turn = Turn(Side.AWAY))
    }

    @Test
    fun `takes a goal it saw in card help`() {
        val s = board(5 to CardType.GOAL).copy(knownAway = setOf(5))
        repeat(50) { assertEquals(5, AiPlayer.chooseCard(s, Random(it))) }
    }

    @Test
    fun `blows the whistle when it is ahead`() {
        val s = board(9 to CardType.WHISTLE).copy(knownAway = setOf(9), away = SideState(goals = 2))
        assertEquals(9, AiPlayer.chooseCard(s, Random(1)))
    }

    @Test
    fun `avoids a whistle or foul it knows about when not ahead`() {
        val s = board(9 to CardType.WHISTLE, 10 to CardType.FOUL).copy(knownAway = setOf(9, 10))
        repeat(200) {
            val pick = AiPlayer.chooseCard(s, Random(it))
            assertNotEquals(9, pick)
            assertNotEquals(10, pick)
        }
    }

    @Test
    fun `heads for a goal it knows about during its penalty`() {
        val s = board(0 to CardType.PENALTY, 7 to CardType.GOAL).copy(knownAway = setOf(7))
        val pen = MatchEngine.flip(s, 0, Random(2)).state
        val r = AiPlayer.penaltyStep(pen, Random(2))
        assertTrue(MatchEvent.Goal(Side.AWAY, fromPenalty = true) in r.events)
    }

    @Test
    fun `simulated matches always reach full time on the whistle`() {
        var goals = 0
        repeat(300) { seed ->
            val end = MatchSimulator.simulate(Random(seed))
            assertEquals(Phase.FULL_TIME, end.phase)
            assertEquals(2, end.half)
            assertTrue(end.board.any { it.type == CardType.WHISTLE && it.revealed })
            goals += end.home.goals + end.away.goals
        }
        // Football-like scores: a few goals a game on average.
        assertTrue(goals / 300.0 in 1.5..8.0, "average goals ${goals / 300.0}")
    }
}
