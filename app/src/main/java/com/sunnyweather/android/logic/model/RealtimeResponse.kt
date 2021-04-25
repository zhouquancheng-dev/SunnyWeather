package com.sunnyweather.android.logic.model

import com.google.gson.annotations.SerializedName

/**
 * 获取实时天气信息接口所返回的JSON数据格式 的 数据模型
 */
data class RealtimeResponse(val status: String, val  result: Result) {

    data class Result(val realtime: Realtime)
    
    data class Realtime(val skycon: String, val temperature: Float,
                        @SerializedName("air_quality") val airQuality: AirQuality)

    data class AirQuality(val aqi: AQI)

    data class AQI(val chn: Float)
}

