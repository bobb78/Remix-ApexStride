package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.formatDuration
import com.example.data.model.formatPace
import com.example.service.LiveRunTelemetry
import com.example.service.RunTrackingState
import com.example.ui.components.ElevationProfileCanvas
import com.example.ui.components.PaceHeatmap3DCanvas
import com.example.ui.components.StatTile
import com.example.ui.components.TrackLapsBarChart
import com.example.ui.components.TrackStadiumLapGauge
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

enum class LiveRunDisplayMode {
    TRACK_LAP_100M,
    KM_TELEMETRY,
    MAP_3D
}

@Composable
fun LiveRunScreen(
    telemetry: LiveRunTelemetry,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onManualLap: () -> Unit = {},
    onToggleVoiceCoach: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isTrackSession = telemetry.activityType.contains("Track", ignoreCase = true)
    var displayMode by remember(isTrackSession) {
        mutableStateOf(if (isTrackSession) LiveRunDisplayMode.TRACK_LAP_100M else LiveRunDisplayMode.KM_TELEMETRY)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Session Header with Sensor Status & Voice Coach
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
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
                                .background(if (telemetry.isActivelyMoving) NeonLime else HyperCoral)
                        )
                        Text(
                            text = if (telemetry.state == RunTrackingState.RUNNING) {
                                if (telemetry.isActivelyMoving) "PELACAKAN AKTIF" else "SENSOR: DIAM"
                            } else {
                                "SESI DIJEDA"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (telemetry.isActivelyMoving) NeonLime else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Text(
                        text = telemetry.movementStatusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (telemetry.isActivelyMoving) ElectricCyan else TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Voice Guidance Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark.copy(alpha = 0.6f))
                            .border(1.dp, SurfaceBorder, CircleShape)
                            .clickable { onToggleVoiceCoach() }
                            .testTag("voice_coach_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (telemetry.isVoiceCoachEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Voice Coach",
                            tint = if (telemetry.isVoiceCoachEnabled) NeonLime else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mode Selector Tabs (Putaran 100m vs Telemetry KM vs Peta 3D)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    LiveRunDisplayMode.TRACK_LAP_100M to "Putaran (100m)",
                    LiveRunDisplayMode.KM_TELEMETRY to "Jarak (KM)",
                    LiveRunDisplayMode.MAP_3D to "Peta 3D"
                ).forEach { (mode, title) ->
                    val isSelected = displayMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NeonLime else Color.Transparent)
                            .clickable { displayMode = mode }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) DarkObsidian else TextSecondary
                        )
                    }
                }
            }
        }

        // 2. Main Center: 100m Track Stadium Gauge / Kilometers Telemetry / 3D Map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            when (displayMode) {
                LiveRunDisplayMode.TRACK_LAP_100M -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TrackStadiumLapGauge(
                            lapNumber = telemetry.currentLapNumber,
                            currentLapMeters = telemetry.currentLapMeters,
                            currentLapProgress = telemetry.currentLapProgressPercent,
                            currentLapDurationSeconds = telemetry.currentLapDurationSeconds,
                            targetLaps = telemetry.targetLaps
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Target Lap Progress Bar if set
                        if (telemetry.targetLaps > 0) {
                            val totalLapProgress = (telemetry.completedLaps.toFloat() / telemetry.targetLaps).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Target ${telemetry.targetLaps} Putaran (${telemetry.targetLaps * 100}m)",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${(totalLapProgress * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        color = NeonLime,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                LiveRunDisplayMode.KM_TELEMETRY -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Volt Atmospheric Glow
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(CircleShape)
                                .background(NeonLime.copy(alpha = 0.06f))
                                .blur(40.dp)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2f", telemetry.distanceKm),
                                fontSize = 84.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-2).sp,
                                lineHeight = 86.sp
                            )
                            Text(
                                text = "KILOMETER",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonLime,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Putaran 100m Summary Tag
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(SurfaceDark)
                                        .border(1.dp, AcidYellow.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "🏁 ${telemetry.completedLaps} Putaran Selesai",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AcidYellow
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(SurfaceDark)
                                        .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "${telemetry.currentCadenceSpm} SPM",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                LiveRunDisplayMode.MAP_3D -> {
                    PaceHeatmap3DCanvas(
                        points = telemetry.points,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 6.dp),
                        isLive = true,
                        showGrid = true,
                        initialIs3D = true
                    )
                }
            }
        }

        // 3. 2-Column Metrics & Lap/Elevation Profile
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    label = if (displayMode == LiveRunDisplayMode.TRACK_LAP_100M) "Pace Putaran" else "Pace Rata-rata",
                    value = formatPace(telemetry.avgPaceSecondsPerKm),
                    unit = "/KM",
                    icon = Icons.Default.Speed,
                    accentColor = NeonLime,
                    modifier = Modifier.weight(1f)
                )

                StatTile(
                    label = "Total Putaran",
                    value = "${telemetry.completedLaps}",
                    unit = "x100m",
                    icon = Icons.Default.Timelapse,
                    accentColor = AcidYellow,
                    modifier = Modifier.weight(1f)
                )
            }

            // If in Track Lap mode and has completed laps, show recent laps chart
            if (displayMode == LiveRunDisplayMode.TRACK_LAP_100M && telemetry.laps.isNotEmpty()) {
                TrackLapsBarChart(
                    laps = telemetry.laps.takeLast(3),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ElevationProfileCanvas(splits = telemetry.splits)
            }
        }

        // 4. Action Controls & Manual Lap Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pill Container with Pause/Resume Button & Digital Stopwatch
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(36.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (telemetry.state == RunTrackingState.RUNNING) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable { onPause() }
                                    .testTag("pause_run_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = DarkObsidian,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(NeonLime)
                                        .clickable { onResume() }
                                        .testTag("resume_run_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Resume",
                                        tint = DarkObsidian,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(HyperCoral)
                                        .clickable { onFinish() }
                                        .testTag("finish_run_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Finish",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }

                        // Digital Stopwatch
                        Text(
                            text = formatDuration(telemetry.elapsedSeconds),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(end = 14.dp)
                        )
                    }
                }

                // Manual Lap Trigger Button (Tandai 1 Putaran 400m Sekarang)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AcidYellow)
                        .clickable(enabled = telemetry.state == RunTrackingState.RUNNING) {
                            onManualLap()
                        }
                        .testTag("manual_lap_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Tandai Putaran",
                            tint = DarkObsidian,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "LAP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = DarkObsidian
                        )
                    }
                }
            }
        }
    }
}
