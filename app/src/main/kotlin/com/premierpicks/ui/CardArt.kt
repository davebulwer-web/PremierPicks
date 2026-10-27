package com.premierpicks.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.premierpicks.engine.CardType

// Card art drawn in the spirit of the 1993 original: bold flat colours, a striped gold
// card back, and a small scene on each face. All original drawings, no copied sprites.

private val Sky = Color(0xFF1E3FC4)
private val SkyDark = Color(0xFF13288A)
private val Grass = Color(0xFF2DB84A)
private val GrassLight = Color(0xFF5BD86E)
private val NetWhite = Color(0xFFE8EEFF)
private val Glove = Color(0xFFF2A51A)
private val Sleeve = Color(0xFF2E9D3E)
private val Skin = Color(0xFFF1C27D)
private val GoldDark = Color(0xFF7A5A0C)
private val GoldLight = Color(0xFFD9A521)
private val Maroon = Color(0xFF7A1F3D)
private val Ink = Color(0xFF111111)

@Composable
fun CardBackArt(modifier: Modifier = Modifier) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) { drawBack() }
        Text(
            "P\nP",
            color = Color(0xFF1F3FD0),
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            lineHeight = 15.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 6.dp),
        )
    }
}

@Composable
fun CardFaceArt(type: CardType, half: Int, modifier: Modifier = Modifier) {
    if (type == CardType.WHISTLE) {
        WhistleArt(fullTime = half == 2, modifier = modifier)
        return
    }
    Canvas(modifier) {
        when (type) {
            CardType.GOAL -> drawGoal()
            CardType.MISS -> drawSave()
            CardType.PENALTY -> drawPenalty()
            CardType.FOUL -> drawInjury()
            CardType.YELLOW -> drawBooking(Color(0xFFFFE135))
            CardType.RED -> drawBooking(Color(0xFFE02020))
            CardType.SUBSTITUTE -> drawSubstitute()
            CardType.HINT -> drawHint()
            CardType.WHISTLE -> Unit
        }
    }
}

@Composable
private fun WhistleArt(fullTime: Boolean, modifier: Modifier) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Maroon)
            drawRect(Sky, Offset(0f, size.height * 0.24f), Size(size.width, size.height * 0.52f))
            if (fullTime) drawWhistle() else drawOrange()
        }
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            WhistleLabel(if (fullTime) "FULL" else "HALF")
            Box(Modifier.weight(1f))
            WhistleLabel("TIME")
        }
    }
}

@Composable
private fun WhistleLabel(text: String) {
    Text(text, color = GoldLight, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
}

// --- Scenes -------------------------------------------------------------------------------

private fun DrawScope.drawBack() {
    val w = size.width
    val h = size.height
    drawRect(GoldDark)
    val stripes = 9
    for (i in 0 until stripes) {
        drawRect(GoldLight, Offset(i * w / stripes, 0f), Size(w / stripes / 2, h))
    }
    // Lion silhouette: mane, body and a raised paw over a ball.
    drawOval(Ink, Offset(w * 0.42f, h * 0.16f), Size(w * 0.46f, h * 0.34f))
    drawRoundRect(Ink, Offset(w * 0.40f, h * 0.42f), Size(w * 0.5f, h * 0.44f), CornerRadius(w * 0.1f))
    drawLine(Ink, Offset(w * 0.5f, h * 0.55f), Offset(w * 0.22f, h * 0.7f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
    // Crown.
    val crown = Path().apply {
        moveTo(w * 0.5f, h * 0.15f)
        lineTo(w * 0.52f, h * 0.05f)
        lineTo(w * 0.6f, h * 0.11f)
        lineTo(w * 0.66f, h * 0.03f)
        lineTo(w * 0.72f, h * 0.11f)
        lineTo(w * 0.8f, h * 0.05f)
        lineTo(w * 0.82f, h * 0.15f)
        close()
    }
    drawPath(crown, Ink)
    drawBall(Offset(w * 0.2f, h * 0.84f), w * 0.11f)
}

private fun DrawScope.drawGoal() {
    val w = size.width
    val h = size.height
    drawRect(Sky)
    drawNet(Offset.Zero, Size(w, h * 0.55f), cells = 6)
    drawPitch(h * 0.55f)
    drawBall(Offset(w * 0.5f, h * 0.55f), w * 0.34f)
}

private fun DrawScope.drawSave() {
    val w = size.width
    val h = size.height
    drawRect(Sky)
    drawRect(SkyDark, Offset(0f, h * 0.7f), Size(w, h * 0.3f))
    // Post and net on the left.
    drawNet(Offset.Zero, Size(w * 0.32f, h * 0.75f), cells = 3)
    drawRect(NetWhite, Offset(w * 0.3f, 0f), Size(w * 0.05f, h * 0.75f))
    drawBall(Offset(w * 0.72f, h * 0.2f), w * 0.15f)
    // Keeper's arm reaching up, glove punching the ball clear.
    drawLine(Sleeve, Offset(w * 0.25f, h * 1.05f), Offset(w * 0.55f, h * 0.42f), strokeWidth = w * 0.2f, cap = StrokeCap.Round)
    drawCircle(Glove, w * 0.12f, Offset(w * 0.58f, h * 0.36f))
}

private fun DrawScope.drawPenalty() {
    val w = size.width
    val h = size.height
    drawRect(Sky)
    drawPitch(h * 0.48f)
    val frame = Offset(w * 0.1f, h * 0.12f)
    val frameSize = Size(w * 0.8f, h * 0.36f)
    drawRect(NetWhite.copy(alpha = 0.35f), frame, frameSize)
    for (x in 1..7) for (y in 1..4) {
        drawCircle(NetWhite, w * 0.012f, Offset(frame.x + frameSize.width * x / 8, frame.y + frameSize.height * y / 5))
    }
    drawRect(NetWhite, frame, frameSize, style = Stroke(w * 0.05f))
    drawBall(Offset(w * 0.5f, h * 0.76f), w * 0.14f)
}

private fun DrawScope.drawInjury() {
    val w = size.width
    val h = size.height
    drawPitch(0f)
    drawArc(Color.White, 200f, 140f, false, Offset(w * 0.1f, h * 0.62f), Size(w * 0.8f, h * 0.5f), style = Stroke(w * 0.03f))
    // A player flat out on the grass: boots, socks, shorts, shirt, head.
    drawRoundRect(Color(0xFFDADADA), Offset(w * 0.08f, h * 0.48f), Size(w * 0.2f, h * 0.1f), CornerRadius(w * 0.05f))
    drawRect(Color.White, Offset(w * 0.26f, h * 0.47f), Size(w * 0.16f, h * 0.12f))
    drawRect(Color(0xFFE02020), Offset(w * 0.4f, h * 0.45f), Size(w * 0.3f, h * 0.15f))
    drawCircle(Skin, w * 0.1f, Offset(w * 0.8f, h * 0.5f))
    // Stars circling the head.
    for (i in 0..2) drawCircle(Color(0xFFFFE135), w * 0.035f, Offset(w * (0.62f + 0.13f * i), h * (0.3f - 0.05f * (i % 2))))
}

private fun DrawScope.drawBooking(cardColor: Color) {
    val w = size.width
    val h = size.height
    drawPitch(0f)
    drawLine(Color.White, Offset(w * 0.6f, 0f), Offset(w * 0.6f, h), strokeWidth = w * 0.03f)
    drawBall(Offset(w * 0.68f, h * 0.72f), w * 0.22f)
    // Referee's black sleeve and hand holding the card up.
    drawLine(Ink, Offset(w * 0.18f, h * 1.05f), Offset(w * 0.3f, h * 0.38f), strokeWidth = w * 0.16f, cap = StrokeCap.Round)
    drawCircle(Skin, w * 0.09f, Offset(w * 0.3f, h * 0.32f))
    rotate(-15f, Offset(w * 0.35f, h * 0.18f)) {
        drawRect(cardColor, Offset(w * 0.22f, h * 0.06f), Size(w * 0.26f, h * 0.24f))
    }
}

private fun DrawScope.drawSubstitute() {
    val w = size.width
    val h = size.height
    drawRect(Sky)
    drawRect(SkyDark, Offset(0f, h * 0.75f), Size(w, h * 0.25f))
    val shirt = Path().apply {
        moveTo(w * 0.32f, h * 0.1f)
        lineTo(w * 0.68f, h * 0.1f)
        lineTo(w * 0.95f, h * 0.3f)
        lineTo(w * 0.85f, h * 0.45f)
        lineTo(w * 0.75f, h * 0.38f)
        lineTo(w * 0.75f, h * 0.88f)
        lineTo(w * 0.25f, h * 0.88f)
        lineTo(w * 0.25f, h * 0.38f)
        lineTo(w * 0.15f, h * 0.45f)
        lineTo(w * 0.05f, h * 0.3f)
        close()
    }
    drawPath(shirt, Color.White)
    drawArrow(Offset(w * 0.6f, h * 0.38f), Offset(w * 0.36f, h * 0.56f), Color(0xFF1F3FD0))
    drawArrow(Offset(w * 0.4f, h * 0.6f), Offset(w * 0.64f, h * 0.78f), Color(0xFFE02020))
}

private fun DrawScope.drawHint() {
    val w = size.width
    val h = size.height
    drawRect(Color(0xFF0E7C86))
    drawCircle(Color.White, w * 0.24f, Offset(w * 0.44f, h * 0.4f), style = Stroke(w * 0.08f))
    drawCircle(Color.White.copy(alpha = 0.25f), w * 0.2f, Offset(w * 0.44f, h * 0.4f))
    drawLine(Color.White, Offset(w * 0.6f, h * 0.53f), Offset(w * 0.82f, h * 0.75f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
    drawBall(Offset(w * 0.44f, h * 0.4f), w * 0.1f)
}

private fun DrawScope.drawOrange() {
    val w = size.width
    val h = size.height
    val c = Offset(w * 0.5f, h * 0.5f)
    val r = w * 0.32f
    drawCircle(Color(0xFFF7A21B), r, c)
    drawCircle(Color(0xFFFFD27A), r * 0.82f, c)
    for (i in 0 until 8) {
        val a = Math.toRadians(i * 45.0)
        drawLine(Color(0xFFF7A21B), c, Offset(c.x + (r * 0.82f * Math.cos(a)).toFloat(), c.y + (r * 0.82f * Math.sin(a)).toFloat()), strokeWidth = w * 0.025f)
    }
}

private fun DrawScope.drawWhistle() {
    val w = size.width
    val h = size.height
    drawCircle(GoldLight, w * 0.24f, Offset(w * 0.42f, h * 0.55f))
    drawCircle(GoldDark, w * 0.1f, Offset(w * 0.42f, h * 0.55f))
    drawRect(GoldLight, Offset(w * 0.42f, h * 0.38f), Size(w * 0.42f, h * 0.12f))
    drawCircle(Color.White.copy(alpha = 0.5f), w * 0.04f, Offset(w * 0.34f, h * 0.47f))
}

// --- Shared pieces ------------------------------------------------------------------------

private fun DrawScope.drawPitch(top: Float) {
    val bands = 4
    val bandHeight = (size.height - top) / bands
    for (i in 0 until bands) {
        drawRect(if (i % 2 == 0) Grass else GrassLight, Offset(0f, top + i * bandHeight), Size(size.width, bandHeight))
    }
}

private fun DrawScope.drawNet(topLeft: Offset, netSize: Size, cells: Int) {
    val stroke = size.width * 0.025f
    for (i in 0..cells) {
        val x = topLeft.x + netSize.width * i / cells
        drawLine(NetWhite, Offset(x, topLeft.y), Offset(x, topLeft.y + netSize.height), strokeWidth = stroke)
    }
    val rows = (cells * netSize.height / netSize.width).toInt().coerceAtLeast(2)
    for (j in 0..rows) {
        val y = topLeft.y + netSize.height * j / rows
        drawLine(NetWhite, Offset(topLeft.x, y), Offset(topLeft.x + netSize.width, y), strokeWidth = stroke)
    }
}

private fun DrawScope.drawBall(center: Offset, radius: Float) {
    drawCircle(Color.White, radius, center)
    drawCircle(Ink, radius, center, style = Stroke(radius * 0.08f))
    val pentagon = Path()
    for (i in 0 until 5) {
        val a = Math.toRadians(-90.0 + i * 72.0)
        val p = Offset(center.x + (radius * 0.38f * Math.cos(a)).toFloat(), center.y + (radius * 0.38f * Math.sin(a)).toFloat())
        if (i == 0) pentagon.moveTo(p.x, p.y) else pentagon.lineTo(p.x, p.y)
        // Patches round the edge.
        val edge = Offset(center.x + (radius * 0.88f * Math.cos(a)).toFloat(), center.y + (radius * 0.88f * Math.sin(a)).toFloat())
        drawCircle(Ink, radius * 0.18f, edge)
    }
    pentagon.close()
    drawPath(pentagon, Ink)
}

private fun DrawScope.drawArrow(from: Offset, to: Offset, color: Color) {
    val w = size.width
    drawLine(color, from, to, strokeWidth = w * 0.09f)
    val angle = Math.atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble())
    val head = w * 0.14f
    val path = Path().apply {
        moveTo(to.x + (head * 0.6f * Math.cos(angle)).toFloat(), to.y + (head * 0.6f * Math.sin(angle)).toFloat())
        lineTo(to.x + (head * Math.cos(angle + 2.4)).toFloat(), to.y + (head * Math.sin(angle + 2.4)).toFloat())
        lineTo(to.x + (head * Math.cos(angle - 2.4)).toFloat(), to.y + (head * Math.sin(angle - 2.4)).toFloat())
        close()
    }
    drawPath(path, color)
}
