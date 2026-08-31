package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateRight
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GPSPoint
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 3D Isometric & Perspective Route Map Canvas
 * Features:
 * - 3D Perspective with tilt (isometric elevation) and 360-degree rotation.
 * - 3D Elevated Ribbon Track suspended above ground terrain with vertical depth curtain walls.
 * - 3D Milestone Flag Pillars for KM splits.
 * - 3D Runner Avatar with dynamic directional heading and ground radar ripples.
 * - Interactive pitch tilt, touch-drag 360-degree orbit rotation, and pinch zoom.
 */
@Composable
fun PaceHeatmap3DCanvas(
    points: List<GPSPoint>,
    modifier: Modifier = Modifier,
    isLive: Boolean = false,
    showGrid: Boolean = true,
    initialIs3D: Boolean = true
) {
    var is3DMode by remember { mutableStateOf(initialIs3D) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var rotationAngleDeg by remember { mutableFloatStateOf(25f) } // 3D Yaw rotation
    var pitchAngleDeg by remember { mutableFloatStateOf(52f) } // 3D Pitch tilt (0 = 2D, 55 = 3D Isometric)
    var elevationMultiplier by remember { mutableFloatStateOf(1.5f) }

    val infiniteTransition = rememberInfiniteTransition(label = "3d_radar_pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
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
                    detectTransformGestures { _, pan, zoom, rotation ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.6f, 5.0f)
                        if (is3DMode) {
                            rotationAngleDeg = (rotationAngleDeg + pan.x * 0.45f) % 360f
                            pitchAngleDeg = (pitchAngleDeg - pan.y * 0.25f).coerceIn(20f, 75f)
                        } else {
                            panOffset += pan
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f

            // 1. Draw 3D Ground Mesh / Tactical Base Grid
            if (showGrid) {
                if (is3DMode) {
                    draw3DGroundGrid(width, height, pitchAngleDeg, rotationAngleDeg)
                } else {
                    draw2DGrid(width, height)
                }
            }

            if (points.isEmpty()) {
                // Standby radar visual
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

            // 2. Compute GPS Coordinates Bounding Box
            var minLat = Double.MAX_VALUE
            var maxLat = -Double.MAX_VALUE
            var minLng = Double.MAX_VALUE
            var maxLng = -Double.MAX_VALUE
            var minAlt = Double.MAX_VALUE
            var maxAlt = -Double.MAX_VALUE
            var minSpeed = Float.MAX_VALUE
            var maxSpeed = Float.MIN_VALUE

            for (p in points) {
                minLat = min(minLat, p.latitude)
                maxLat = max(maxLat, p.latitude)
                minLng = min(minLng, p.longitude)
                maxLng = max(maxLng, p.longitude)
                minAlt = min(minAlt, p.altitude)
                maxAlt = max(maxAlt, p.altitude)
                minSpeed = min(minSpeed, p.speed)
                maxSpeed = max(maxSpeed, p.speed)
            }

            val latRange = max(maxLat - minLat, 0.0004)
            val lngRange = max(maxLng - minLng, 0.0004)
            val speedRange = max(maxSpeed - minSpeed, 1.0f)
            val altRange = max(maxAlt - minAlt, 4.0)

            val centerLng = (minLng + maxLng) / 2.0
            val centerLat = (minLat + maxLat) / 2.0

            val trackRadius = min(width, height) * 0.38f * zoomScale

            // 3D Projection Math
            val pitchRad = Math.toRadians(if (is3DMode) pitchAngleDeg.toDouble() else 0.0)
            val yawRad = Math.toRadians(if (is3DMode) rotationAngleDeg.toDouble() else 0.0)

            val cosPitch = cos(pitchRad)
            val sinPitch = sin(pitchRad)
            val cosYaw = cos(yawRad)
            val sinYaw = sin(yawRad)

            // Convert GPS point to 3D projected screen coordinate
            // Returns Pair(GroundPoint, ElevatedPoint)
            fun project3D(p: GPSPoint): Pair<Offset, Offset> {
                // Normalized relative position from centroid (-1.0 to 1.0)
                val nx = ((p.longitude - centerLng) / lngRange * 2.0 - 1.0)
                val ny = ((p.latitude - centerLat) / latRange * 2.0 - 1.0)
                val normAlt = ((p.altitude - minAlt) / altRange).toFloat().coerceIn(0f, 1f)

                // Scale to world space units
                val wx = nx * trackRadius
                val wy = -ny * trackRadius // Invert latitude for screen Y

                // 3D Yaw Rotation around center Z axis
                val rx = (wx * cosYaw - wy * sinYaw).toFloat()
                val ry = (wx * sinYaw + wy * cosYaw).toFloat()

                // 3D Pitch tilt: Y compresses by cos(pitch), Z elevates upwards
                val groundScreenX = centerX + rx + panOffset.x
                val groundScreenY = centerY + (ry * cosPitch).toFloat() + panOffset.y

                // Height offset in 3D (altitude + base ribbon lift of 24dp)
                val baseLift = if (is3DMode) 32f else 0f
                val altLift = if (is3DMode) (normAlt * 48f * elevationMultiplier) else 0f
                val totalZ = baseLift + altLift

                val elevatedScreenX = groundScreenX
                val elevatedScreenY = groundScreenY - totalZ

                return Pair(Offset(groundScreenX, groundScreenY), Offset(elevatedScreenX, elevatedScreenY))
            }

            // 3. Render 3D Ground Drop Shadow / Volumetric Curtain Wall
            if (is3DMode && points.size >= 2) {
                for (i in 0 until points.size - 1) {
                    val (g1, e1) = project3D(points[i])
                    val (g2, e2) = project3D(points[i + 1])

                    val curtainPath = Path().apply {
                        moveTo(e1.x, e1.y)
                        lineTo(e2.x, e2.y)
                        lineTo(g2.x, g2.y)
                        lineTo(g1.x, g1.y)
                        close()
                    }

                    val normSpeed = ((points[i + 1].speed - minSpeed) / speedRange).coerceIn(0f, 1f)
                    val baseColor = when {
                        normSpeed > 0.65f -> NeonLime
                        normSpeed > 0.35f -> ElectricCyan
                        else -> HyperCoral
                    }

                    // Draw vertical drop curtain
                    drawPath(
                        path = curtainPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.35f),
                                baseColor.copy(alpha = 0.05f)
                            ),
                            startY = min(e1.y, e2.y),
                            endY = max(g1.y, g2.y)
                        )
                    )

                    // Ground shadow line
                    drawLine(
                        color = Color.Black.copy(alpha = 0.45f),
                        start = g1,
                        end = g2,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 4. Render 3D Elevated Ribbon Track
            if (points.size >= 2) {
                // Wide ambient neon halo
                val elevatedPath = Path()
                val (_, firstElevated) = project3D(points.first())
                elevatedPath.moveTo(firstElevated.x, firstElevated.y)

                for (i in 1 until points.size) {
                    val (_, e) = project3D(points[i])
                    elevatedPath.lineTo(e.x, e.y)
                }

                drawPath(
                    path = elevatedPath,
                    color = NeonLime.copy(alpha = 0.22f),
                    style = Stroke(
                        width = 16f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // High-precision colored ribbon segments
                for (i in 0 until points.size - 1) {
                    val (_, e1) = project3D(points[i])
                    val (_, e2) = project3D(points[i + 1])

                    val normSpeed = ((points[i + 1].speed - minSpeed) / speedRange).coerceIn(0f, 1f)
                    val segmentColor = when {
                        normSpeed > 0.65f -> NeonLime
                        normSpeed > 0.35f -> ElectricCyan
                        else -> HyperCoral
                    }

                    // Main track line
                    drawLine(
                        color = segmentColor,
                        start = e1,
                        end = e2,
                        strokeWidth = if (is3DMode) 7f else 6f,
                        cap = StrokeCap.Round
                    )
                }

                // 5. 3D Milestone KM Split Pillars
                var accumulatedMeters = 0.0
                var currentKm = 1
                for (i in 1 until points.size) {
                    val pPrev = points[i - 1]
                    val pCurr = points[i]
                    accumulatedMeters += hypot(
                        (pCurr.longitude - pPrev.longitude) * 111000.0,
                        (pCurr.latitude - pPrev.latitude) * 111000.0
                    )

                    if (accumulatedMeters >= currentKm * 1000.0) {
                        val (groundPt, elevatedPt) = project3D(pCurr)

                        if (is3DMode) {
                            // Vertical 3D pillar pole anchored to ground
                            drawLine(
                                color = NeonLime.copy(alpha = 0.7f),
                                start = groundPt,
                                end = Offset(elevatedPt.x, elevatedPt.y - 18f),
                                strokeWidth = 2f
                            )
                            drawCircle(
                                color = DarkObsidian,
                                radius = 4f,
                                center = groundPt
                            )
                            drawCircle(
                                color = NeonLime,
                                radius = 3.5f,
                                center = groundPt,
                                style = Stroke(width = 1.5f)
                            )
                        }

                        // Floating Split Flag Badge
                        val flagCenter = Offset(elevatedPt.x, elevatedPt.y - (if (is3DMode) 18f else 0f))
                        drawCircle(
                            color = DarkObsidian,
                            radius = 9f,
                            center = flagCenter
                        )
                        drawCircle(
                            color = NeonLime,
                            radius = 8f,
                            center = flagCenter,
                            style = Stroke(width = 2.5f)
                        )
                        drawCircle(
                            color = NeonLime,
                            radius = 3.5f,
                            center = flagCenter
                        )

                        currentKm++
                    }
                }
            }

            // 6. Start Point Marker (Green Flag)
            val (startGround, startElevated) = project3D(points.first())
            if (is3DMode) {
                drawLine(
                    color = NeonLime,
                    start = startGround,
                    end = startElevated,
                    strokeWidth = 2f
                )
            }
            drawCircle(
                color = NeonLime,
                radius = 9f,
                center = startElevated
            )
            drawCircle(
                color = DarkObsidian,
                radius = 4.5f,
                center = startElevated
            )

            // 7. Live / End Runner Avatar with 3D Heading
            val (lastGround, lastElevated) = project3D(points.last())
            if (isLive) {
                // Ground projection radar rings
                if (is3DMode) {
                    drawCircle(
                        color = ElectricCyan.copy(alpha = pulseAlpha * 0.6f),
                        radius = pulseRadius * 1.2f,
                        center = lastGround,
                        style = Stroke(width = 1.5f)
                    )
                    // Drop line connecting runner to ground
                    drawLine(
                        color = ElectricCyan.copy(alpha = 0.8f),
                        start = lastGround,
                        end = lastElevated,
                        strokeWidth = 2.5f
                    )
                }

                // Pulsing 3D Runner Sphere
                drawCircle(
                    color = ElectricCyan.copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = lastElevated
                )
                drawCircle(
                    color = ElectricCyan,
                    radius = 9f,
                    center = lastElevated
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.5f,
                    center = lastElevated
                )
            } else {
                // Finish Checkered Marker
                drawCircle(
                    color = HyperCoral,
                    radius = 9f,
                    center = lastElevated
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.5f,
                    center = lastElevated
                )
            }
        }

        // Top Overlay Controls: 3D Toggle, 360° Orbit, Zoom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // 3D / 2D Mode Switcher Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceDark.copy(alpha = 0.9f))
                    .border(1.dp, if (is3DMode) NeonLime else SurfaceBorder, RoundedCornerShape(20.dp))
                    .clickable { is3DMode = !is3DMode }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (is3DMode) Icons.Default.ViewInAr else Icons.Default.Terrain,
                        contentDescription = "3D View",
                        tint = if (is3DMode) NeonLime else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (is3DMode) "3D ISOMETRIC" else "2D TOP-DOWN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (is3DMode) NeonLime else TextPrimary,
                        letterSpacing = 1.sp
                    )
                }
            }

            // 3D Orbit & Zoom Controls
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // 360 Orbit Rotate Button
                if (is3DMode) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark.copy(alpha = 0.9f))
                            .border(1.dp, SurfaceBorder, CircleShape)
                            .clickable { rotationAngleDeg = (rotationAngleDeg + 45f) % 360f },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Rotate 3D",
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Zoom In
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark.copy(alpha = 0.9f))
                        .border(1.dp, SurfaceBorder, CircleShape)
                        .clickable { zoomScale = (zoomScale * 1.25f).coerceAtMost(5f) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Zoom Out
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark.copy(alpha = 0.9f))
                        .border(1.dp, SurfaceBorder, CircleShape)
                        .clickable { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.6f) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Reset Camera
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark.copy(alpha = 0.9f))
                        .border(1.dp, SurfaceBorder, CircleShape)
                        .clickable {
                            zoomScale = 1.0f
                            rotationAngleDeg = 25f
                            pitchAngleDeg = 52f
                            panOffset = Offset.Zero
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = "Reset Camera",
                        tint = NeonLime,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Bottom Legend / Perspective Info Badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark.copy(alpha = 0.9f))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (is3DMode) {
                    Text(
                        text = "3D Orbit: ${rotationAngleDeg.toInt()}° • Kemiringan: ${pitchAngleDeg.toInt()}°",
                        fontSize = 10.sp,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NeonLime))
                        Text("Sprint", fontSize = 9.sp, color = TextSecondary)
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ElectricCyan))
                        Text("Steady", fontSize = 9.sp, color = TextSecondary)
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(HyperCoral))
                        Text("Jog/Tanjakan", fontSize = 9.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

private fun DrawScope.draw3DGroundGrid(width: Float, height: Float, pitchDeg: Float, yawDeg: Float) {
    val centerX = width / 2f
    val centerY = height / 2f
    val gridColor = SurfaceBorder.copy(alpha = 0.4f)
    val accentColor = ElectricCyan.copy(alpha = 0.15f)

    val pitchRad = Math.toRadians(pitchDeg.toDouble())
    val yawRad = Math.toRadians(yawDeg.toDouble())
    val cosPitch = cos(pitchRad).toFloat()
    val cosYaw = cos(yawRad).toFloat()
    val sinYaw = sin(yawRad).toFloat()

    val gridSize = min(width, height) * 0.65f
    val step = gridSize / 5f

    for (i in -4..4) {
        val offsetVal = i * step

        // Transform line along X-axis
        val p1x = ((-gridSize) * cosYaw - offsetVal * sinYaw)
        val p1y = ((-gridSize) * sinYaw + offsetVal * cosYaw) * cosPitch
        val p2x = (gridSize * cosYaw - offsetVal * sinYaw)
        val p2y = (gridSize * sinYaw + offsetVal * cosYaw) * cosPitch

        drawLine(
            color = if (i == 0) accentColor else gridColor,
            start = Offset(centerX + p1x, centerY + p1y),
            end = Offset(centerX + p2x, centerY + p2y),
            strokeWidth = if (i == 0) 1.5f else 0.8f
        )

        // Transform line along Y-axis
        val q1x = (offsetVal * cosYaw - (-gridSize) * sinYaw)
        val q1y = (offsetVal * sinYaw + (-gridSize) * cosYaw) * cosPitch
        val q2x = (offsetVal * cosYaw - gridSize * sinYaw)
        val q2y = (offsetVal * sinYaw + gridSize * cosYaw) * cosPitch

        drawLine(
            color = if (i == 0) accentColor else gridColor,
            start = Offset(centerX + q1x, centerY + q1y),
            end = Offset(centerX + q2x, centerY + q2y),
            strokeWidth = if (i == 0) 1.5f else 0.8f
        )
    }
}

private fun DrawScope.draw2DGrid(width: Float, height: Float) {
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
