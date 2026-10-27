package com.premierpicks.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MatchEngineTest {
    private val rng = Random(1)

    /** A first-half state with a hand-made board, home side to choose a budget. */
    private fun stateWith(vararg types: CardType): MatchState {
        val board = types.map { Card(it) } + List(BoardFactory.SIZE - types.size) { Card(CardType.MISS) }
        return MatchEngine.kickOff(rng).copy(board = board)
    }

    private fun indexOf(state: MatchState, type: CardType) = state.board.indexOfFirst { it.type == type && !it.revealed }

    @Test
    fun `cannot flip before choosing a budget`() {
        val s = stateWith(CardType.GOAL)
        assertFailsWith<IllegalStateException> { MatchEngine.flip(s, 0, rng) }
    }

    @Test
    fun `budget must be between 1 and the side's max`() {
        val s = stateWith()
        assertFailsWith<IllegalArgumentException> { MatchEngine.chooseBudget(s, 0) }
        assertFailsWith<IllegalArgumentException> { MatchEngine.chooseBudget(s, 7) }
        assertEquals(Phase.FLIPPING, MatchEngine.chooseBudget(s, 6).phase)
    }

    @Test
    fun `goal scores for the active side`() {
        val s = MatchEngine.chooseBudget(stateWith(CardType.GOAL), 2)
        val r = MatchEngine.flip(s, 0, rng)
        assertEquals(1, r.state.home.goals)
        assertEquals(0, r.state.away.goals)
        assertTrue(MatchEvent.Goal(Side.HOME, fromPenalty = false) in r.events)
    }

    @Test
    fun `flipping a revealed card does nothing and costs no pick`() {
        val s = MatchEngine.flip(MatchEngine.chooseBudget(stateWith(), 3), 0, rng).state
        val again = MatchEngine.flip(s, 0, rng)
        assertEquals(s, again.state)
        assertTrue(again.events.isEmpty())
    }

    @Test
    fun `turn passes to the opponent when the budget is spent`() {
        var s = MatchEngine.chooseBudget(stateWith(), 2)
        s = MatchEngine.flip(s, 0, rng).state
        assertEquals(Side.HOME, s.turn.side)
        val r = MatchEngine.flip(s, 1, rng)
        assertEquals(Side.AWAY, r.state.turn.side)
        assertEquals(Phase.AWAITING_BUDGET, r.state.phase)
        assertTrue(MatchEvent.TurnChanged(Side.AWAY, 0) in r.events)
    }

    @Test
    fun `foul forfeits unspent picks to the opponent's next turn`() {
        // Budget 5: one miss, then the foul. Two picks used, three forfeited.
        var s = MatchEngine.chooseBudget(stateWith(CardType.MISS, CardType.FOUL), 5)
        s = MatchEngine.flip(s, 0, rng).state
        val r = MatchEngine.flip(s, 1, rng)
        assertTrue(MatchEvent.Foul(Side.HOME, forfeitedPicks = 3) in r.events)
        assertEquals(Side.AWAY, r.state.turn.side)
        assertEquals(3, r.state.turn.bonus)
        assertEquals(0, r.state.away.bonusPicks, "bonus moves onto the turn, not left on the side")

        val awayTurn = MatchEngine.chooseBudget(r.state, 2)
        assertEquals(5, awayTurn.turn.remaining)
    }

    @Test
    fun `foul on the last pick forfeits nothing`() {
        val s = MatchEngine.chooseBudget(stateWith(CardType.FOUL), 1)
        val r = MatchEngine.flip(s, 0, rng)
        assertTrue(MatchEvent.Foul(Side.HOME, forfeitedPicks = 0) in r.events)
        assertEquals(0, r.state.turn.bonus)
    }

    @Test
    fun `two yellows or one red cut the max budget by one each`() {
        var s = MatchEngine.chooseBudget(stateWith(CardType.YELLOW, CardType.YELLOW, CardType.RED), 3)
        s = MatchEngine.flip(s, 0, rng).state
        assertEquals(6, s.home.maxPicks)
        val second = MatchEngine.flip(s, 1, rng)
        assertTrue(MatchEvent.SentOff(Side.HOME, 5) in second.events)
        val third = MatchEngine.flip(second.state, 2, rng)
        assertEquals(4, third.state.home.maxPicks)
        assertFailsWith<IllegalArgumentException> {
            // Away's turn now; hand it back to home to check the cap is enforced.
            MatchEngine.chooseBudget(third.state.copy(turn = Turn(Side.HOME)), 5)
        }
    }

    @Test
    fun `max picks never drops below one`() {
        assertEquals(1, SideState(reds = 10).maxPicks)
    }

    @Test
    fun `penalty scores about 80 percent of the time`() {
        val s = MatchEngine.chooseBudget(stateWith(CardType.PENALTY), 1)
        val r = Random(7)
        val scored = (1..10_000).count { MatchEngine.flip(s, 0, r).state.home.goals == 1 }
        assertTrue(scored in 7_700..8_300, "scored $scored")
    }

    @Test
    fun `hint highlights the remaining goals`() {
        val s0 = stateWith(CardType.HINT, CardType.GOAL, CardType.MISS, CardType.GOAL)
        val r = MatchEngine.flip(MatchEngine.chooseBudget(s0, 3), 0, rng)
        assertEquals(setOf(1, 3), r.state.highlightedGoals)
        val after = MatchEngine.flip(r.state, 1, rng).state
        assertEquals(setOf(3), after.highlightedGoals)
    }

    @Test
    fun `clearing the board ends the half, then the match`() {
        var s = MatchEngine.kickOff(Random(3))
        // Reveal all but one card directly, then flip the last.
        val last = s.board.indices.last { s.board[it].type == CardType.MISS }
        s = s.copy(board = s.board.mapIndexed { i, c -> if (i == last) c else c.copy(revealed = true) })
        s = MatchEngine.chooseBudget(s, 6)
        val half = MatchEngine.flip(s, last, rng)
        assertEquals(Phase.HALF_TIME, half.state.phase)
        assertTrue(MatchEvent.HalfTime in half.events)

        var second = MatchEngine.startSecondHalf(half.state, Random(4))
        assertEquals(2, second.half)
        assertEquals(Side.AWAY, second.turn.side)
        assertTrue(second.board.none { it.revealed })

        val last2 = second.board.indices.last { second.board[it].type == CardType.MISS }
        second = second.copy(board = second.board.mapIndexed { i, c -> if (i == last2) c else c.copy(revealed = true) })
        val end = MatchEngine.flip(MatchEngine.chooseBudget(second, 1), last2, rng)
        assertEquals(Phase.FULL_TIME, end.state.phase)
    }

    @Test
    fun `discipline carries over into the second half`() {
        val booked = MatchEngine.kickOff(rng).copy(
            home = SideState(reds = 1),
            phase = Phase.HALF_TIME,
        )
        assertEquals(5, MatchEngine.startSecondHalf(booked, rng).home.maxPicks)
    }
}
