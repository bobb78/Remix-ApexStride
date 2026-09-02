package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.speech.tts.TextToSpeech
import com.example.data.model.GPSPoint
import com.example.data.model.KmSplit
import com.example.data.model.RunActivity
import com.example.data.model.TrackLap
import com.example.data.model.formatPace
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

enum class RunTrackingState {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

data class LiveRunTelemetry(
    val state: RunTrackingState = RunTrackingState.IDLE,
    val activityType: String = "Lari", // "Lari", "Lari Track 100m", or "Jalan Kaki"
    val elapsedSeconds: Long = 0L,
    val activeMovingSeconds: Long = 0L,
    val distanceMeters: Double = 0.0,
    val currentPaceSecondsPerKm: Int = 0,
    val avgPaceSecondsPerKm: Int = 0,
    val currentSpeedKmh: Double = 0.0,
    val caloriesBurned: Int = 0,
    val currentCadenceSpm: Int = 0,
    val estimatedHeartRateBpm: Int = 0,
    val elevationGainMeters: Int = 0,
    val isActivelyMoving: Boolean = false,
    val movementStatusText: String = "Menunggu Gerakan",
    val gpsAccuracyMeters: Float = 0.0f,
    val points: List<GPSPoint> = emptyList(),
    val splits: List<KmSplit> = emptyList(),
    val laps: List<TrackLap> = emptyList(),
    val completedLaps: Int = 0,
    val currentLapMeters: Double = 0.0,
    val currentLapDurationSeconds: Long = 0L,
    val targetLaps: Int = 0,
    val isSimulationMode: Boolean = true,
    val isVoiceCoachEnabled: Boolean = false,
    val currentSplitTimeSeconds: Long = 0L
) {
    val distanceKm: Double
        get() = distanceMeters / 1000.0

    val currentLapProgressPercent: Float
        get() = (currentLapMeters / 100.0).coerceIn(0.0, 1.0).toFloat()

    val currentLapNumber: Int
        get() = completedLaps + 1

    val remainingLapMeters: Double
        get() = (100.0 - currentLapMeters).coerceAtLeast(0.0)

    val totalLapsCalculated: Double
        get() = distanceMeters / 100.0

    val formattedLapMeters: String
        get() = "${currentLapMeters.toInt()}m / 100m"
}

class LiveRunTracker(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null
    private var simulationJob: Job? = null

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _telemetry = MutableStateFlow(LiveRunTelemetry())
    val telemetry: StateFlow<LiveRunTelemetry> = _telemetry.asStateFlow()

    private var lastRecordedLocation: Location? = null
    private var lastSplitDistanceMeters = 0.0
    private var splitStartSeconds = 0L

    // 400m Track Lap tracking variables
    private var lastLapDistanceMeters = 0.0
    private var lapStartSeconds = 0L

    // Simulation track variables
    private var simAngle = 0.0
    private val simCenterLat = -6.2185
    private val simCenterLng = 106.8025

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        val result = tts?.setLanguage(Locale("id", "ID"))
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            tts?.setLanguage(Locale.US)
                        }
                        isTtsReady = true
                    } catch (e: Throwable) {
                        isTtsReady = false
                    }
                }
            }
        } catch (e: Throwable) {
            isTtsReady = false
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (_telemetry.value.state != RunTrackingState.RUNNING) return
            val loc = result.lastLocation ?: return
            handleNewLocation(loc)
        }
    }

    fun startRun(isSimulation: Boolean = false, activityType: String = "Lari", targetLaps: Int = 0) {
        val isTrack = activityType.contains("Track", ignoreCase = true)
        _telemetry.value = LiveRunTelemetry(
            state = RunTrackingState.RUNNING,
            activityType = activityType,
            targetLaps = targetLaps,
            isSimulationMode = isSimulation,
            isActivelyMoving = isSimulation,
            movementStatusText = if (isSimulation) "Simulasi $activityType Aktif" else "Mencari Sinyal GPS...",
            estimatedHeartRateBpm = if (activityType == "Jalan Kaki") 105 else 138,
            currentCadenceSpm = if (activityType == "Jalan Kaki") 110 else 168
        )
        lastRecordedLocation = null
        lastSplitDistanceMeters = 0.0
        splitStartSeconds = 0L
        lastLapDistanceMeters = 0.0
        lapStartSeconds = 0L
        simAngle = 0.0

        val welcomeMsg = if (isTrack) {
            if (targetLaps > 0) {
                "Sesi Lari Track 100 meter dimulai! Target $targetLaps putaran."
            } else {
                "Sesi Lari Track 100 meter dimulai! 1 putaran sama dengan 100 meter. Selamat berlari!"
            }
        } else {
            "Sesi $activityType dimulai! Selamat berolahraga."
        }
        speakVoiceCue(welcomeMsg)

        startTimer()

        if (isSimulation) {
            startSimulationLoop(activityType)
        } else {
            startGpsTracking()
        }
    }

    fun pauseRun() {
        if (_telemetry.value.state == RunTrackingState.RUNNING) {
            _telemetry.update { it.copy(state = RunTrackingState.PAUSED) }
            speakVoiceCue("Sesi latihan dijeda.")
        }
    }

    fun resumeRun() {
        if (_telemetry.value.state == RunTrackingState.PAUSED) {
            _telemetry.update { it.copy(state = RunTrackingState.RUNNING) }
            speakVoiceCue("Sesi latihan dilanjutkan!")
        }
    }

    fun toggleVoiceCoach() {
        _telemetry.update { it.copy(isVoiceCoachEnabled = !it.isVoiceCoachEnabled) }
    }

    fun recordManualLap() {
        if (_telemetry.value.state != RunTrackingState.RUNNING) return
        val current = _telemetry.value
        val lapDist = (current.distanceMeters - lastLapDistanceMeters).coerceAtLeast(10.0)
        val lapDur = (current.elapsedSeconds - lapStartSeconds).coerceAtLeast(1L)
        val lapPace = if (lapDist > 0) ((lapDur / (lapDist / 1000.0)).toInt()).coerceIn(60, 1800) else current.avgPaceSecondsPerKm
        val lapNum = current.laps.size + 1

        val newLap = TrackLap(
            lapNumber = lapNum,
            durationSeconds = lapDur,
            distanceMeters = lapDist,
            paceSecondsPerKm = lapPace,
            avgHeartRateBpm = current.estimatedHeartRateBpm,
            avgCadenceSpm = current.currentCadenceSpm
        )
        val updatedLaps = current.laps + newLap
        lastLapDistanceMeters = current.distanceMeters
        lapStartSeconds = current.elapsedSeconds

        _telemetry.update {
            it.copy(
                laps = updatedLaps,
                completedLaps = updatedLaps.size,
                currentLapMeters = 0.0,
                currentLapDurationSeconds = 0L
            )
        }
        speakLapAnnouncement(lapNum, lapDur, lapPace)
    }

    fun finishRun(): RunActivity {
        stopTracking()
        val current = _telemetry.value
        _telemetry.update { it.copy(state = RunTrackingState.FINISHED) }

        val finalAvgPace = if (current.distanceKm > 0.05) {
            (current.elapsedSeconds / current.distanceKm).toInt()
        } else {
            0
        }

        // Add remaining partial split if any (> 200m)
        val finalSplits = current.splits.toMutableList()
        val remainingDist = current.distanceMeters - lastSplitDistanceMeters
        if (remainingDist > 200) {
            val partialDur = current.elapsedSeconds - splitStartSeconds
            val partialPace = if (remainingDist > 0) ((partialDur / (remainingDist / 1000.0)).toInt()) else finalAvgPace
            finalSplits.add(
                KmSplit(
                    kmNumber = finalSplits.size + 1,
                    durationSeconds = partialDur,
                    paceSecondsPerKm = partialPace,
                    elevationGainMeters = 2,
                    avgHeartRateBpm = current.estimatedHeartRateBpm
                )
            )
        }

        // Add remaining partial lap if any (> 20m)
        val finalLaps = current.laps.toMutableList()
        val remainingLapDist = current.distanceMeters - lastLapDistanceMeters
        if (remainingLapDist > 20.0) {
            val partialLapDur = current.elapsedSeconds - lapStartSeconds
            val partialLapPace = if (remainingLapDist > 0) ((partialLapDur / (remainingLapDist / 1000.0)).toInt()) else finalAvgPace
            finalLaps.add(
                TrackLap(
                    lapNumber = finalLaps.size + 1,
                    durationSeconds = partialLapDur,
                    distanceMeters = remainingLapDist,
                    paceSecondsPerKm = partialLapPace,
                    avgHeartRateBpm = current.estimatedHeartRateBpm,
                    avgCadenceSpm = current.currentCadenceSpm
                )
            )
        }

        val totalLapsCount = (current.distanceMeters / 100.0)
        val lapsSpeech = if (totalLapsCount >= 1.0) "Total ${String.format(Locale.US, "%.1f", totalLapsCount)} putaran seratus meter." else ""
        speakVoiceCue("Sesi latihan selesai! Total jarak ${String.format(Locale.US, "%.2f", current.distanceKm)} kilometer. $lapsSpeech")

        val title = when (current.activityType) {
            "Lari Track 100m", "Lari Track 150m", "Lari Track 400m" -> "Sesi Lari Track 100m (${String.format(Locale.US, "%.1f", totalLapsCount)} Putaran)"
            "Jalan Kaki" -> "Sesi Jalan Kaki Outdoor"
            else -> "Sesi Lari Outdoor"
        }

        return RunActivity(
            id = UUID.randomUUID().toString(),
            title = title,
            activityType = current.activityType,
            timestamp = System.currentTimeMillis(),
            durationSeconds = current.elapsedSeconds,
            distanceMeters = current.distanceMeters,
            avgPaceSecondsPerKm = finalAvgPace,
            maxPaceSecondsPerKm = if (finalAvgPace > 30) finalAvgPace - 30 else finalAvgPace,
            caloriesBurned = current.caloriesBurned,
            elevationGainMeters = current.elevationGainMeters,
            avgCadenceSpm = if (current.currentCadenceSpm > 0) current.currentCadenceSpm else if (current.activityType == "Jalan Kaki") 115 else 172,
            avgHeartRateBpm = if (current.estimatedHeartRateBpm > 0) current.estimatedHeartRateBpm else if (current.activityType == "Jalan Kaki") 110 else 152,
            routePoints = current.points,
            splits = finalSplits,
            laps = finalLaps,
            feelingTag = "🚀 Kuat & Bertenaga",
            shoeName = "Nike Alphafly 3",
            weatherCondition = "Cerah 28°C"
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                if (_telemetry.value.state == RunTrackingState.RUNNING) {
                    _telemetry.update { prev ->
                        val newSeconds = prev.elapsedSeconds + 1
                        val newMovingSeconds = if (prev.isActivelyMoving || prev.isSimulationMode) {
                            prev.activeMovingSeconds + 1
                        } else {
                            prev.activeMovingSeconds
                        }
                        val splitSec = newSeconds - splitStartSeconds
                        val lapSec = newSeconds - lapStartSeconds
                        val calPerKm = if (prev.activityType == "Jalan Kaki") 45 else 68
                        val cal = (prev.distanceKm * calPerKm).toInt()

                        val isRunningType = prev.activityType != "Jalan Kaki"
                        val baseHr = if (isRunningType) 138 else 102
                        val hr = if (prev.isActivelyMoving || prev.isSimulationMode) {
                            (baseHr + (newSeconds % 25) / 2).coerceIn(80, 185).toInt()
                        } else {
                            (85 + (newSeconds % 10)).coerceIn(70, 100).toInt()
                        }

                        val cadence = if (prev.isActivelyMoving || prev.isSimulationMode) {
                            if (isRunningType) prev.currentCadenceSpm.coerceIn(150, 195) else prev.currentCadenceSpm.coerceIn(90, 135)
                        } else {
                            0
                        }

                        val avgPace = if (prev.distanceKm > 0.02 && newMovingSeconds > 0) {
                            (newMovingSeconds / prev.distanceKm).toInt()
                        } else {
                            0
                        }

                        prev.copy(
                            elapsedSeconds = newSeconds,
                            activeMovingSeconds = newMovingSeconds,
                            currentSplitTimeSeconds = splitSec,
                            currentLapDurationSeconds = lapSec,
                            caloriesBurned = cal,
                            estimatedHeartRateBpm = hr,
                            currentCadenceSpm = cadence,
                            avgPaceSecondsPerKm = avgPace
                        )
                    }
                }
            }
        }
    }

    private fun startSimulationLoop(activityType: String) {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            val earthRadius = 6371000.0
            val isTrack = activityType.contains("Track", ignoreCase = true)
            // Radius ~15.91m gives exactly 2*pi*r = 100m perimeter for track!
            val radiusMeters = if (isTrack) 15.91 else 350.0
            val isRunning = activityType != "Jalan Kaki"
            val angularSpeed = if (isTrack) 0.11 else if (isRunning) 0.045 else 0.018
            val baseSpeedMs = if (isRunning) 3.5f else 1.35f

            while (isActive) {
                delay(1000L)
                if (_telemetry.value.state == RunTrackingState.RUNNING) {
                    simAngle += angularSpeed
                    val xOffset = radiusMeters * 1.3 * Math.cos(simAngle)
                    val yOffset = radiusMeters * 0.9 * Math.sin(simAngle)

                    val latOffset = (yOffset / earthRadius) * (180.0 / Math.PI)
                    val lngOffset = (xOffset / (earthRadius * Math.cos(Math.toRadians(simCenterLat)))) * (180.0 / Math.PI)

                    val lat = simCenterLat + latOffset
                    val lng = simCenterLng + lngOffset
                    val alt = 15.0 + Math.sin(simAngle) * 2.0

                    val simulatedSpeedMs = (baseSpeedMs + (Math.sin(simAngle * 4) * (if (isRunning) 0.3f else 0.1f))).toFloat()
                    val instPaceSec = if (simulatedSpeedMs > 0.3f) (1000f / simulatedSpeedMs).toInt() else 600

                    _telemetry.update { prev ->
                        val addedMeters = simulatedSpeedMs.toDouble()
                        val newDist = prev.distanceMeters + addedMeters
                        val newPoint = GPSPoint(
                            latitude = lat,
                            longitude = lng,
                            altitude = alt,
                            speed = simulatedSpeedMs,
                            timestamp = System.currentTimeMillis(),
                            distanceFromStartMeters = newDist
                        )
                        val updatedPoints = prev.points + newPoint

                        // 1. Check for KM split completion (every 1000m)
                        val splits = prev.splits.toMutableList()
                        if (newDist - lastSplitDistanceMeters >= 1000.0) {
                            val kmNum = splits.size + 1
                            val splitDur = prev.elapsedSeconds - splitStartSeconds
                            val splitPace = splitDur.toInt()
                            splits.add(
                                KmSplit(
                                    kmNumber = kmNum,
                                    durationSeconds = splitDur,
                                    paceSecondsPerKm = splitPace,
                                    elevationGainMeters = 3,
                                    avgHeartRateBpm = prev.estimatedHeartRateBpm
                                )
                            )
                            lastSplitDistanceMeters = newDist
                            splitStartSeconds = prev.elapsedSeconds

                            speakSplitAnnouncement(kmNum, splitPace)
                        }

                        // 2. Check for 100m Track Lap completion (every 100m)
                        val laps = prev.laps.toMutableList()
                        if (newDist - lastLapDistanceMeters >= 100.0) {
                            val lapNum = laps.size + 1
                            val lapDur = prev.elapsedSeconds - lapStartSeconds
                            val lapPace = if (lapDur > 0) ((lapDur / 0.10).toInt()) else prev.avgPaceSecondsPerKm
                            laps.add(
                                TrackLap(
                                    lapNumber = lapNum,
                                    durationSeconds = lapDur,
                                    distanceMeters = 100.0,
                                    paceSecondsPerKm = lapPace,
                                    avgHeartRateBpm = prev.estimatedHeartRateBpm,
                                    avgCadenceSpm = prev.currentCadenceSpm
                                )
                            )
                            lastLapDistanceMeters = newDist
                            lapStartSeconds = prev.elapsedSeconds

                            speakLapAnnouncement(lapNum, lapDur, lapPace)
                        }

                        val curLapMeters = (newDist - lastLapDistanceMeters).coerceAtLeast(0.0)
                        val curLapSec = prev.elapsedSeconds - lapStartSeconds

                        val cadenceVal = if (isRunning) {
                            (168 + (Math.sin(simAngle * 5) * 4)).toInt()
                        } else {
                            (112 + (Math.sin(simAngle * 3) * 3)).toInt()
                        }

                        val statusLabel = if (isTrack) "Lari Track 100m" else if (isRunning) "Berlari Aktif" else "Jalan Kaki Aktif"

                        prev.copy(
                            distanceMeters = newDist,
                            currentPaceSecondsPerKm = instPaceSec,
                            currentSpeedKmh = (simulatedSpeedMs * 3.6),
                            currentCadenceSpm = cadenceVal,
                            isActivelyMoving = true,
                            movementStatusText = "$statusLabel (${String.format(Locale.US, "%.1f", simulatedSpeedMs * 3.6)} km/jam)",
                            elevationGainMeters = (newDist / 250.0).toInt(),
                            points = updatedPoints,
                            splits = splits,
                            laps = laps,
                            completedLaps = laps.size,
                            currentLapMeters = curLapMeters,
                            currentLapDurationSeconds = curLapSec
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startGpsTracking() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(800L)
                .setMinUpdateDistanceMeters(1.0f)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            // Fallback to simulation if GPS permission fails
            startSimulationLoop(_telemetry.value.activityType)
        }
    }

    private fun handleNewLocation(loc: Location) {
        // 1. Filter out poor accuracy GPS readings to prevent jitter spikes
        if (loc.hasAccuracy() && loc.accuracy > 25.0f) {
            _telemetry.update {
                it.copy(
                    gpsAccuracyMeters = loc.accuracy,
                    movementStatusText = "Akurasi GPS (${loc.accuracy.toInt()}m) - Menunggu Sinyal Jernih"
                )
            }
            return
        }

        val prevLoc = lastRecordedLocation
        val deltaMeters = if (prevLoc != null) loc.distanceTo(prevLoc).toDouble() else 0.0
        val deltaTimeSec = if (prevLoc != null) (loc.time - prevLoc.time) / 1000.0 else 1.0

        // Calculate speed based on GPS hardware speed or computed distance over time
        val calculatedSpeedMs = if (deltaTimeSec > 0.3) (deltaMeters / deltaTimeSec).toFloat() else 0f
        val rawSpeedMs = if (loc.hasSpeed() && loc.speed > 0f) loc.speed else calculatedSpeedMs

        // Movement filter
        val isWalking = (deltaMeters >= 1.4 && rawSpeedMs >= 0.6f && rawSpeedMs < 1.8f)
        val isRunning = (deltaMeters >= 1.8 && rawSpeedMs >= 1.8f && rawSpeedMs <= 12.0f)
        val isTrueMovement = isWalking || isRunning

        if (isTrueMovement) {
            lastRecordedLocation = loc
            val instPaceSec = (1000f / rawSpeedMs).toInt()
            val dynamicCadence = if (isRunning) {
                (155 + (rawSpeedMs * 7.5f)).toInt().coerceIn(150, 195)
            } else {
                (95 + (rawSpeedMs * 18.0f)).toInt().coerceIn(90, 135)
            }

            val isTrack = _telemetry.value.activityType.contains("Track", ignoreCase = true)
            val statusText = if (isTrack) {
                "Lari Track 100m (${String.format(Locale.US, "%.1f", rawSpeedMs * 3.6)} km/jam)"
            } else if (isRunning) {
                "Berlari Aktif (${String.format(Locale.US, "%.1f", rawSpeedMs * 3.6)} km/jam)"
            } else {
                "Jalan Kaki (${String.format(Locale.US, "%.1f", rawSpeedMs * 3.6)} km/jam)"
            }

            _telemetry.update { prev ->
                val newDist = prev.distanceMeters + deltaMeters
                val newPoint = GPSPoint(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    altitude = loc.altitude,
                    speed = rawSpeedMs,
                    timestamp = System.currentTimeMillis(),
                    distanceFromStartMeters = newDist
                )
                val updatedPoints = prev.points + newPoint

                // 1. KM Split check (every 1000m)
                val splits = prev.splits.toMutableList()
                if (newDist - lastSplitDistanceMeters >= 1000.0) {
                    val kmNum = splits.size + 1
                    val splitDur = prev.elapsedSeconds - splitStartSeconds
                    splits.add(
                        KmSplit(
                            kmNumber = kmNum,
                            durationSeconds = splitDur,
                            paceSecondsPerKm = splitDur.toInt(),
                            elevationGainMeters = 2,
                            avgHeartRateBpm = prev.estimatedHeartRateBpm
                        )
                    )
                    lastSplitDistanceMeters = newDist
                    splitStartSeconds = prev.elapsedSeconds

                    speakSplitAnnouncement(kmNum, splitDur.toInt())
                }

                // 2. 100m Track Lap check (every 100m)
                val laps = prev.laps.toMutableList()
                if (newDist - lastLapDistanceMeters >= 100.0) {
                    val lapNum = laps.size + 1
                    val lapDur = prev.elapsedSeconds - lapStartSeconds
                    val lapPace = if (lapDur > 0) ((lapDur / 0.10).toInt()) else prev.avgPaceSecondsPerKm
                    laps.add(
                        TrackLap(
                            lapNumber = lapNum,
                            durationSeconds = lapDur,
                            distanceMeters = 100.0,
                            paceSecondsPerKm = lapPace,
                            avgHeartRateBpm = prev.estimatedHeartRateBpm,
                            avgCadenceSpm = dynamicCadence
                        )
                    )
                    lastLapDistanceMeters = newDist
                    lapStartSeconds = prev.elapsedSeconds

                    speakLapAnnouncement(lapNum, lapDur, lapPace)
                }

                val curLapMeters = (newDist - lastLapDistanceMeters).coerceAtLeast(0.0)
                val curLapSec = prev.elapsedSeconds - lapStartSeconds

                prev.copy(
                    distanceMeters = newDist,
                    currentPaceSecondsPerKm = instPaceSec,
                    currentSpeedKmh = (rawSpeedMs * 3.6),
                    currentCadenceSpm = dynamicCadence,
                    isActivelyMoving = true,
                    movementStatusText = statusText,
                    gpsAccuracyMeters = if (loc.hasAccuracy()) loc.accuracy else 5.0f,
                    points = updatedPoints,
                    splits = splits,
                    laps = laps,
                    completedLaps = laps.size,
                    currentLapMeters = curLapMeters,
                    currentLapDurationSeconds = curLapSec
                )
            }
        } else {
            // User is stationary or just moving hand/arm in place
            _telemetry.update { prev ->
                prev.copy(
                    currentPaceSecondsPerKm = 0,
                    currentSpeedKmh = 0.0,
                    currentCadenceSpm = 0,
                    isActivelyMoving = false,
                    movementStatusText = "Diam / Menunggu Langkah Nyata",
                    gpsAccuracyMeters = if (loc.hasAccuracy()) loc.accuracy else 5.0f
                )
            }
        }
    }

    private fun speakSplitAnnouncement(kmNumber: Int, paceSecondsPerKm: Int) {
        val paceText = formatPace(paceSecondsPerKm)
        val msg = "Kilometer $kmNumber selesai! Pace: $paceText per kilometer."
        speakVoiceCue(msg)
    }

    private fun speakLapAnnouncement(lapNumber: Int, durationSeconds: Long, paceSecondsPerKm: Int) {
        val min = durationSeconds / 60
        val sec = durationSeconds % 60
        val timeStr = if (min > 0) "$min menit $sec detik" else "$sec detik"
        val paceText = formatPace(paceSecondsPerKm)
        val msg = "Putaran $lapNumber (100 meter) selesai! Waktu putaran: $timeStr, pace: $paceText per kilometer."
        speakVoiceCue(msg)
    }

    private fun speakVoiceCue(message: String) {
        if (!_telemetry.value.isVoiceCoachEnabled) return
        try {
            if (isTtsReady) {
                tts?.speak(message, TextToSpeech.QUEUE_ADD, null, "apex_cue_${System.currentTimeMillis()}")
            }
        } catch (e: Throwable) {
            // Ignore
        }
    }

    private fun stopTracking() {
        timerJob?.cancel()
        timerJob = null
        simulationJob?.cancel()
        simulationJob = null
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun release() {
        stopTracking()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Throwable) {
            // Ignore
        }
    }
}
