package com.example.data.remote
 
import com.example.data.local.RunJsonConverter
import com.example.data.model.RunActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreSyncManager {
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            null
        }
    }

    suspend fun syncRunToFirestore(userId: String, run: RunActivity): Result<Boolean> = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext Result.success(true)
        try {
            val runMap = hashMapOf(
                "id" to run.id,
                "userId" to userId,
                "title" to run.title,
                "activityType" to run.activityType,
                "timestamp" to run.timestamp,
                "durationSeconds" to run.durationSeconds,
                "distanceMeters" to run.distanceMeters,
                "avgPaceSecondsPerKm" to run.avgPaceSecondsPerKm,
                "caloriesBurned" to run.caloriesBurned,
                "elevationGainMeters" to run.elevationGainMeters,
                "avgCadenceSpm" to run.avgCadenceSpm,
                "avgHeartRateBpm" to run.avgHeartRateBpm,
                "routePointsJson" to RunJsonConverter.routePointsToJson(run.routePoints),
                "splitsJson" to RunJsonConverter.splitsToJson(run.splits),
                "feelingTag" to run.feelingTag,
                "shoeName" to run.shoeName,
                "notes" to run.notes,
                "weatherCondition" to run.weatherCondition
            )

            // Save in user private subcollection
            fs.collection("users")
                .document(userId)
                .collection("runs")
                .document(run.id)
                .set(runMap)
                .await()

            Result.success(true)
        } catch (e: Throwable) {
            // Local persistence remains intact in Room
            Result.failure(e)
        }
    }

    suspend fun fetchUserRunsFromFirestore(userId: String): Result<List<RunActivity>> = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext Result.success(emptyList())
        try {
            val snapshot = fs.collection("users")
                .document(userId)
                .collection("runs")
                .orderBy("timestamp")
                .get()
                .await()

            val runs = snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val title = doc.getString("title") ?: "Sesi Lari"
                    val activityType = doc.getString("activityType") ?: "Lari"
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val durationSeconds = doc.getLong("durationSeconds") ?: 0L
                    val distanceMeters = doc.getDouble("distanceMeters") ?: 0.0
                    val avgPaceSecondsPerKm = doc.getLong("avgPaceSecondsPerKm")?.toInt() ?: 0
                    val caloriesBurned = doc.getLong("caloriesBurned")?.toInt() ?: 0
                    val elevationGainMeters = doc.getLong("elevationGainMeters")?.toInt() ?: 0
                    val avgCadenceSpm = doc.getLong("avgCadenceSpm")?.toInt() ?: 0
                    val avgHeartRateBpm = doc.getLong("avgHeartRateBpm")?.toInt() ?: 0
                    val routePointsJson = doc.getString("routePointsJson") ?: "[]"
                    val splitsJson = doc.getString("splitsJson") ?: "[]"
                    val lapsJson = doc.getString("lapsJson") ?: "[]"
                    val feelingTag = doc.getString("feelingTag") ?: ""
                    val shoeName = doc.getString("shoeName") ?: ""
                    val notes = doc.getString("notes") ?: ""
                    val weatherCondition = doc.getString("weatherCondition") ?: ""

                    RunActivity(
                        id = id,
                        title = title,
                        activityType = activityType,
                        timestamp = timestamp,
                        durationSeconds = durationSeconds,
                        distanceMeters = distanceMeters,
                        avgPaceSecondsPerKm = avgPaceSecondsPerKm,
                        caloriesBurned = caloriesBurned,
                        elevationGainMeters = elevationGainMeters,
                        avgCadenceSpm = avgCadenceSpm,
                        avgHeartRateBpm = avgHeartRateBpm,
                        routePoints = RunJsonConverter.jsonToRoutePoints(routePointsJson),
                        splits = RunJsonConverter.jsonToSplits(splitsJson),
                        laps = RunJsonConverter.jsonToLaps(lapsJson),
                        feelingTag = feelingTag,
                        shoeName = shoeName,
                        notes = notes,
                        weatherCondition = weatherCondition
                    )
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(runs)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}
