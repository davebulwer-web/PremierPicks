package com.premierpicks.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SeasonTest {
    private val player = DEFAULT_TEAMS.first()

    /** Plays a round, giving the player a 2-1 win whenever they have a match. */
    private fun Season.playRound(rng: Random) {
        val mine = playerFixture
        when {
            mine == null -> completeRound(null, null, rng)
            playerSide(mine) == Side.HOME -> completeRound(2, 1, rng)
            else -> completeRound(1, 2, rng)
        }
    }

    @Test
    fun `a full season plays 42 rounds and every team plays 40 games`() {
        val season = Season.new(player)
        val rng = Random(5)
        var rests = 0
        while (!season.isFinished) {
            if (season.playerFixture == null) rests++
            season.playRound(rng)
        }
        assertEquals(42, season.roundsPlayed)
        assertEquals(2, rests)
        assertNull(season.currentRound)
        assertTrue(season.table().all { it.played == 40 })
        val me = season.table().first { it.team == player }
        assertEquals(40, me.won)
        assertEquals(1, season.playerPosition())
    }

    @Test
    fun `the player's score is recorded the right way round`() {
        val season = Season.new(player)
        val rng = Random(1)
        while (season.playerFixture?.let { season.playerSide(it) } != Side.AWAY) season.playRound(rng)
        val fixture = season.playerFixture!!
        season.completeRound(0, 3, rng)
        val result = season.lastRoundResults.first { it.fixture == fixture }
        assertEquals(0, result.homeGoals)
        assertEquals(3, result.awayGoals)
    }

    @Test
    fun `player score is required exactly when they have a match`() {
        val season = Season.new(player)
        val rng = Random(2)
        if (season.playerFixture != null) {
            assertFailsWith<IllegalArgumentException> { season.completeRound(null, null, rng) }
        } else {
            assertFailsWith<IllegalArgumentException> { season.completeRound(1, 0, rng) }
        }
    }

    @Test
    fun `a saved season loads back identically`() {
        val season = Season.new(DEFAULT_TEAMS[7])
        val rng = Random(3)
        repeat(9) { season.playRound(rng) }
        val loaded = Season.decode(season.encode())
        assertEquals(season.playerTeam, loaded.playerTeam)
        assertEquals(season.teams, loaded.teams)
        assertEquals(season.results, loaded.results)
        assertEquals(season.roundsPlayed, loaded.roundsPlayed)
        assertEquals(season.table(), loaded.table())
    }

    @Test
    fun `rejects text that isn't a save`() {
        assertFailsWith<IllegalArgumentException> { Season.decode("hello") }
    }
}
