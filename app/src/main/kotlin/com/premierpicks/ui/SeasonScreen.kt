package com.premierpicks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.premierpicks.engine.DEFAULT_TEAMS
import com.premierpicks.engine.Season
import com.premierpicks.engine.TableRow

@Composable
fun TeamPickerScreen(onPick: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Pitch).safeDrawingPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Premier Picks", color = Gold, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text("Choose your team", color = Color.White, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(DEFAULT_TEAMS) { _, team ->
                Surface(
                    color = PitchLight,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onPick(team) },
                ) {
                    Text(team, color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
fun SeasonScreen(season: Season, version: Int, onPlay: () -> Unit, onNewSeason: () -> Unit) {
    var confirmNew by remember { mutableStateOf(false) }
    // [version] changes whenever the mutable season does, so these are recomputed each round.
    val table = remember(version) { season.table() }
    val position = remember(version) { season.playerPosition() }

    Column(
        Modifier.fillMaxSize().background(Pitch).safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(season.playerTeam, color = Gold, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text(
            if (season.isFinished) "Season complete" else "Round ${season.roundsPlayed + 1} of ${season.totalRounds} · ${ordinal(position)} place",
            color = Color.White,
        )

        NextUp(season, position, onPlay, onNewSeason = { confirmNew = true })
        LastResult(season)
        LeagueTableView(table, season.playerTeam, Modifier.weight(1f))

        if (!season.isFinished) {
            TextButton(onClick = { confirmNew = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Start a new season", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }

    if (confirmNew) {
        AlertDialog(
            onDismissRequest = { confirmNew = false },
            title = { Text("Start a new season?") },
            text = { Text(if (season.isFinished) "You'll choose a team for next season." else "This season's progress will be lost.") },
            confirmButton = {
                TextButton(onClick = { confirmNew = false; onNewSeason() }) { Text("New season") }
            },
            dismissButton = {
                TextButton(onClick = { confirmNew = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun NextUp(season: Season, position: Int, onPlay: () -> Unit, onNewSeason: () -> Unit) {
    Surface(color = PitchLight, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val fixture = season.playerFixture
            when {
                season.isFinished -> {
                    Text("You finished ${ordinal(position)}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    GoldButton("New season", onNewSeason)
                }
                fixture == null -> {
                    Text("No match this round", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Your team rests while the others play", color = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.height(12.dp))
                    GoldButton("Play the round", onPlay)
                }
                else -> {
                    Text(if (fixture.home == season.playerTeam) "Next: at home" else "Next: away", color = Color.White.copy(alpha = 0.7f))
                    Text(
                        "${fixture.home}  v  ${fixture.away}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    GoldButton("Kick off", onPlay)
                }
            }
        }
    }
}

@Composable
private fun LastResult(season: Season) {
    if (season.roundsPlayed == 0) return
    val mine = season.lastRoundResults.firstOrNull { season.playerTeam in setOf(it.fixture.home, it.fixture.away) }
    val text = if (mine == null) {
        "Last round: your team rested"
    } else {
        "Last result: ${mine.fixture.home} ${mine.homeGoals} – ${mine.awayGoals} ${mine.fixture.away}"
    }
    Text(text, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
}

@Composable
private fun LeagueTableView(rows: List<TableRow>, playerTeam: String, modifier: Modifier) {
    Surface(color = PitchLight, shape = RoundedCornerShape(16.dp), modifier = modifier.fillMaxWidth()) {
        LazyColumn(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            item { TableLine("", "Team", "P", "W", "D", "L", "GD", "Pts", header = true, highlight = false) }
            itemsIndexed(rows) { i, row ->
                TableLine(
                    "${i + 1}",
                    row.team,
                    "${row.played}",
                    "${row.won}",
                    "${row.drawn}",
                    "${row.lost}",
                    "${row.goalDifference}",
                    "${row.points}",
                    header = false,
                    highlight = row.team == playerTeam,
                )
            }
        }
    }
}

@Composable
private fun TableLine(
    pos: String,
    team: String,
    played: String,
    won: String,
    drawn: String,
    lost: String,
    gd: String,
    points: String,
    header: Boolean,
    highlight: Boolean,
) {
    val color = when {
        highlight -> Gold
        header -> Color.White.copy(alpha = 0.6f)
        else -> Color.White
    }
    val weight = if (header || highlight) FontWeight.Bold else FontWeight.Normal
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(pos, color = color, fontWeight = weight, fontSize = 13.sp, modifier = Modifier.width(24.dp))
        Text(team, color = color, fontWeight = weight, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        for (value in listOf(played, won, drawn, lost, gd)) {
            Text(value, color = color, fontWeight = weight, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.width(28.dp))
        }
        Text(points, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.width(34.dp))
    }
}

@Composable
private fun GoldButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Gold),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

private fun ordinal(n: Int): String {
    val suffix = if (n % 100 in 11..13) "th" else when (n % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    return "$n$suffix"
}
