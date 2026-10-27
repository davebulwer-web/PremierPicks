package com.premierpicks.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MatchEngineTest {
    private val rng = Random(1)

    /** A first-half state in play with a hand-made board, home to pick. Unlisted cards are misses. */
    private fun stateWith(vararg types: CardType): MatchState {
        val board = types.map { Card(it) } + List(BoardFactory.SIZE - types.size) { Card(CardType.MISS) }
        return MatchEngine.kickOff(rng).copy(board = board, phase = Phase.PICKING)
    }

    @Test
    fun `a half starts with the card-help preview`() {
        val s = MatchEngine.kickOff(rng)
        assertEquals(Phase.PREVIEW, s.phase)
        assertFailsWith<IllegalStateException> { MatchEngine.flip(s, 0, rng) }
        assertEquals(Phase.PICKING, MatchEngine.beginPlay(s).phase)
    }

    @Test
    fun `preview shows a side that many different face-down cards`() {
        val s = MatchEngine.kickOff(rng)
        val (after, shown) = MatchEngine.preview(s, Side.HOME, 4, rng)
        assertEquals(4, shown.toSet().size)
        assertEquals(shown.toSet(), after.knownHome)
        assertTrue(after.knownAway.isEmpty())
        assertFailsWith<IllegalArgumentException> { MatchEngine.preview(s, Side.HOME, 7, rng) }
    }

    @Test
    fun `sides take turns one card at a time`() {
        val r = MatchEngine.flip(stateWith(), 0, rng)
        assertEquals(Turn(Side.AWAY, 1), r.state.turn)
        assertTrue(MatchEvent.TurnChanged(Side.AWAY, 1) in r.events)
        assertEquals(Side.HOME, MatchEngine.flip(r.state, 1, rng).state.turn.side)
    }

    @Test
    fun `goal scores for the side that flips it`() {
        val r = MatchEngine.flip(stateWith(CardType.GOAL), 0, rng)
        assertEquals(1, r.state.home.goals)
        assertEquals(0, r.state.away.goals)
        assertTrue(MatchEvent.Goal(Side.HOME, fromPenalty = false) in r.events)
    }

    @Test
    fun `flipping a revealed card does nothing`() {
        val s = MatchEngine.flip(stateWith(), 0, rng).state
        val again = MatchEngine.flip(s, 0, rng)
        assertEquals(s, again.state)
        assertTrue(again.events.isEmpty())
    }

    @Test
    fun `a yellow card is a free kick - pick again`() {
        val r = MatchEngine.flip(stateWith(CardType.YELLOW), 0, rng)
        assertEquals(Turn(Side.HOME, 1), r.state.turn)
        assertEquals(1, r.state.home.yellows)
        assertTrue(MatchEvent.FreeKick(Side.HOME) in r.events)
    }

    @Test
    fun `a foul hands the opponent two picks`() {
        val r = MatchEngine.flip(stateWith(CardType.FOUL), 0, rng)
        assertEquals(Turn(Side.AWAY, 2), r.state.turn)
        val afterOne = MatchEngine.flip(r.state, 1, rng).state
        assertEquals(Turn(Side.AWAY, 1), afterOne.turn)
        assertEquals(Side.HOME, MatchEngine.flip(afterOne, 2, rng).state.turn.side)
    }

    @Test
    fun `a foul ends the turn even with picks to spare`() {
        val s = stateWith(CardType.MISS, CardType.FOUL).copy(turn = Turn(Side.HOME, 2))
        val r = MatchEngine.flip(s, 1, rng)
        assertEquals(Turn(Side.AWAY, 2), r.state.turn)
    }

    @Test
    fun `a penalty is scored by finding a goal card`() {
        val s = MatchEngine.flip(stateWith(CardType.PENALTY, CardType.MISS, CardType.GOAL), 0, rng).state
        assertEquals(Phase.PENALTY, s.phase)
        assertEquals(Side.HOME, s.turn.side)

        val miss = MatchEngine.penaltyPeek(s, 1)
        assertEquals(listOf(1), miss.state.penaltyPeeks)
        assertTrue(1 in miss.state.knownHome, "the taker remembers what they saw")
        assertTrue(!miss.state.board[1].revealed)

        val goal = MatchEngine.penaltyPeek(miss.state, 2)
        assertTrue(MatchEvent.Goal(Side.HOME, fromPenalty = true) in goal.events)
        assertEquals(1, goal.state.home.goals)
        assertTrue(goal.state.board[2].revealed, "the goal found is used up")
        assertEquals(Phase.PICKING, goal.state.phase)
        assertEquals(Side.AWAY, goal.state.turn.side)
        assertTrue(goal.state.penaltyPeeks.isEmpty())
    }

    @Test
    fun `a penalty runs out of time`() {
        val s = MatchEngine.flip(stateWith(CardType.PENALTY), 0, rng).state
        val r = MatchEngine.penaltyMissed(MatchEngine.penaltyPeek(s, 3).state)
        assertTrue(MatchEvent.PenaltyMissed(Side.HOME) in r.events)
        assertEquals(0, r.state.home.goals)
        assertEquals(Turn(Side.AWAY, 1), r.state.turn)
        assertTrue(r.state.board.count { it.revealed } == 1)
    }

    @Test
    fun `the whistle ends the first half with cards still face down`() {
        val r = MatchEngine.flip(stateWith(CardType.MISS, CardType.WHISTLE), 1, rng)
        assertEquals(Phase.HALF_TIME, r.state.phase)
        assertTrue(MatchEvent.HalfTime in r.events)
        assertEquals(17, r.state.faceDown.size)
    }

    @Test
    fun `second half is a new board, away kicks off, memories wiped`() {
        val halfTime = MatchEngine.flip(stateWith(CardType.WHISTLE).copy(knownHome = setOf(4)), 0, rng).state
        val second = MatchEngine.startSecondHalf(halfTime, rng)
        assertEquals(2, second.half)
        assertEquals(Phase.PREVIEW, second.phase)
        assertEquals(Turn(Side.AWAY, 1), second.turn)
        assertTrue(second.board.none { it.revealed })
        assertTrue(second.knownHome.isEmpty())

        val whistle = second.board.indexOfFirst { it.type == CardType.WHISTLE }
        val end = MatchEngine.flip(MatchEngine.beginPlay(second), whistle, rng)
        assertEquals(Phase.FULL_TIME, end.state.phase)
        assertTrue(MatchEvent.FullTime in end.events)
    }

    @Test
    fun `a substitute is swapped for another card which takes effect`() {
        repeat(200) { seed ->
            val r = MatchEngine.flip(stateWith(CardType.SUBSTITUTE), 0, Random(seed))
            val card = r.state.board[0]
            assertTrue(card.revealed && card.substituted && card.type != CardType.SUBSTITUTE)
            assertEquals(MatchEvent.Substitution(Side.HOME, card.type), r.events.first())
            when (card.type) {
                CardType.GOAL -> assertEquals(1, r.state.home.goals)
                CardType.YELLOW -> assertEquals(Turn(Side.HOME, 1), r.state.turn)
                CardType.FOUL -> assertEquals(Turn(Side.AWAY, 2), r.state.turn)
                CardType.PENALTY -> assertEquals(Phase.PENALTY, r.state.phase)
                else -> assertEquals(Side.AWAY, r.state.turn.side)
            }
        }
    }
}
