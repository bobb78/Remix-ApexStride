package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GPSPoint
import com.example.data.model.formatPace
import com.example.ui.theme.AcidYellow
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HyperCoral
import com.example.ui.theme.NeonLime
import com.example.ui.theme.SlateDark
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class MapColorMode {
    PACE_HEATMAP,
    ELEVATION_PROFILE
}

/**
 * Visualizer component using Jetpack Compose Canvas to render the running route
 * with smooth color-coded gradients based on real-time and historical pace data.
 */
@Composable
fun PaceHeatmapCanvas(
    points: List<GPSPoint>,
    modifier: Modifier = Modifier,
    isLive: Boolean = false,
    showGrid: Boolean = true,
    initialColorMode: MapColorMode = MapColorMode.PACE_HEATMAP
) {
    var is3DView by remember { mutableStateOf(false) }

    if (is3DView) {
        PaceHeatmap3DCanvas(
            points = points,
            modifier = modifier,
            isLive = isLive,
            showGrid = showGrid,
            initialIs3D = true
        )
        return
    }

    var colorMode by remember { mutableStateOf(initialColorMode) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .background(SlateDark, RoundedCornerShape(24.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.7f, 4.5f)
                        panOffset += pan
                    }
                }
                .pointerInput(points) {
                    detectTapGestures { tapOffset ->
                        if (points.size < 2) return@detectTapGestures
                        // Find closest projected point on canvas
                        // Handled via local coordinate projection
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f

            // 1. Draw Tactical Grid Background
            if (showGrid) {
                drawGrid(width, height)
            }

            if (points.isEmpty()) {
                // Empty state radial beacon
                drawCircle(
                    color = ElectricCyan.copy(alpha = 0.12f),
                    radius = width * 0.28f,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.5f)
                )
                drawCircle(
                    color = NeonLime.copy(alpha = 0.08f),
                    radius = width * 0.42f,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.5f)
                )
                return@Canvas
            }

            // 2. Coordinate Bounding Box
            var minLat = Double.MAX_VALUE
            var maxLat = -Double.MAX_VALUE
            var minLng = Double.MAX_VALUE
            var maxLng = -Double.MAX_VALUE
            var minSpeed = Float.MAX_VALUE
            var maxSpeed = Float.MIN_VALUE
            var minAlt = Double.MAX_VALUE
            var maxAlt = -Double.MAX_VALUE

            for (p in points) {
                minLat = min(minLat, p.latitude)
                maxLat = max(maxLat, p.latitude)
                minLng = min(minLng, p.longitude)
                maxLng = max(maxLng, p.longitude)
                minSpeed = min(minSpeed, p.speed)
                maxSpeed = max(maxSpeed, p.speed)
                minAlt = min(minAlt, p.altitude)
                maxAlt = max(maxAlt, p.altitude)
            }

            val latRange = max(maxLat - minLat, 0.0004)
            val lngRange = max(maxLng - minLng, 0.0004)
            val speedRange = max(maxSpeed - minSpeed, 1.0f)
            val altRange = max(maxAlt - minAlt, 4.0)

            val centerLng = (minLng + maxLng) / 2.0
            val centerLat = (minLat + maxLat) / 2.0

            val trackRadius = min(width, height) * 0.38f * zoomScale

            fun project(p: GPSPoint): Offset {
                val nx = ((p.longitude - centerLng) / lngRange * 2.0 - 1.0)
                val ny = ((p.latitude - centerLat) / latRange * 2.0 - 1.0)
                val px = centerX + (nx * trackRadius).toFloat() + panOffset.x
                val py = centerY - (ny * trackRadius).toFloat() + panOffset.y
                return Offset(px, py)
            }

            // Pace Color Evaluator (Speed m/s -> Pace sec/km -> Color)
            fun getSegmentColor(point: GPSPoint): Color {
                return if (colorMode == MapColorMode.PACE_HEATMAP) {
                    val speed = point.speed.coerceAtLeast(0.5f)
                    val paceSec = (1000.0 / speed).toInt().coerceIn(180, 720)
                    when {
                        paceSec <= 270 -> NeonLime // < 4:30 min/km (Fast / Sprint)
                        paceSec <= 330 -> ElectricCyan // 4:30 - 5:30 min/km (Steady)
                        paceSec <= 390 -> AcidYellow // 5:30 - 6:30 min/km (Moderate)
                        paceSec <= 480 -> BlazeOrange // 6:30 - 8:00 min/km (Recovery)
                        else -> HyperCoral // > 8:00 min/km (Warm-up / Walk)
                    }
                } else {
                    // Elevation color mode
                    val normAlt = ((point.altitude - minAlt) / altRange).toFloat().coerceIn(0f, 1f)
                    when {
                        normAlt > 0.75f -> HyperCoral
                        normAlt > 0.50f -> BlazeOrange
                        normAlt > 0.25f -> AcidYellow
                        else -> ElectricCyan
                    }
                }
            }

            val projectedOffsets = points.map { project(it) }

            // 3. Draw Route Glow Shadow Layer
            if (projectedOffsets.size >= 2) {
                for (i in 0 until projectedOffsets.size - 1) {
                    val p1 = projectedOffsets[i]
                    val p2 = projectedOffsets[i + 1]
                    val c1 = getSegmentColor(points[i]).copy(alpha = 0.22f)
                    val c2 = getSegmentColor(points[i + 1]).copy(alpha = 0.22f)

                    drawLine(
                        brush = Brush.linearGradient(listOf(c1, c2), start = p1, end = p2),
                        start = p1,
                        end = p2,
                        strokeWidth = 14f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 4. Draw Core Color-Coded Gradient Route Ribbon
            if (projectedOffsets.size >= 2) {
                for (i in 0 until projectedOffsets.size - 1) {
                    val p1 = projectedOffsets[i]
                    val p2 = projectedOffsets[i + 1]
                    val c1 = getSegmentColor(points[i])
                    val c2 = getSegmentColor(points[i + 1])

                    drawLine(
                        brush = Brush.linearGradient(listOf(c1, c2), start = p1, end = p2),
                        start = p1,
                        end = p2,
                        strokeWidth = 6.5f,
                        cap = StrokeCap.Round
                    )

                    // Direction Chevron Arrow at every 10th segment
                    if (i % 8 == 0 && i < projectedOffsets.size - 1) {
                        val dx = p2.x - p1.x
                        val dy = p2.y - p1.y
                        val len = hypot(dx, dy)
                        if (len > 12f) {
                            val midX = (p1.x + p2.x) / 2f
                            val midY = (p1.y + p2.y) / 2f
                            val angle = atan2(dy, dx)
                            val arrowLen = 6f
                            val a1X = midX - arrowLen * cos(angle - 0.5f)
                            val a1Y = midY - arrowLen * sin(angle - 0.5f)
                            val a2X = midX - arrowLen * cos(angle + 0.5f)
                            val a2Y = midY - arrowLen * sin(angle + 0.5f)

                            val arrowPath = Path().apply {
                                moveTo(a1X, a1Y)
                                lineTo(midX, midY)
                                lineTo(a2X, a2Y)
                            }
                            drawPath(
                                path = arrowPath,
                                color = DarkObsidian.copy(alpha = 0.7f),
                                style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
            }

            // 5. Draw Start Pin (🟢) & Finish Pin (🏁)
            if (projectedOffsets.isNotEmpty()) {
                val startPos = projectedOffsets.first()

                // Start Marker Outer Ring & Center
                drawCircle(color = DarkObsidian, radius = 10f, center = startPos)
                drawCircle(color = NeonLime, radius = 7f, center = startPos)
                drawCircle(color = DarkObsidian, radius = 3f, center = startPos)

                if (!isLive && projectedOffsets.size > 1) {
                    val finishPos = projectedOffsets.last()
                    drawCircle(color = DarkObsidian, radius = 10f, center = finishPos)
                    drawCircle(color = BlazeOrange, radius = 7f, center = finishPos)
                    drawCircle(color = Color.White, radius = 3f, center = finishPos)
                }
            }

            // 6. Live Runner Indicator Pulse
            if (isLive && projectedOffsets.isNotEmpty()) {
                val livePos = projectedOffsets.last()
                drawCircle(
                    color = NeonLime.copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = livePos
                )
                drawCircle(
                    color = DarkObsidian,
                    radius = 9f,
                    center = livePos
                )
                drawCircle(
                    color = NeonLime,
                    radius = 6f,
                    center = livePos
                )
            }
        }

        // Top Control Overlay: Mode Badge & Switchers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode Indicator Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark.copy(alpha = 0.85f))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        colorMode = if (colorMode == MapColorMode.PACE_HEATMAP) {
                            MapColorMode.ELEVATION_PROFILE
                        } else {
                            MapColorMode.PACE_HEATMAP
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (colorMode == MapColorMode.PACE_HEATMAP) Icons.Default.Speed else Icons.Default.Terrain,
                        contentDescription = "Color Mode",
                        tint = if (colorMode == MapColorMode.PACE_HEATMAP) NeonLime else HyperCoral,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (colorMode == MapColorMode.PACE_HEATMAP) "GRADIENT PACE" else "ELEVASI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // 3D & Zoom Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // 3D Isometric View Switch
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark.copy(alpha = 0.85f))
                        .border(1.dp, SurfaceBorder, CircleShape)
                        .clickable { is3DView = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "Switch to 3D",
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Reset Zoom
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark.copy(alpha = 0.85f))
                        .border(1.dp, SurfaceBorder, CircleShape)
                        .clickable {
                            zoomScale = 1.0f
                            panOffset = Offset.Zero
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = "Reset Center",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Bottom Gradient Legend Scale Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark.copy(alpha = 0.90f))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (colorMode == MapColorMode.PACE_HEATMAP) "GRADASI KECEPATAN (PACE)" else "GRADASI KETINGGIAN (ELEVASI)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${points.size} Titik GPS",
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }

                // Gradient Bar Strip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (colorMode == MapColorMode.PACE_HEATMAP) {
                                Brush.horizontalGradient(
                                    listOf(
                                        NeonLime,
                                        ElectricCyan,
                                        AcidYellow,
                                        BlazeOrange,
                                        HyperCoral
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        ElectricCyan,
                                        AcidYellow,
                                        BlazeOrange,
                                        HyperCoral
                                    )
                                )
                            }
                        )
                )

                // Legend Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (colorMode == MapColorMode.PACE_HEATMAP) {
                        Text(text = "⚡ Cepat (<4:30)", fontSize = 9.sp, color = NeonLime, fontWeight = FontWeight.Bold)
                        Text(text = "Stabil (5:30)", fontSize = 9.sp, color = ElectricCyan)
                        Text(text = "Sedang (6:30)", fontSize = 9.sp, color = AcidYellow)
                        Text(text = "Lambat (>8:00)", fontSize = 9.sp, color = HyperCoral)
                    } else {
                        Text(text = "Rendah", fontSize = 9.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                        Text(text = "Sedang", fontSize = 9.sp, color = AcidYellow)
                        Text(text = "Tinggi", fontSize = 9.sp, color = HyperCoral, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawGrid(width: Float, height: Float) {
    val step = 40f
    val gridColor = SurfaceBorder.copy(alpha = 0.35f)

    var x = 0f
    while (x <= width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 0.8f
        )
        x += step
    }

    var y = 0f
    while (y <= height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 0.8f
        )
        y += step
    }
}
