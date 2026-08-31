package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.AppDestination
import com.example.ui.MainViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveRunScreen
import com.example.ui.screens.PerformanceAnalyticsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RunHistoryScreen
import com.example.ui.screens.RunSummaryScreen
import com.example.ui.theme.ApexStrideTheme
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.NeonLime
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ApexStrideTheme {
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { /* Permissions granted */ }

                LaunchedEffect(Unit) {
                    val permissions = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    val ungranted = permissions.filter {
                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (ungranted.isNotEmpty()) {
                        permissionLauncher.launch(ungranted.toTypedArray())
                    }
                }

                ApexStrideApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ApexStrideApp(viewModel: MainViewModel) {
    val currentDest by viewModel.currentDestination.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val allRuns by viewModel.allRuns.collectAsState()
    val performanceMetrics by viewModel.performanceMetrics.collectAsState()
    val liveTelemetry by viewModel.liveTelemetry.collectAsState()
    val summaryRun by viewModel.summaryRun.collectAsState()
    val isSignedIn by viewModel.isSignedIn.collectAsState()

    // Handle Hardware Back Button
    BackHandler(enabled = currentDest != AppDestination.HOME) {
        when (currentDest) {
            AppDestination.LIVE_RUN -> viewModel.pauseRun()
            AppDestination.RUN_SUMMARY -> viewModel.closeSummaryWithoutSaving()
            else -> viewModel.navigateTo(AppDestination.HOME)
        }
    }

    val showBottomNav = currentDest in listOf(
        AppDestination.HOME,
        AppDestination.HISTORY,
        AppDestination.ANALYTICS,
        AppDestination.PROFILE
    )

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                ApexBottomNavBar(
                    currentDestination = currentDest,
                    onNavigate = { dest -> viewModel.navigateTo(dest) },
                    onStartRunClick = { viewModel.startRun(isSimulation = false, activityType = "Lari") }
                )
            }
        },
        containerColor = DarkObsidian
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkObsidian)
        ) {
            AnimatedContent(
                targetState = currentDest,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { target ->
                when (target) {
                    AppDestination.HOME -> {
                        HomeScreen(
                            userProfile = userProfile,
                            recentRuns = allRuns,
                            onStartRun = { isSim, actType, targetLaps -> viewModel.startRun(isSim, actType, targetLaps) },
                            onViewRunDetail = { runId -> viewModel.viewRunDetail(runId) },
                            onNavigateToAnalytics = { viewModel.navigateTo(AppDestination.ANALYTICS) },
                            onNavigateToHistory = { viewModel.navigateTo(AppDestination.HISTORY) }
                        )
                    }

                    AppDestination.HISTORY -> {
                        RunHistoryScreen(
                            runs = allRuns,
                            onViewRunDetail = { runId -> viewModel.viewRunDetail(runId) },
                            onDeleteRun = { runId -> viewModel.deleteRun(runId) }
                        )
                    }

                    AppDestination.LIVE_RUN -> {
                        LiveRunScreen(
                            telemetry = liveTelemetry,
                            onPause = { viewModel.pauseRun() },
                            onResume = { viewModel.resumeRun() },
                            onFinish = { viewModel.finishRun() },
                            onManualLap = { viewModel.recordManualLap() },
                            onToggleVoiceCoach = { viewModel.toggleVoiceCoach() }
                        )
                    }

                    AppDestination.RUN_SUMMARY -> {
                        summaryRun?.let { run ->
                            RunSummaryScreen(
                                run = run,
                                onSaveAndClose = { title, feeling, shoe, notes ->
                                    viewModel.saveSummaryAndClose(title, feeling, shoe, notes)
                                },
                                onClose = { viewModel.closeSummaryWithoutSaving() }
                            )
                        }
                    }

                    AppDestination.ANALYTICS -> {
                        PerformanceAnalyticsScreen(
                            metrics = performanceMetrics,
                            recentRuns = allRuns
                        )
                    }

                    AppDestination.PROFILE -> {
                        ProfileScreen(
                            userProfile = userProfile,
                            isSignedIn = isSignedIn,
                            onSignInWithGoogle = { viewModel.signInWithGoogle() },
                            onSignOut = { viewModel.signOut() },
                            onUpdateWeeklyGoal = { goal -> viewModel.updateWeeklyGoal(goal) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ApexBottomNavBar(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    onStartRunClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(SurfaceDark.copy(alpha = 0.95f))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(26.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(
                    icon = Icons.Default.Home,
                    label = "Beranda",
                    isSelected = currentDestination == AppDestination.HOME,
                    onClick = { onNavigate(AppDestination.HOME) },
                    testTag = "nav_home"
                )

                NavBarItem(
                    icon = Icons.Default.History,
                    label = "Riwayat",
                    isSelected = currentDestination == AppDestination.HISTORY,
                    onClick = { onNavigate(AppDestination.HISTORY) },
                    testTag = "nav_history"
                )

                // Central Floating Action Button for Instant Run
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(NeonLime)
                        .clickable { onStartRunClick() }
                        .testTag("nav_quick_run"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = "Quick Run",
                        tint = DarkObsidian,
                        modifier = Modifier.size(24.dp)
                    )
                }

                NavBarItem(
                    icon = Icons.Default.Insights,
                    label = "Analisis",
                    isSelected = currentDestination == AppDestination.ANALYTICS,
                    onClick = { onNavigate(AppDestination.ANALYTICS) },
                    testTag = "nav_analytics"
                )

                NavBarItem(
                    icon = Icons.Default.Person,
                    label = "Profil",
                    isSelected = currentDestination == AppDestination.PROFILE,
                    onClick = { onNavigate(AppDestination.PROFILE) },
                    testTag = "nav_profile"
                )
            }
        }
    }
}

@Composable
fun NavBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) NeonLime else TextMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) NeonLime else TextMuted,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 10.sp
        )
    }
}
