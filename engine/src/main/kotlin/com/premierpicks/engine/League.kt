package com.premierpicks.engine

/** The 1992-93 Premier League had 22 clubs, as did the original game. */
const val LEAGUE_SIZE = 22

/** Points awarded per result; the original let you change these before a season. */
data class PointsRules(val win: Int = 3, val draw: Int = 1, val loss: Int = 0)

data class Fixture(val round: Int, val home: String, val away: String)

data class TableRow(
    val team: String,
    val played: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val points: Int = 0,
) {
    val goalDifference: Int get() = goalsFor - goalsAgainst

    fun record(scored: Int, conceded: Int, rules: PointsRules): TableRow = copy(
        played = played + 1,
        won = won + if (scored > conceded) 1 else 0,
        drawn = drawn + if (scored == conceded) 1 else 0,
        lost = lost + if (scored < conceded) 1 else 0,
        goalsFor = goalsFor + scored,
        goalsAgainst = goalsAgainst + conceded,
        points = points + when {
            scored > conceded -> rules.win
            scored == conceded -> rules.draw
            else -> rules.loss
        },
    )
}

object FixtureGenerator {
    /**
     * Double round robin by the circle method: 22 teams give 42 rounds, every team at home
     * and away to each other. With an odd number of teams one team rests each round.
     * The second half of the season mirrors the first.
     */
    fun doubleRoundRobin(teams: List<String>): List<List<Fixture>> {
        require(teams.size >= 2) { "Need at least two teams" }
        require(teams.toSet().size == teams.size) { "Team names must be unique" }
        val slots: MutableList<String?> = teams.toMutableList()
        if (slots.size % 2 == 1) slots += null
        val n = slots.size
        val rounds = n - 1

        val firstLeg = (0 until rounds).map { round ->
            val pairs = (0 until n / 2).mapNotNull { i ->
                val a = slots[i]
                val b = slots[n - 1 - i]
                if (a == null || b == null) return@mapNotNull null
                // Alternate the fixed team's venue, and flip the rest by position, to balance home/away.
                val swap = if (i == 0) round % 2 == 1 else i % 2 == 1
                if (swap) Fixture(round + 1, b, a) else Fixture(round + 1, a, b)
            }
            // Rotate every slot except the first.
            slots.add(1, slots.removeAt(n - 1))
            pairs
        }
        val secondLeg = firstLeg.mapIndexed { r, fixtures ->
            fixtures.map { Fixture(rounds + r + 1, it.away, it.home) }
        }
        return firstLeg + secondLeg
    }
}

class LeagueTable(teams: List<String>, private val rules: PointsRules = PointsRules()) {
    private val rows = teams.associateWith { TableRow(it) }.toMutableMap()

    fun record(fixture: Fixture, homeGoals: Int, awayGoals: Int) {
        rows[fixture.home] = rows.getValue(fixture.home).record(homeGoals, awayGoals, rules)
        rows[fixture.away] = rows.getValue(fixture.away).record(awayGoals, homeGoals, rules)
    }

    /** Ordered by points, then goal difference, then goals scored, then name. */
    fun standings(): List<TableRow> = rows.values.sortedWith(
        compareByDescending<TableRow> { it.points }
            .thenByDescending { it.goalDifference }
            .thenByDescending { it.goalsFor }
            .thenBy { it.team },
    )
}

/**
 * Default club names; fictional so the shipped game avoids real club trademarks. Like the
 * original, players can rename them.
 */
val DEFAULT_TEAMS = listOf(
    "Ashford Rovers", "Bramley City", "Castleton Albion", "Dunmore Athletic", "Eastbridge United",
    "Fairhaven Town", "Glenmouth Wanderers", "Harrowgate Forest", "Ironbridge County", "Kingsmere Villa",
    "Lowther Park", "Marshfield Rangers", "Northcote Orient", "Oakhurst Hotspur", "Portwell Argyle",
    "Queensgate Wednesday", "Redcliffe Palace", "Southmoor Borough", "Thornbury Dynamo", "Westford Athletic",
    "Yarrow Vale", "Zetland Rovers",
)
