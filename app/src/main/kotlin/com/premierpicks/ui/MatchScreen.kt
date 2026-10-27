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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.premierpicks.engine.Card
import com.premierpicks.engine.MAX_PICKS
import com.premierpicks.engine.Phase
import com.premierpicks.engine.Side
import com.premierpicks.engine.SideState
import kotlinx.coroutines.delay

@Composable
fun MatchScreen(vm: MatchViewModel, onFinished: (homeGoals: Int, awayGoals: Int) -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    MatchContent(
        ui = ui,
        onChooseBudget = vm::chooseBudget,
        onFlip = vm::flip,
        onSecondHalf = vm::startSecondHalf,
        onFinished = { onFinished(ui.match.home.goals, ui.match.away.goals) },
    )
}

@Composable
fun MatchContent(
    ui: MatchUiState,
    onChooseBudget: (Int) -> Unit,
    onFlip: (Int) -> Unit,
    onSecondHalf: () -> Unit,
    onFinished: () -> Unit,
) {
    val state = ui.match
    val me = state.side(ui.humanSide)
    val cpu = state.side(ui.humanSide.opponent)
    val score = "${ui.homeName} ${state.home.goals} – ${state.away.goals} ${ui.awayName}"

    Box(Modifier.fillMaxSize().background(Pitch)) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Scoreboard(ui)
            TurnStatus(ui)
            // A new board gets fresh tiles, so cards don't animate from the old board's faces.
            key(state.half) {
                CardGrid(
                    cards = state.board,
                    half = state.half,
                    highlighted = state.highlightedGoals,
                    canFlip = ui.isHumanTurn && state.phase == Phase.FLIPPING,
                    onFlip = onFlip,
                    modifier = Modifier.weight(1f),
                )
            }
            PickSelector(
                enabled = ui.isHumanTurn && state.phase == Phase.AWAITING_BUDGET,
                maxPicks = me.maxPicks,
                onChoose = onChooseBudget,
            )
        }

        ui.banner?.let { key(it.id) { BannerOverlay(it) } }

        // Let the whistle card be seen before the break screen covers the board.
        var showBreak by remember(state.phase) { mutableStateOf(false) }
        LaunchedEffect(state.phase) {
            delay(BREAK_DELAY_MS)
            showBreak = true
        }
        if (showBreak) when (state.phase) {
            Phase.HALF_TIME -> BreakOverlay(
                title = "Half time",
                score = score,
                action = "Start second half",
                onAction = onSecondHalf,
            )
            Phase.FULL_TIME -> BreakOverlay(
                title = when {
                    me.goals > cpu.goals -> "Full time: you win!"
                    me.goals < cpu.goals -> "Full time: you lose"
                    else -> "Full time: a draw"
                },
                score = score,
                action = "Continue",
                onAction = onFinished,
            )
            else -> Unit
        }
    }
}

@Composable
private fun Scoreboard(ui: MatchUiState) {
    Surface(color = PitchLight, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (ui.match.half == 1) "1st half" else "2nd half", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamScore(ui.homeName, ui.humanSide == Side.HOME, ui.match.home, Modifier.weight(1f))
                Text("–", color = Color.White, fontSize = 32.sp)
                TeamScore(ui.awayName, ui.humanSide == Side.AWAY, ui.match.away, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TeamScore(name: String, isYou: Boolean, side: SideState, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, color = if (isYou) Gold else Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (isYou) "YOU" else "CPU", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        Text("${side.goals}", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
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
        ui.match.phase == Phase.AWAITING_BUDGET -> "${ui.name(turn.side)} are thinking…"
        else -> "${ui.name(turn.side)} picks remaining: ${turn.remaining} / ${turn.total}"
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
    half: Int,
    highlighted: Set<Int>,
    canFlip: Boolean,
    onFlip: (Int) -> Unit,
    modifier: Modifier,
) {
    // Six across and three down, like the original, with portrait cards.
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
        cards.chunked(COLUMNS).forEachIndexed { row, rowCards ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowCards.forEachIndexed { col, card ->
                    val index = row * COLUMNS + col
                    CardTile(
                        card = card,
                        half = half,
                        highlighted = index in highlighted,
                        enabled = canFlip && !card.revealed,
                        onClick = { onFlip(index) },
                        modifier = Modifier.weight(1f).aspectRatio(CARD_ASPECT),
                    )
                }
            }
        }
    }
}

@Composable
private fun CardTile(card: Card, half: Int, highlighted: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val rotation by animateFloatAsState(if (card.revealed) 180f else 0f, tween(400), label = "flip")
    val showingFace = rotation > 90f
    val shape = RoundedCornerShape(4.dp)

    Box(
        modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(shape)
            .border(if (highlighted && !showingFace) 3.dp else 2.dp, if (highlighted && !showingFace) Gold else Color(0xFF1A1A1A), shape)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        if (showingFace) {
            // Counter-rotate so the face isn't mirrored.
            CardFaceArt(card.type, half, Modifier.fillMaxSize().graphicsLayer { rotationY = 180f })
        } else {
            CardBackArt(Modifier.fillMaxSize())
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
    // A plain Box rather than a Surface: a Surface swallows taps, which made the cards
    // underneath the banner ignore the player while it was showing.
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .scale(scale.value)
                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(banner.title, color = Gold, fontSize = 34.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            banner.detail?.let { Text(it, color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center) }
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

private const val COLUMNS = 6
private const val CARD_ASPECT = 0.72f
private const val BREAK_DELAY_MS = 1_200L
