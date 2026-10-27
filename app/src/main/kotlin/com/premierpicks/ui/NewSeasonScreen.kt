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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.premierpicks.engine.PointsRules
import com.premierpicks.engine.SeasonSettings

/**
 * Before a season, like the original: choose your club, rename any club, and set the
 * points for a win, draw and defeat, the number of games, and whether card help is on.
 */
@Composable
fun NewSeasonScreen(
    teamNames: List<String>,
    onRename: (index: Int, name: String) -> Boolean,
    onResetNames: () -> Unit,
    onStart: (team: String, settings: SeasonSettings) -> Unit,
) {
    val maxGames = 2 * (teamNames.size - 1)
    var games by remember { mutableIntStateOf(maxGames) }
    var win by remember { mutableIntStateOf(3) }
    var draw by remember { mutableIntStateOf(1) }
    var defeat by remember { mutableIntStateOf(0) }
    var cardHelp by remember { mutableStateOf(true) }
    var chosen by remember { mutableStateOf<Int?>(null) }
    var renaming by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize().background(Pitch).safeDrawingPadding().padding(16.dp)) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Premier Picks", color = Gold, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                }
            }
            item {
                Surface(color = PitchLight, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Stepper("Games in a season", games, 1..maxGames) { games = it }
                        Stepper("Points for a win", win, 0..9) { win = it }
                        Stepper("Points for a draw", draw, 0..9) { draw = it }
                        Stepper("Points for a defeat", defeat, 0..9) { defeat = it }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Card help", color = Color.White, modifier = Modifier.weight(1f))
                            Switch(checked = cardHelp, onCheckedChange = { cardHelp = it })
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Choose your team", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onResetNames) { Text("Reset names", color = Color.White.copy(alpha = 0.7f)) }
                }
            }
            itemsIndexed(teamNames) { i, team ->
                val selected = chosen == i
                Surface(
                    color = if (selected) Gold else PitchLight,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { chosen = i },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            team,
                            color = if (selected) Color(0xFF1B1B1B) else Color.White,
                            fontSize = 17.sp,
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                        TextButton(onClick = { renaming = i }) {
                            Text("Rename", color = if (selected) Color(0xFF1B1B1B) else Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                chosen?.let { onStart(teamNames[it], SeasonSettings(games, PointsRules(win, draw, defeat), cardHelp)) }
            },
            enabled = chosen != null,
            colors = ButtonDefaults.buttonColors(containerColor = Gold),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(
                chosen?.let { "Start the season with ${teamNames[it]}" } ?: "Choose a team to start",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }

    renaming?.let { index ->
        RenameDialog(
            current = teamNames[index],
            onDismiss = { renaming = null },
            onSave = { name -> onRename(index, name).also { ok -> if (ok) renaming = null } },
        )
    }
}

@Composable
private fun Stepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        StepButton("−", enabled = value > range.first) { onChange(value - 1) }
        Text("$value", color = Gold, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.width(40.dp))
        StepButton("+", enabled = value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (enabled) Color(0xFF7A1F3D) else Color(0xFF3A2A30),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.size(36.dp).clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun RenameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Boolean) {
    var text by remember { mutableStateOf(current) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename club") },
        text = {
            Column {
                OutlinedTextField(value = text, onValueChange = { text = it.take(MAX_NAME); error = false }, singleLine = true)
                if (error) Text("That name is empty or already taken", color = Color(0xFFFF8A80), fontSize = 13.sp)
            }
        },
        confirmButton = { TextButton(onClick = { error = !onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private const val MAX_NAME = 24
