package com.premierpicks.engine

import kotlin.random.Random

data class MatchResult(val fixture: Fixture, val homeGoals: Int, val awayGoals: Int)

/**
 * One league season from the player's point of view. Rounds are completed whole: the
 * player's own match (if they have one that round) plus a simulation of every other fixture.
 */
data class SeasonSettings(
    /** Rounds in the season, up to a full home-and-away double round robin (the default, null). */
    val rounds: Int? = null,
    val points: PointsRules = PointsRules(),
    /** Whether each half starts with the card-help preview buzzer. */
    val cardHelp: Boolean = true,
)

class Season(
    val teams: List<String>,
    val playerTeam: String,
    val settings: SeasonSettings = SeasonSettings(),
    results: List<MatchResult> = emptyList(),
) {
    init {
        require(playerTeam in teams) { "$playerTeam is not in the league" }
    }

    val fixtures: List<List<Fixture>> = FixtureGenerator.doubleRoundRobin(teams).let { all ->
        val rounds = settings.rounds ?: all.size
        require(rounds in 1..all.size) { "Bad season length $rounds" }
        all.take(rounds)
    }

    private val _results = results.toMutableList()
    val results: List<MatchResult> get() = _results

    /** Rounds played so far; rounds are always recorded in full. */
    val roundsPlayed: Int get() = _results.maxOfOrNull { it.fixture.round } ?: 0

    val totalRounds: Int get() = fixtures.size

    val isFinished: Boolean get() = roundsPlayed == totalRounds

    val currentRound: List<Fixture>? get() = fixtures.getOrNull(roundsPlayed)

    /** The player's fixture this round, or null if they rest this round (or the season is over). */
    val playerFixture: Fixture? get() = currentRound?.firstOrNull { it.home == playerTeam || it.away == playerTeam }

    fun playerSide(fixture: Fixture): Side = if (fixture.home == playerTeam) Side.HOME else Side.AWAY

    /** Results of the most recently completed round. */
    val lastRoundResults: List<MatchResult> get() = _results.filter { it.fixture.round == roundsPlayed }

    fun table(): List<TableRow> {
        val table = LeagueTable(teams, settings.points)
        _results.forEach { table.record(it.fixture, it.homeGoals, it.awayGoals) }
        return table.standings()
    }

    fun playerPosition(): Int = table().indexOfFirst { it.team == playerTeam } + 1

    /**
     * Records the player's score (required exactly when they have a fixture this round)
     * and simulates the rest of the round.
     */
    fun completeRound(playerHomeGoals: Int?, playerAwayGoals: Int?, rng: Random): List<MatchResult> {
        val round = checkNotNull(currentRound) { "The season is over" }
        val mine = playerFixture
        require((mine != null) == (playerHomeGoals != null && playerAwayGoals != null)) {
            if (mine == null) "The player rests this round" else "The player's score is required"
        }
        val played = round.map { fixture ->
            if (fixture == mine) {
                MatchResult(fixture, playerHomeGoals!!, playerAwayGoals!!)
            } else {
                val sim = MatchSimulator.simulate(rng, settings.cardHelp)
                MatchResult(fixture, sim.home.goals, sim.away.goals)
            }
        }
        _results += played
        return played
    }

    /** A plain-text save format: easy to diff and stable across app versions. */
    fun encode(): String = buildString {
        appendLine(FORMAT)
        appendLine("player\t$playerTeam")
        appendLine("teams\t${teams.joinToString("\t")}")
        appendLine("rounds\t${fixtures.size}")
        appendLine("points\t${settings.points.win}\t${settings.points.draw}\t${settings.points.loss}")
        appendLine("cardhelp\t${settings.cardHelp}")
        _results.forEach {
            appendLine("result\t${it.fixture.round}\t${it.fixture.home}\t${it.fixture.away}\t${it.homeGoals}\t${it.awayGoals}")
        }
    }

    companion object {
        private const val FORMAT = "premier-picks-season-v1"

        fun new(playerTeam: String, teams: List<String> = DEFAULT_TEAMS, settings: SeasonSettings = SeasonSettings()) =
            Season(teams, playerTeam, settings)

        fun decode(text: String): Season {
            val lines = text.lines().filter { it.isNotBlank() }
            require(lines.firstOrNull() == FORMAT) { "Not a season save" }
            var player: String? = null
            var teams: List<String>? = null
            var rounds: Int? = null
            var points = PointsRules()
            var cardHelp = true
            val results = mutableListOf<MatchResult>()
            for (line in lines.drop(1)) {
                val parts = line.split("\t")
                when (parts[0]) {
                    "player" -> player = parts[1]
                    "teams" -> teams = parts.drop(1)
                    "rounds" -> rounds = parts[1].toInt()
                    "points" -> points = PointsRules(parts[1].toInt(), parts[2].toInt(), parts[3].toInt())
                    "cardhelp" -> cardHelp = parts[1].toBoolean()
                    "result" -> results += MatchResult(
                        Fixture(parts[1].toInt(), parts[2], parts[3]),
                        parts[4].toInt(),
                        parts[5].toInt(),
                    )
                }
            }
            val league = requireNotNull(teams) { "Save has no teams" }
            // Saves from before season settings existed played a full double round robin.
            val settings = SeasonSettings(rounds ?: FixtureGenerator.doubleRoundRobin(league).size, points, cardHelp)
            return Season(league, requireNotNull(player) { "Save has no player team" }, settings, results)
        }
    }
}
