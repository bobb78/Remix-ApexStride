package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GPSPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speed: Float = 0f, // in m/s
    val timestamp: Long = System.currentTimeMillis(),
    val distanceFromStartMeters: Double = 0.0
)

data class KmSplit(
    val kmNumber: Int,
    val durationSeconds: Long,
    val paceSecondsPerKm: Int,
    val elevationGainMeters: Int = 0,
    val avgHeartRateBpm: Int = 145
) {
    val formattedPace: String
        get() = formatPace(paceSecondsPerKm)

    val formattedDuration: String
        get() = formatDuration(durationSeconds)
}

data class TrackLap(
    val lapNumber: Int,
    val durationSeconds: Long,
    val distanceMeters: Double = 400.0,
    val paceSecondsPerKm: Int,
    val avgHeartRateBpm: Int = 145,
    val avgCadenceSpm: Int = 168
) {
    val formattedDuration: String
        get() = formatDuration(durationSeconds)

    val formattedPace: String
        get() = formatPace(paceSecondsPerKm)
}

data class RunActivity(
    val id: String,
    val title: String,
    val activityType: String = "Lari", // "Lari", "Lari Track 400m", or "Jalan Kaki"
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long,
    val distanceMeters: Double,
    val avgPaceSecondsPerKm: Int,
    val maxPaceSecondsPerKm: Int = 0,
    val caloriesBurned: Int,
    val elevationGainMeters: Int = 0,
    val avgCadenceSpm: Int = 168,
    val avgHeartRateBpm: Int = 152,
    val routePoints: List<GPSPoint> = emptyList(),
    val splits: List<KmSplit> = emptyList(),
    val laps: List<TrackLap> = emptyList(),
    val isSyncedToCloud: Boolean = false,
    val feelingTag: String = "🔥 Kuat & Berenergi",
    val shoeName: String = "Nike Alphafly 3",
    val weatherCondition: String = "Cerah 28°C",
    val notes: String = ""
) {
    val distanceKm: Double
        get() = distanceMeters / 1000.0

    val completedLapsCount: Int
        get() = (distanceMeters / 400.0).toInt()

    val totalLaps400m: Double
        get() = distanceMeters / 400.0

    val formattedLapsCount: String
        get() = String.format(Locale.US, "%.1f", totalLaps400m)

    val formattedDistance: String
        get() = String.format(Locale.US, "%.2f", distanceKm)

    val formattedDuration: String
        get() = formatDuration(durationSeconds)

    val formattedPace: String
        get() = formatPace(avgPaceSecondsPerKm)

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("EEEE, dd MMM yyyy • HH:mm", Locale("id", "ID"))
            return sdf.format(Date(timestamp))
        }

    val speedKmh: Double
        get() = if (durationSeconds > 0) (distanceMeters / 1000.0) / (durationSeconds / 3600.0) else 0.0
}

data class UserProfile(
    val uid: String = "local_runner",
    val displayName: String = "Pelari",
    val email: String = "akun.lokal@perangkat",
    val photoUrl: String? = null,
    val levelTitle: String = "Pelari Pemula",
    val currentStreakDays: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val totalRunsCount: Int = 0,
    val totalDurationHours: Double = 0.0,
    val best5kSeconds: Long = 0,
    val best10kSeconds: Long = 0,
    val best21kSeconds: Long = 0,
    val weeklyGoalKm: Double = 20.0,
    val weeklyProgressKm: Double = 0.0,
    val favoriteShoe: String = "Sepatu Lari",
    val shoeMileageKm: Double = 0.0
)

data class PerformanceMetrics(
    val vo2MaxEstimate: Int = 0,
    val vo2MaxCategory: String = "Belum Ada Data",
    val paceConsistencyScore: Int = 0,
    val avgCadenceSpm: Int = 0,
    val groundContactTimeMs: Int = 0,
    val strideLengthMeters: Double = 0.0,
    val verticalOscillationCm: Double = 0.0,
    val cardiacDriftPercentage: Double = 0.0,
    val aerobicEfficiencyScore: Int = 0,
    val racePredictions: Map<String, String> = mapOf(
        "5K" to "--:--",
        "10K" to "--:--",
        "Half Marathon (21.1K)" to "--:--",
        "Full Marathon (42.2K)" to "--:--"
    ),
    val heartRateZonesDurationSeconds: Map<String, Long> = mapOf(
        "Zone 1 (Pemulihan Aktif)" to 0L,
        "Zone 2 (Pondasi Aerobik)" to 0L,
        "Zone 3 (Tempo)" to 0L,
        "Zone 4 (Ambang Laktat)" to 0L,
        "Zone 5 (Maksimal)" to 0L
    ),
    val weeklyVolumeKm: List<Pair<String, Double>> = listOf(
        "Sen" to 0.0,
        "Sel" to 0.0,
        "Rab" to 0.0,
        "Kam" to 0.0,
        "Jum" to 0.0,
        "Sab" to 0.0,
        "Min" to 0.0
    ),
    val recoveryTimeHours: Int = 0
)

fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun formatPace(paceSecondsPerKm: Int): String {
    if (paceSecondsPerKm <= 0 || paceSecondsPerKm > 3600) return "--'--\""
    val minutes = paceSecondsPerKm / 60
    val seconds = paceSecondsPerKm % 60
    return String.format(Locale.US, "%d'%02d\"", minutes, seconds)
}
