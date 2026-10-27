package com.premierpicks.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LeagueTest {
    private val fixtures = FixtureGenerator.doubleRoundRobin(DEFAULT_TEAMS)

    @Test
    fun `22 teams give 42 rounds of 11 matches`() {
        assertEquals(LEAGUE_SIZE, DEFAULT_TEAMS.size)
        assertEquals(42, fixtures.size)
        assertTrue(fixtures.all { it.size == 11 })
    }

    @Test
    fun `every team plays 42 games, 21 home and 21 away`() {
        val all = fixtures.flatten()
        for (team in DEFAULT_TEAMS) {
            assertEquals(21, all.count { it.home == team }, "$team home")
            assertEquals(21, all.count { it.away == team }, "$team away")
        }
    }

    @Test
    fun `every pairing happens once each way`() {
        val pairs = fixtures.flatten().map { it.home to it.away }
        assertEquals(pairs.size, pairs.toSet().size)
        assertEquals(22 * 21, pairs.size)
    }

    @Test
    fun `nobody plays twice in a round`() {
        for (round in fixtures) {
            val teams = round.flatMap { listOf(it.home, it.away) }
            assertEquals(teams.size, teams.toSet().size)
        }
    }

    @Test
    fun `table ranks by points, goal difference, goals for, then name`() {
        val table = LeagueTable(listOf("A", "B", "C", "D"))
        table.record(Fixture(1, "A", "B"), 3, 0) // A 3pts +3
        table.record(Fixture(1, "C", "D"), 1, 0) // C 3pts +1
        table.record(Fixture(2, "B", "D"), 2, 2) // B, D 1pt
        val order = table.standings().map { it.team }
        assertEquals(listOf("A", "C", "D", "B"), order)
        val a = table.standings().first()
        assertEquals(3, a.points)
        assertEquals(3, a.goalDifference)
    }

    @Test
    fun `points for a win, draw and defeat can be changed`() {
        val table = LeagueTable(listOf("A", "B", "C"), PointsRules(win = 2, draw = 1, loss = 0))
        table.record(Fixture(1, "A", "B"), 1, 0)
        table.record(Fixture(2, "B", "C"), 1, 1)
        assertEquals(listOf(2, 1, 1), table.standings().map { it.points })
    }

    @Test
    fun `odd-sized leagues give each team a rest`() {
        val five = FixtureGenerator.doubleRoundRobin(listOf("A", "B", "C", "D", "E"))
        assertEquals(10, five.size)
        assertTrue(five.all { it.size == 2 })
    }
}
