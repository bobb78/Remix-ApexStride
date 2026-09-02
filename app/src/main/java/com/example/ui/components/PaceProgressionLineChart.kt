package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RunActivity
import com.example.data.model.formatPace
import com.example.ui.theme.AcidYellow
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HyperCoral
import com.example.ui.theme.NeonLime
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Recharts-inspired Interactive Pace Progression Line Chart
 * Designed for Android Jetpack Compose with smooth cubic bezier spline curves,
 * dynamic gradient area fill, cartesian gridlines, and touch scrubbing tooltips.
 */
@Composable
fun PaceProgressionLineChart(
    runs: List<RunActivity>,
    isSyncingFirestore: Boolean = false,
    onRefreshFirestore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Sort chronological: oldest to newest for progression timeline
    val sortedRuns = remember(runs) {
        runs.filter { it.avgPaceSecondsPerKm > 60 && it.distanceMeters >= 200 }
            .sortedBy { it.timestamp }
    }

    var selectedRunIndex by remember(sortedRuns) {
        mutableStateOf<Int?>(if (sortedRuns.isNotEmpty()) sortedRuns.size - 1 else null)
    }

    val chartProgress = remember { Animatable(0f) }
    LaunchedEffect(sortedRuns.size) {
        chartProgress.snapTo(0f)
        chartProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    // Pace analysis (faster pace = lower seconds)
    val paceValues = sortedRuns.map { it.avgPaceSecondsPerKm }
    val minPaceSec = if (paceValues.isNotEmpty()) paceValues.minOrNull() ?: 300 else 300 // Fastest
    val maxPaceSec = if (paceValues.isNotEmpty()) paceValues.maxOrNull() ?: 480 else 480 // Slowest
    val avgPaceSec = if (paceValues.isNotEmpty()) paceValues.average().toInt() else 360

    // Progression trend: Compare first half vs second half
    val progressionPercent: Double? = if (sortedRuns.size >= 2) {
        val firstPace = sortedRuns.first().avgPaceSecondsPerKm.toDouble()
        val lastPace = sortedRuns.last().avgPaceSecondsPerKm.toDouble()
        // If last pace is faster (smaller number), improvement is positive
        ((firstPace - lastPace) / firstPace) * 100.0
    } else null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header: Title & Cloud Firestore Sync Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Progression",
                            tint = NeonLime,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "PACE PROGRESSION (RECHARTS STYLE)",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonLime,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Text(
                        text = "Tren Kecepatan Lari & Firestore",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Cloud Firestore Status / Fetch Trigger
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                ) {
                    if (isSyncingFirestore) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = ElectricCyan,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Sync Firestore...",
                                fontSize = 11.sp,
                                color = ElectricCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onRefreshFirestore,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Tarik Data Firestore",
                                tint = NeonLime,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Stat Progression Highlights
            if (sortedRuns.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Best Pace
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceElevated)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "PACE TERCEPAT",
                                fontSize = 9.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${formatPace(minPaceSec)}/km",
                                fontSize = 16.sp,
                                color = NeonLime,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Average Pace
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceElevated)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "PACE RATA-RATA",
                                fontSize = 9.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${formatPace(avgPaceSec)}/km",
                                fontSize = 16.sp,
                                color = ElectricCyan,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Trend
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceElevated)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "TREN PERFORMA",
                                fontSize = 9.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            if (progressionPercent != null) {
                                val isImproved = progressionPercent >= 0
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isImproved) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isImproved) NeonLime else HyperCoral,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%+.1f%%", progressionPercent),
                                        fontSize = 14.sp,
                                        color = if (isImproved) NeonLime else HyperCoral,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            } else {
                                Text(
                                    text = "1 Sesi",
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Tooltip Card (Selected Run)
            if (sortedRuns.isNotEmpty() && selectedRunIndex != null && selectedRunIndex!! in sortedRuns.indices) {
                val selectedRun = sortedRuns[selectedRunIndex!!]
                val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
                val dateStr = dateFormat.format(Date(selectedRun.timestamp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(SurfaceElevated, SurfaceElevated.copy(alpha = 0.8f))
                            )
                        )
                        .border(1.dp, NeonLime.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(NeonLime)
                                )
                                Text(
                                    text = selectedRun.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "$dateStr • ${selectedRun.formattedDistance} km",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${selectedRun.formattedPace}/km",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonLime
                            )
                            Text(
                                text = "Pace Sesi #${selectedRunIndex!! + 1}",
                                fontSize = 10.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Chart Canvas
            if (sortedRuns.size < 2) {
                // Not enough data points
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkObsidian.copy(alpha = 0.6f))
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = if (sortedRuns.isEmpty()) "Belum ada riwayat lari" else "Selesaikan 1 sesi lagi untuk melihat grafik tren",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Grafik Recharts ini memetakan perkembangan kecepatan lari dari waktu ke waktu secara presisi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Recharts-inspired Custom Smooth Spline Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkObsidian.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    val progress = chartProgress.value
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(sortedRuns) {
                                detectTapGestures { offset ->
                                    val count = sortedRuns.size
                                    if (count > 1) {
                                        val leftPadding = 48.dp.toPx()
                                        val rightPadding = 16.dp.toPx()
                                        val plotWidth = size.width - leftPadding - rightPadding
                                        val stepX = plotWidth / (count - 1)
                                        val clickX = (offset.x - leftPadding).coerceIn(0f, plotWidth)
                                        val nearestIdx = (clickX / stepX).toInt().coerceIn(0, count - 1)
                                        selectedRunIndex = nearestIdx
                                    }
                                }
                            }
                            .pointerInput(sortedRuns) {
                                detectDragGestures { change, _ ->
                                    val count = sortedRuns.size
                                    if (count > 1) {
                                        val leftPadding = 48.dp.toPx()
                                        val rightPadding = 16.dp.toPx()
                                        val plotWidth = size.width - leftPadding - rightPadding
                                        val clickX = (change.position.x - leftPadding).coerceIn(0f, plotWidth)
                                        val nearestIdx = (clickX / stepX(plotWidth, count)).toInt().coerceIn(0, count - 1)
                                        selectedRunIndex = nearestIdx
                                    }
                                }
                            }
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val leftPadding = 48.dp.toPx()
                        val rightPadding = 16.dp.toPx()
                        val topPadding = 18.dp.toPx()
                        val bottomPadding = 30.dp.toPx()

                        val plotWidth = canvasWidth - leftPadding - rightPadding
                        val plotHeight = canvasHeight - topPadding - bottomPadding

                        // Y Scale: In running pace, smaller pace (e.g. 4:00) is BETTER, so we put faster pace at the TOP (lower y)
                        // Range with margins
                        val minRange = (minPaceSec - 20).coerceAtLeast(120)
                        val maxRange = (maxPaceSec + 20).coerceAtLeast(minRange + 40)

                        fun paceToY(pace: Int): Float {
                            val fraction = (pace - minRange).toFloat() / (maxRange - minRange).toFloat()
                            // fraction = 0 -> minRange (fastest) -> topPadding
                            // fraction = 1 -> maxRange (slowest) -> topPadding + plotHeight
                            return topPadding + (fraction * plotHeight)
                        }

                        // 1. Draw Cartesian Gridlines (Recharts style)
                        val gridLinesCount = 4
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(140, 160, 160, 175)
                            textSize = 24f
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.MONOSPACE
                        }

                        for (i in 0..gridLinesCount) {
                            val y = topPadding + (i.toFloat() / gridLinesCount) * plotHeight
                            val paceAtLine = minRange + ((i.toFloat() / gridLinesCount) * (maxRange - minRange)).toInt()
                            val paceStr = formatPace(paceAtLine)

                            // Grid line
                            drawLine(
                                color = SurfaceBorder.copy(alpha = 0.5f),
                                start = Offset(leftPadding, y),
                                end = Offset(canvasWidth - rightPadding, y),
                                strokeWidth = 1.2f,
                                pathEffect = dashEffect
                            )

                            // Y-axis label (min:sec/km)
                            drawContext.canvas.nativeCanvas.drawText(
                                paceStr,
                                4f,
                                y + 8f,
                                textPaint
                            )
                        }

                        // 2. Compute Points for Runs
                        val count = sortedRuns.size
                        val step = plotWidth / (count - 1)
                        val points = sortedRuns.mapIndexed { idx, run ->
                            val x = leftPadding + (idx * step)
                            val targetY = paceToY(run.avgPaceSecondsPerKm)
                            // Animated Y entry
                            val animatedY = (topPadding + plotHeight) - ((topPadding + plotHeight - targetY) * progress)
                            Offset(x, animatedY)
                        }

                        // 3. Build Smooth Monotone Cubic Spline (Recharts <Line type="monotone" />)
                        val linePath = Path()
                        val areaPath = Path()

                        if (points.isNotEmpty()) {
                            linePath.moveTo(points.first().x, points.first().y)
                            areaPath.moveTo(points.first().x, topPadding + plotHeight)
                            areaPath.lineTo(points.first().x, points.first().y)

                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i + 1]

                                val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                                val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)

                                linePath.cubicTo(
                                    controlPoint1.x, controlPoint1.y,
                                    controlPoint2.x, controlPoint2.y,
                                    p1.x, p1.y
                                )
                                areaPath.cubicTo(
                                    controlPoint1.x, controlPoint1.y,
                                    controlPoint2.x, controlPoint2.y,
                                    p1.x, p1.y
                                )
                            }

                            areaPath.lineTo(points.last().x, topPadding + plotHeight)
                            areaPath.close()

                            // Draw Area Gradient (NeonLime to Transparent)
                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        NeonLime.copy(alpha = 0.35f * progress),
                                        NeonLime.copy(alpha = 0.08f * progress),
                                        Color.Transparent
                                    ),
                                    startY = topPadding,
                                    endY = topPadding + plotHeight
                                )
                            )

                            // Draw Spline Line (NeonLime Stroke)
                            drawPath(
                                path = linePath,
                                color = NeonLime,
                                style = Stroke(
                                    width = 3.5.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // 4. Draw X-Axis Date Labels
                        val datePaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(170, 160, 160, 175)
                            textSize = 22f
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        val dayFormat = SimpleDateFormat("d MMM", Locale("id", "ID"))

                        // Select up to 4 evenly spaced labels
                        val labelIndices = if (count <= 4) {
                            points.indices.toList()
                        } else {
                            listOf(0, count / 3, (2 * count) / 3, count - 1)
                        }

                        labelIndices.forEach { idx ->
                            val pt = points[idx]
                            val labelText = dayFormat.format(Date(sortedRuns[idx].timestamp))
                            drawContext.canvas.nativeCanvas.drawText(
                                labelText,
                                pt.x,
                                topPadding + plotHeight + 20.dp.toPx(),
                                datePaint
                            )
                        }

                        // 5. Draw Data Dots
                        points.forEachIndexed { idx, pt ->
                            val isSelected = selectedRunIndex == idx
                            // Outer ring
                            drawCircle(
                                color = if (isSelected) ElectricCyan else DarkObsidian,
                                radius = if (isSelected) 8.dp.toPx() else 5.dp.toPx(),
                                center = pt
                            )
                            // Inner core
                            drawCircle(
                                color = if (isSelected) NeonLime else NeonLime.copy(alpha = 0.85f),
                                radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                                center = pt
                            )
                        }

                        // 6. Draw Selected Crosshair Cursor (Recharts tooltip cursor)
                        if (selectedRunIndex != null && selectedRunIndex!! in points.indices) {
                            val selPoint = points[selectedRunIndex!!]
                            drawLine(
                                color = ElectricCyan.copy(alpha = 0.8f),
                                start = Offset(selPoint.x, topPadding),
                                end = Offset(selPoint.x, topPadding + plotHeight),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                            )
                        }
                    }
                }
            }

            // Legend / Tip footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(NeonLime)
                    )
                    Text(
                        text = "Pace (min/km)",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Cloud",
                        tint = ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Firestore Cloud Synced",
                        fontSize = 11.sp,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun stepX(plotWidth: Float, count: Int): Float {
    return if (count > 1) plotWidth / (count - 1) else plotWidth
}
