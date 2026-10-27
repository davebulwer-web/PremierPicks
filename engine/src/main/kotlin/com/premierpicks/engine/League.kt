package com.premierpicks.engine

const val LEAGUE_SIZE = 21

data class Fixture(val round: Int, val home: String, val away: String)

data class TableRow(
    val team: String,
    val played: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
) {
    val points: Int get() = won * 3 + drawn
    val goalDifference: Int get() = goalsFor - goalsAgainst

    fun record(scored: Int, conceded: Int): TableRow = copy(
        played = played + 1,
        won = won + if (scored > conceded) 1 else 0,
        drawn = drawn + if (scored == conceded) 1 else 0,
        lost = lost + if (scored < conceded) 1 else 0,
        goalsFor = goalsFor + scored,
        goalsAgainst = goalsAgainst + conceded,
    )
}

object FixtureGenerator {
    /**
     * Double round robin by the circle method. With an odd number of teams one team rests
     * each round, so 21 teams give 42 rounds in which every team plays 40 matches,
     * 20 at home and 20 away. The second half of the season mirrors the first.
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

class LeagueTable(teams: List<String>) {
    private val rows = teams.associateWith { TableRow(it) }.toMutableMap()

    fun record(fixture: Fixture, homeGoals: Int, awayGoals: Int) {
        rows[fixture.home] = rows.getValue(fixture.home).record(homeGoals, awayGoals)
        rows[fixture.away] = rows.getValue(fixture.away).record(awayGoals, homeGoals)
    }

    /** Ordered by points, then goal difference, then goals scored, then name. */
    fun standings(): List<TableRow> = rows.values.sortedWith(
        compareByDescending<TableRow> { it.points }
            .thenByDescending { it.goalDifference }
            .thenByDescending { it.goalsFor }
            .thenBy { it.team },
    )
}

/** Placeholder club names; fictional so the shipped game avoids real club trademarks. */
val DEFAULT_TEAMS = listOf(
    "Ashford Rovers", "Bramley City", "Castleton Albion", "Dunmore Athletic", "Eastbridge United",
    "Fairhaven Town", "Glenmouth Wanderers", "Harrowgate Forest", "Ironbridge County", "Kingsmere Villa",
    "Lowther Park", "Marshfield Rangers", "Northcote Orient", "Oakhurst Hotspur", "Portwell Argyle",
    "Queensgate Wednesday", "Redcliffe Palace", "Southmoor Borough", "Thornbury Dynamo", "Westford Athletic",
    "Yarrow Vale",
)
