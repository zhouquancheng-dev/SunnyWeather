package com.sunnyweather.android.logic.model

/**
 *  将Realtime和Daily对象封装起来
 */
data class Weather(val realtime: RealtimeResponse.Realtime,
                   val daily: DailyResponse.Daily)