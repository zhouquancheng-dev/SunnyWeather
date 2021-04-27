package com.sunnyweather.android.logic.model

import android.location.Location
import com.google.gson.annotations.SerializedName

/**
 * 获取搜索全球城市地名接口所返回的JSON数据格式 的 数据模型
 */
data class PlaceResponse(val status: String, val places: List<Place>)

data class Place(val name: String, val location: Location,
                 @SerializedName("formatted_address") val address: String)

data class Location(val lng: String, val lat: String)