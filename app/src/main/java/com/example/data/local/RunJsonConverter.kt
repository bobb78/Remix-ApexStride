package com.example.data.local

import com.example.data.model.GPSPoint
import com.example.data.model.KmSplit
import com.example.data.model.TrackLap
import org.json.JSONArray
import org.json.JSONObject

object RunJsonConverter {
    fun routePointsToJson(points: List<GPSPoint>): String {
        val array = JSONArray()
        for (point in points) {
            val obj = JSONObject()
            obj.put("lat", point.latitude)
            obj.put("lng", point.longitude)
            obj.put("alt", point.altitude)
            obj.put("spd", point.speed.toDouble())
            obj.put("time", point.timestamp)
            obj.put("dist", point.distanceFromStartMeters)
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToRoutePoints(jsonStr: String?): List<GPSPoint> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        val list = mutableListOf<GPSPoint>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    GPSPoint(
                        latitude = obj.optDouble("lat", 0.0),
                        longitude = obj.optDouble("lng", 0.0),
                        altitude = obj.optDouble("alt", 0.0),
                        speed = obj.optDouble("spd", 0.0).toFloat(),
                        timestamp = obj.optLong("time", 0L),
                        distanceFromStartMeters = obj.optDouble("dist", 0.0)
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }

    fun splitsToJson(splits: List<KmSplit>): String {
        val array = JSONArray()
        for (s in splits) {
            val obj = JSONObject()
            obj.put("km", s.kmNumber)
            obj.put("dur", s.durationSeconds)
            obj.put("pace", s.paceSecondsPerKm)
            obj.put("elev", s.elevationGainMeters)
            obj.put("hr", s.avgHeartRateBpm)
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToSplits(jsonStr: String?): List<KmSplit> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        val list = mutableListOf<KmSplit>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    KmSplit(
                        kmNumber = obj.optInt("km", i + 1),
                        durationSeconds = obj.optLong("dur", 300L),
                        paceSecondsPerKm = obj.optInt("pace", 300),
                        elevationGainMeters = obj.optInt("elev", 0),
                        avgHeartRateBpm = obj.optInt("hr", 145)
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }

    fun lapsToJson(laps: List<TrackLap>): String {
        val array = JSONArray()
        for (lap in laps) {
            val obj = JSONObject()
            obj.put("num", lap.lapNumber)
            obj.put("dur", lap.durationSeconds)
            obj.put("dist", lap.distanceMeters)
            obj.put("pace", lap.paceSecondsPerKm)
            obj.put("hr", lap.avgHeartRateBpm)
            obj.put("cad", lap.avgCadenceSpm)
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToLaps(jsonStr: String?): List<TrackLap> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        val list = mutableListOf<TrackLap>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TrackLap(
                        lapNumber = obj.optInt("num", i + 1),
                        durationSeconds = obj.optLong("dur", 90L),
                        distanceMeters = obj.optDouble("dist", 400.0),
                        paceSecondsPerKm = obj.optInt("pace", 240),
                        avgHeartRateBpm = obj.optInt("hr", 145),
                        avgCadenceSpm = obj.optInt("cad", 168)
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }
}
