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
}
