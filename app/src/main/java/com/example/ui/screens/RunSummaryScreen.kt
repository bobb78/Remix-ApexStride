package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RunActivity
import com.example.data.model.formatDuration
import com.example.data.model.formatPace
import com.example.ui.components.ElevationProfileCanvas
import com.example.ui.components.PaceHeatmap3DCanvas
import com.example.ui.components.SplitsBarChart
import com.example.ui.components.StatTile
import com.example.ui.components.TrackLapsBarChart
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSummaryScreen(
    run: RunActivity,
    onSaveAndClose: (title: String, feeling: String, shoe: String, notes: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var titleText by remember { mutableStateOf(run.title) }
    var selectedFeeling by remember { mutableStateOf(run.feelingTag) }
    var shoeText by remember { mutableStateOf(run.shoeName) }
    var notesText by remember { mutableStateOf(run.notes) }
    var showShareSheet by remember { mutableStateOf(false) }

    val feelingOptions = listOf("🔥 Kuat", "🚀 Luar Biasa", "✨ Segar", "💨 Cepat", "💪 Capek Tapi Puas")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "RINGKASAN AKTIVITAS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showShareSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = ElectricCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkObsidian,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkObsidian)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { onSaveAndClose(titleText, selectedFeeling, shoeText, notesText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("save_run_summary_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonLime,
                        contentColor = DarkObsidian
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Save and Sync",
                        tint = DarkObsidian,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SIMPAN KE JURNAL PRIBADI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        },
        containerColor = DarkObsidian
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkObsidian),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Title & Feeling Tag Edit Box
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        label = { Text("Judul Sesi", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Feeling selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        feelingOptions.take(3).forEach { feeling ->
                            val isSelected = selectedFeeling == feeling
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) NeonLime else SurfaceElevated)
                                    .border(1.dp, if (isSelected) NeonLime else SurfaceBorder, RoundedCornerShape(20.dp))
                                    .clickable { selectedFeeling = feeling }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = feeling,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) DarkObsidian else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 2. Giant Headline Telemetry
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    SurfaceDark,
                                    SurfaceElevated
                                )
                            )
                        )
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "TOTAL JARAK TEMPUH",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = run.formattedDistance,
                                style = MaterialTheme.typography.displayLarge,
                                color = NeonLime,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "KM",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "DURASI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = run.formattedDuration,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(SurfaceBorder)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "AVG PACE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${run.formattedPace}/km",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ElectricCyan,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(SurfaceBorder)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "KALORI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${run.caloriesBurned}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = BlazeOrange,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // 3. Interactive 3D/2D Route & Pace Heatmap Canvas
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "RUTE & HEATMAP PACE (3D / 2D)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    PaceHeatmap3DCanvas(
                        points = run.routePoints,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        isLive = false,
                        showGrid = true,
                        initialIs3D = true
                    )
                }
            }

            // 4. Biometrics Stat Tiles Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            label = "Cadence Rata-rata",
                            value = "${run.avgCadenceSpm}",
                            unit = "SPM",
                            icon = if (run.activityType == "Lari") Icons.Default.DirectionsRun else Icons.Default.DirectionsWalk,
                            accentColor = NeonLime,
                            modifier = Modifier.weight(1f)
                        )

                        StatTile(
                            label = "Heart Rate",
                            value = "${run.avgHeartRateBpm}",
                            unit = "BPM",
                            icon = Icons.Default.Favorite,
                            accentColor = HyperCoral,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            label = "Elevasi Mendaki",
                            value = "+${run.elevationGainMeters}",
                            unit = "METER",
                            icon = Icons.Default.Terrain,
                            accentColor = ElectricCyan,
                            modifier = Modifier.weight(1f)
                        )

                        StatTile(
                            label = "Top Speed Pace",
                            value = com.example.data.model.formatPace(run.maxPaceSecondsPerKm),
                            unit = "/KM",
                            icon = Icons.Default.Speed,
                            accentColor = BlazeOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. Track 400m Laps Breakdown & Chart (if any laps recorded)
            if (run.laps.isNotEmpty()) {
                item {
                    TrackLapsSummarySection(run = run)
                }
            }

            // 6. Splits & Elevation Charts
            item {
                SplitsBarChart(splits = run.splits)
            }

            item {
                ElevationProfileCanvas(splits = run.splits)
            }

            // 6. Recovery & Personal Notes Section
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "ESTIMASI PEMULIHAN & CATATAN PRIBADI",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonLime,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceElevated)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Rekomendasi Waktu Istirahat", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    text = if (run.distanceKm > 8.0) "24 - 36 Jam" else "18 - 24 Jam",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "💧 Minum 500ml",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                        }

                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            label = { Text("Catatan Latihan / Evaluasi Diri", color = TextSecondary) },
                            placeholder = { Text("Contoh: Rasanya ringan di 3km awal, tanjakan terasa menantang.", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonLime,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = SurfaceElevated,
                                unfocusedContainerColor = SurfaceElevated
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }
    }

    // Share Poster Modal Sheet
    if (showShareSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            containerColor = DarkObsidian
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "POSTER PREVIEW ATLET",
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold
                )

                // Share Poster Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(SurfaceDark, SlateDark)
                            )
                        )
                        .border(1.dp, NeonLime.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "APEX STRIDE // ATLET PROTOCOL",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonLime,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${run.formattedDistance} KM",
                            style = MaterialTheme.typography.displayLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Pace ${run.formattedPace}/km • Waktu ${run.formattedDuration}",
                            style = MaterialTheme.typography.titleMedium,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        PaceHeatmap3DCanvas(
                            points = run.routePoints,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            isLive = false,
                            showGrid = false,
                            initialIs3D = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "${run.title} • ${run.formattedDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                Button(
                    onClick = { showShareSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonLime,
                        contentColor = DarkObsidian
                    )
                ) {
                    Text("BAGIKAN KE MEDIA SOSIAL", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TrackLapsSummarySection(
    run: RunActivity,
    modifier: Modifier = Modifier
) {
    val fastestLap = run.laps.minByOrNull { it.durationSeconds }
    val avgLapDuration = if (run.laps.isNotEmpty()) run.laps.map { it.durationSeconds }.average().toLong() else 0L

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark)
            .border(1.dp, AcidYellow.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AcidYellow.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = "Laps",
                            tint = AcidYellow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "ANALISIS PUTARAN TRACK (100M)",
                        style = MaterialTheme.typography.labelMedium,
                        color = AcidYellow,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${run.laps.size} PUTARAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Quick Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(text = "LAP TERCEPAT", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        fastestLap?.let {
                            Text(
                                text = formatDuration(it.durationSeconds),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonLime
                            )
                            Text(text = "Lap #${it.lapNumber} (${formatPace(it.paceSecondsPerKm)}/km)", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(text = "RATA-RATA LAP", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatDuration(avgLapDuration),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(text = "per 100 meter", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }

            // Lap Visualization Bar Chart
            TrackLapsBarChart(laps = run.laps)

            // Laps Table Rows
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceElevated)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("PUTARAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Text("WAKTU", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Text("PACE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                }

                run.laps.forEach { lap ->
                    val isFastest = fastestLap?.lapNumber == lap.lapNumber
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Lap #${lap.lapNumber}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFastest) NeonLime else TextPrimary
                            )
                            if (isFastest) {
                                Text(text = "⚡", fontSize = 11.sp)
                            }
                        }

                        Text(
                            text = formatDuration(lap.durationSeconds),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )

                        Text(
                            text = "${formatPace(lap.paceSecondsPerKm)}/km",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFastest) NeonLime else ElectricCyan
                        )
                    }
                }
            }
        }
    }
}
