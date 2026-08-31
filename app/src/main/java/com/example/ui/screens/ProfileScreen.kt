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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.model.formatDuration
import com.example.ui.components.StatTile
import com.example.ui.theme.AcidYellow
import com.example.ui.theme.BlazeOrange
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
import java.util.Locale

@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    isSignedIn: Boolean,
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onUpdateWeeklyGoal: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditingGoal by remember { mutableStateOf(false) }
    var goalInput by remember { mutableStateOf("${userProfile.weeklyGoalKm.toInt()}") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkObsidian),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Profile Header Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(SurfaceDark, SurfaceElevated)
                        )
                    )
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar Ring
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.2f))
                            .border(2.dp, NeonLime, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userProfile.displayName.take(2).uppercase(Locale.ROOT),
                            style = MaterialTheme.typography.headlineMedium,
                            color = NeonLime,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = userProfile.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = userProfile.email,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(NeonLime)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = userProfile.levelTitle.uppercase(Locale.ROOT),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = DarkObsidian,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cloud Sync Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Cloud",
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Tersinkronisasi dengan Firebase Firestore",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                    }
                }
            }
        }

        // 2. Lifetime Statistics
        item {
            Text(
                text = "STATISTIK SEUMUR HIDUP",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        label = "Total Jarak",
                        value = "${userProfile.totalDistanceKm}",
                        unit = "KM",
                        icon = Icons.Default.DirectionsRun,
                        accentColor = NeonLime,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Total Sesi Lari",
                        value = "${userProfile.totalRunsCount}",
                        unit = "SESI",
                        icon = Icons.Default.Timer,
                        accentColor = ElectricCyan,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        label = "Total Waktu",
                        value = "${userProfile.totalDurationHours}",
                        unit = "JAM",
                        icon = Icons.Default.Timer,
                        accentColor = BlazeOrange,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Streak Aktif",
                        value = "${userProfile.currentStreakDays}",
                        unit = "HARI",
                        icon = Icons.Default.LocalFireDepartment,
                        accentColor = HyperCoral,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Personal Records (PR Trophy Cabinet)
        item {
            Text(
                text = "REKOR PRIBADI (PERSONAL BEST)",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val has5k = userProfile.best5kSeconds > 0
                val pace5k = if (has5k) (userProfile.best5kSeconds / 5).toInt() else 0
                PersonalRecordCard(
                    category = "5 KILOMETER",
                    time = if (has5k) formatDuration(userProfile.best5kSeconds) else "--:--",
                    pace = if (has5k) "${com.example.data.model.formatPace(pace5k)} /km" else "--'--\" /km",
                    badge = if (has5k) "🏆 Gold PB" else "Belum Ada Rekor"
                )

                val has10k = userProfile.best10kSeconds > 0
                val pace10k = if (has10k) (userProfile.best10kSeconds / 10).toInt() else 0
                PersonalRecordCard(
                    category = "10 KILOMETER",
                    time = if (has10k) formatDuration(userProfile.best10kSeconds) else "--:--",
                    pace = if (has10k) "${com.example.data.model.formatPace(pace10k)} /km" else "--'--\" /km",
                    badge = if (has10k) "🥈 Silver PB" else "Belum Ada Rekor"
                )

                val has21k = userProfile.best21kSeconds > 0
                val pace21k = if (has21k) (userProfile.best21kSeconds / 21.1).toInt() else 0
                PersonalRecordCard(
                    category = "HALF MARATHON (21.1 KM)",
                    time = if (has21k) formatDuration(userProfile.best21kSeconds) else "--:--",
                    pace = if (has21k) "${com.example.data.model.formatPace(pace21k)} /km" else "--'--\" /km",
                    badge = if (has21k) "🥉 Bronze PB" else "Belum Ada Rekor"
                )
            }
        }

        // 4. Gear Tracker
        item {
            Text(
                text = "SEPATU ANDALAN",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = userProfile.favoriteShoe,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${userProfile.shoeMileageKm} km tercatat • Siap untuk sesi latihan",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "GEAR AKTIF",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonLime
                        )
                    }
                }
            }
        }

        // 5. Auth Account Actions
        item {
            if (isSignedIn) {
                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("sign_out_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceElevated,
                        contentColor = HyperCoral
                    )
                ) {
                    Text("Keluar dari Akun Firebase", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onSignInWithGoogle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonLime,
                        contentColor = DarkObsidian
                    )
                ) {
                    Text("Masuk dengan Google (Firebase Auth)", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun PersonalRecordCard(
    category: String,
    time: String,
    pace: String,
    badge: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Pace: $pace",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = AcidYellow,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
