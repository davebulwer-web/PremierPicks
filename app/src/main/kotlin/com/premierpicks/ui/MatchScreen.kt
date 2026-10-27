package com.premierpicks.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.premierpicks.engine.Card
import com.premierpicks.engine.MAX_PICKS
import com.premierpicks.engine.Phase
import com.premierpicks.engine.SideState

@Composable
fun MatchScreen(vm: MatchViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    MatchContent(
        ui = ui,
        onChooseBudget = vm::chooseBudget,
        onFlip = vm::flip,
        onSecondHalf = vm::startSecondHalf,
        onNewMatch = vm::newMatch,
    )
}

@Composable
fun MatchContent(
    ui: MatchUiState,
    onChooseBudget: (Int) -> Unit,
    onFlip: (Int) -> Unit,
    onSecondHalf: () -> Unit,
    onNewMatch: () -> Unit,
) {
    val state = ui.match
    val me = state.side(ui.humanSide)
    val cpu = state.side(ui.humanSide.opponent)

    Box(Modifier.fillMaxSize().background(Pitch)) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Scoreboard(half = state.half, me = me, cpu = cpu)
            TurnStatus(ui)
            CardGrid(
                cards = state.board,
                highlighted = state.highlightedGoals,
                canFlip = ui.isHumanTurn && state.phase == Phase.FLIPPING,
                onFlip = onFlip,
                modifier = Modifier.weight(1f),
            )
            PickSelector(
                enabled = ui.isHumanTurn && state.phase == Phase.AWAITING_BUDGET,
                maxPicks = me.maxPicks,
                onChoose = onChooseBudget,
            )
        }

        ui.banner?.let { key(it.id) { BannerOverlay(it) } }

        when (state.phase) {
            Phase.HALF_TIME -> BreakOverlay(
                title = "Half time",
                score = "You ${me.goals} – ${cpu.goals} CPU",
                action = "Start second half",
                onAction = onSecondHalf,
            )
            Phase.FULL_TIME -> BreakOverlay(
                title = when {
                    me.goals > cpu.goals -> "Full time: you win!"
                    me.goals < cpu.goals -> "Full time: you lose"
                    else -> "Full time: a draw"
                },
                score = "You ${me.goals} – ${cpu.goals} CPU",
                action = "Play again",
                onAction = onNewMatch,
            )
            else -> Unit
        }
    }
}

@Composable
private fun Scoreboard(half: Int, me: SideState, cpu: SideState) {
    Surface(color = PitchLight, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (half == 1) "1st half" else "2nd half", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamScore("YOU", me, Modifier.weight(1f))
                Text("–", color = Color.White, fontSize = 32.sp)
                TeamScore("CPU", cpu, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TeamScore(name: String, side: SideState, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, color = Color.White, fontWeight = FontWeight.Bold)
        Text("${side.goals}", color = Gold, fontSize = 40.sp, fontWeight = FontWeight.Black)
        val discipline = "🟨".repeat(side.yellows) + "🟥".repeat(side.reds)
        val limit = if (side.maxPicks < MAX_PICKS) "  max ${side.maxPicks}" else ""
        Text(discipline + limit, color = Color.White, fontSize = 12.sp, modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun TurnStatus(ui: MatchUiState) {
    val turn = ui.match.turn
    val bonus = if (turn.bonus > 0) " (+${turn.bonus} from a foul)" else ""
    val text = when {
        ui.match.phase != Phase.AWAITING_BUDGET && ui.match.phase != Phase.FLIPPING -> ""
        ui.isHumanTurn && ui.match.phase == Phase.AWAITING_BUDGET -> "Your turn: how many cards will you pick?$bonus"
        ui.isHumanTurn -> "Picks remaining: ${turn.remaining} / ${turn.total}"
        ui.match.phase == Phase.AWAITING_BUDGET -> "CPU is thinking…"
        else -> "CPU picks remaining: ${turn.remaining} / ${turn.total}"
    }
    Text(
        text,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CardGrid(
    cards: List<Card>,
    highlighted: Set<Int>,
    canFlip: Boolean,
    onFlip: (Int) -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cards.chunked(COLUMNS).forEachIndexed { row, rowCards ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowCards.forEachIndexed { col, card ->
                    val index = row * COLUMNS + col
                    CardTile(
                        card = card,
                        highlighted = index in highlighted,
                        enabled = canFlip && !card.revealed,
                        onClick = { onFlip(index) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CardTile(card: Card, highlighted: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val rotation by animateFloatAsState(if (card.revealed) 180f else 0f, tween(400), label = "flip")
    val showingFace = rotation > 90f
    val face = faceOf(card.type)
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(shape)
            .background(if (showingFace) face.background else CardBack)
            .then(if (highlighted && !showingFace) Modifier.border(3.dp, Gold, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (showingFace) {
            // Counter-rotate so the face isn't mirrored.
            Column(Modifier.graphicsLayer { rotationY = 180f }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(face.emoji, fontSize = 26.sp)
                Text(face.label, color = face.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text("?", color = Color.White.copy(alpha = 0.8f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PickSelector(enabled: Boolean, maxPicks: Int, onChoose: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (n in 1..MAX_PICKS) {
            Button(
                onClick = { onChoose(n) },
                enabled = enabled && n <= maxPicks,
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Text("$n", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BannerOverlay(banner: Banner) {
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, tween(250)) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            color = Color.Black.copy(alpha = 0.8f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.scale(scale.value),
        ) {
            Column(Modifier.padding(horizontal = 32.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(banner.title, color = Gold, fontSize = 34.sp, fontWeight = FontWeight.Black)
                banner.detail?.let { Text(it, color = Color.White, fontSize = 16.sp) }
            }
        }
    }
}

@Composable
private fun BreakOverlay(title: String, score: String, action: String, onAction: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        Surface(color = PitchLight, shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(24.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(score, color = Gold, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = onAction) { Text(action, color = Color.White) }
            }
        }
    }
}

private const val COLUMNS = 3
