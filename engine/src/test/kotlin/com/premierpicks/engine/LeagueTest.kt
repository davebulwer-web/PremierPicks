package com.premierpicks.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LeagueTest {
    private val fixtures = FixtureGenerator.doubleRoundRobin(DEFAULT_TEAMS)

    @Test
    fun `21 teams give 42 rounds of 10 matches`() {
        assertEquals(LEAGUE_SIZE, DEFAULT_TEAMS.size)
        assertEquals(42, fixtures.size)
        assertTrue(fixtures.all { it.size == 10 })
    }

    @Test
    fun `every team plays 40 games, 20 home and 20 away`() {
        val all = fixtures.flatten()
        for (team in DEFAULT_TEAMS) {
            assertEquals(20, all.count { it.home == team }, "$team home")
            assertEquals(20, all.count { it.away == team }, "$team away")
        }
    }

    @Test
    fun `every pairing happens once each way`() {
        val pairs = fixtures.flatten().map { it.home to it.away }
        assertEquals(pairs.size, pairs.toSet().size)
        assertEquals(21 * 20, pairs.size)
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
}
