package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PerformanceMetrics
import com.example.data.model.RunActivity
import com.example.data.model.UserProfile
import com.example.data.remote.FirebaseAuthManager
import com.example.data.repository.RunRepository
import com.example.service.LiveRunTelemetry
import com.example.service.LiveRunTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class AppDestination {
    HOME,
    HISTORY,
    LIVE_RUN,
    RUN_SUMMARY,
    ANALYTICS,
    PROFILE
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val runRepository = RunRepository(application)
    private val authManager = FirebaseAuthManager(application)
    private val liveRunTracker = LiveRunTracker(application)

    // Navigation state
    private val _currentDestination = MutableStateFlow(AppDestination.HOME)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    // Runs list from Room database
    val allRuns: StateFlow<List<RunActivity>> = runRepository.allRunsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Performance Metrics derived dynamically from all runs
    val performanceMetrics: StateFlow<PerformanceMetrics> = allRuns.map { runs ->
        runRepository.computePerformanceMetrics(runs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PerformanceMetrics())

    // User profile combined with real recorded runs
    val userProfile: StateFlow<UserProfile> = combine(
        authManager.currentUserProfile,
        allRuns
    ) { baseProfile, runs ->
        val totalDistKm = runs.sumOf { it.distanceKm }
        val totalSecs = runs.sumOf { it.durationSeconds }
        val totalHrs = totalSecs / 3600.0
        val runsCount = runs.size

        // Calculate weekly progress (past 7 days)
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        val weeklyKm = runs.filter { it.timestamp >= sevenDaysAgo }.sumOf { it.distanceKm }

        // Find personal records
        val b5k = runs.filter { it.distanceKm >= 5.0 }.minOfOrNull { it.durationSeconds } ?: 0L
        val b10k = runs.filter { it.distanceKm >= 10.0 }.minOfOrNull { it.durationSeconds } ?: 0L
        val b21k = runs.filter { it.distanceKm >= 21.0 }.minOfOrNull { it.durationSeconds } ?: 0L

        val streak = if (runs.isEmpty()) 0 else {
            // Number of distinct days active
            runs.map { it.timestamp / (24 * 60 * 60 * 1000) }.distinct().size
        }

        val shoeMileage = runs.filter { it.shoeName == baseProfile.favoriteShoe }.sumOf { it.distanceKm }

        baseProfile.copy(
            totalDistanceKm = (totalDistKm * 10).roundToInt() / 10.0,
            totalRunsCount = runsCount,
            totalDurationHours = (totalHrs * 10).roundToInt() / 10.0,
            weeklyProgressKm = (weeklyKm * 10).roundToInt() / 10.0,
            currentStreakDays = streak,
            best5kSeconds = b5k,
            best10kSeconds = b10k,
            best21kSeconds = b21k,
            shoeMileageKm = (shoeMileage * 10).roundToInt() / 10.0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val isSignedIn: StateFlow<Boolean> = authManager.isSignedIn

    // Live Run Telemetry
    val liveTelemetry: StateFlow<LiveRunTelemetry> = liveRunTracker.telemetry

    // Current viewed / completed run for summary screen
    private val _summaryRun = MutableStateFlow<RunActivity?>(null)
    val summaryRun: StateFlow<RunActivity?> = _summaryRun.asStateFlow()

    fun navigateTo(destination: AppDestination) {
        _currentDestination.value = destination
    }

    fun startRun(isSimulation: Boolean = false, activityType: String = "Lari", targetLaps: Int = 0) {
        liveRunTracker.startRun(isSimulation = isSimulation, activityType = activityType, targetLaps = targetLaps)
        _currentDestination.value = AppDestination.LIVE_RUN
    }

    fun pauseRun() {
        liveRunTracker.pauseRun()
    }

    fun resumeRun() {
        liveRunTracker.resumeRun()
    }

    fun recordManualLap() {
        liveRunTracker.recordManualLap()
    }

    fun toggleVoiceCoach() {
        liveRunTracker.toggleVoiceCoach()
    }

    fun finishRun() {
        val completedRun = liveRunTracker.finishRun()
        _summaryRun.value = completedRun
        _currentDestination.value = AppDestination.RUN_SUMMARY
    }

    fun viewRunDetail(runId: String) {
        viewModelScope.launch {
            val run = runRepository.getRunById(runId)
            if (run != null) {
                _summaryRun.value = run
                _currentDestination.value = AppDestination.RUN_SUMMARY
            }
        }
    }

    fun saveSummaryAndClose(title: String, feelingTag: String, shoeName: String, notes: String = "") {
        val current = _summaryRun.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                title = title,
                feelingTag = feelingTag,
                shoeName = shoeName,
                notes = notes
            )
            runRepository.saveRun(updated, userProfile.value.uid)
            _summaryRun.value = null
            _currentDestination.value = AppDestination.HOME
        }
    }

    fun closeSummaryWithoutSaving() {
        val current = _summaryRun.value
        if (current != null) {
            viewModelScope.launch {
                runRepository.saveRun(current, userProfile.value.uid)
            }
        }
        _summaryRun.value = null
        _currentDestination.value = AppDestination.HOME
    }

    fun deleteRun(runId: String) {
        viewModelScope.launch {
            runRepository.deleteRun(runId)
            if (_summaryRun.value?.id == runId) {
                _summaryRun.value = null
                _currentDestination.value = AppDestination.HOME
            }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            authManager.signInWithGoogle()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }

    fun updateWeeklyGoal(newGoal: Double) {
        authManager.updateWeeklyGoal(newGoal)
    }

    override fun onCleared() {
        super.onCleared()
        liveRunTracker.release()
    }
}
