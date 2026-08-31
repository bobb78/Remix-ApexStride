package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.GPSPoint
import com.example.data.model.KmSplit
import com.example.data.model.RunActivity
import com.example.data.model.TrackLap

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey val id: String,
    val title: String,
    val activityType: String = "Lari",
    val timestamp: Long,
    val durationSeconds: Long,
    val distanceMeters: Double,
    val avgPaceSecondsPerKm: Int,
    val maxPaceSecondsPerKm: Int,
    val caloriesBurned: Int,
    val elevationGainMeters: Int,
    val avgCadenceSpm: Int,
    val avgHeartRateBpm: Int,
    val routePointsJson: String,
    val splitsJson: String,
    val lapsJson: String = "[]",
    val isSyncedToCloud: Boolean = false,
    val feelingTag: String = "🔥 Kuat",
    val shoeName: String = "Nike Alphafly 3",
    val weatherCondition: String = "Cerah 28°C",
    val notes: String = ""
) {
    fun toDomain(
        routePoints: List<GPSPoint>,
        splits: List<KmSplit>,
        laps: List<TrackLap> = emptyList()
    ): RunActivity {
        return RunActivity(
            id = id,
            title = title,
            activityType = activityType,
            timestamp = timestamp,
            durationSeconds = durationSeconds,
            distanceMeters = distanceMeters,
            avgPaceSecondsPerKm = avgPaceSecondsPerKm,
            maxPaceSecondsPerKm = maxPaceSecondsPerKm,
            caloriesBurned = caloriesBurned,
            elevationGainMeters = elevationGainMeters,
            avgCadenceSpm = avgCadenceSpm,
            avgHeartRateBpm = avgHeartRateBpm,
            routePoints = routePoints,
            splits = splits,
            laps = laps,
            isSyncedToCloud = isSyncedToCloud,
            feelingTag = feelingTag,
            shoeName = shoeName,
            weatherCondition = weatherCondition,
            notes = notes
        )
    }

    companion object {
        fun fromDomain(
            domain: RunActivity,
            routePointsJson: String,
            splitsJson: String,
            lapsJson: String = "[]"
        ): RunEntity {
            return RunEntity(
                id = domain.id,
                title = domain.title,
                activityType = domain.activityType,
                timestamp = domain.timestamp,
                durationSeconds = domain.durationSeconds,
                distanceMeters = domain.distanceMeters,
                avgPaceSecondsPerKm = domain.avgPaceSecondsPerKm,
                maxPaceSecondsPerKm = domain.maxPaceSecondsPerKm,
                caloriesBurned = domain.caloriesBurned,
                elevationGainMeters = domain.elevationGainMeters,
                avgCadenceSpm = domain.avgCadenceSpm,
                avgHeartRateBpm = domain.avgHeartRateBpm,
                routePointsJson = routePointsJson,
                splitsJson = splitsJson,
                lapsJson = lapsJson,
                isSyncedToCloud = domain.isSyncedToCloud,
                feelingTag = domain.feelingTag,
                shoeName = domain.shoeName,
                weatherCondition = domain.weatherCondition,
                notes = domain.notes
            )
        }
    }
}
