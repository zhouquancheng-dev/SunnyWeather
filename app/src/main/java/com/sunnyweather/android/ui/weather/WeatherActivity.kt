package com.sunnyweather.android.ui.weather

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import com.sunnyweather.android.LogUtil
import com.sunnyweather.android.R
import com.sunnyweather.android.SunnyWeatherApplication
import com.sunnyweather.android.logic.Repository
import com.sunnyweather.android.logic.model.RealtimeResponse
import com.sunnyweather.android.logic.model.Weather
import com.sunnyweather.android.logic.model.getSky
import com.sunnyweather.android.logic.network.ServiceCreator
import com.sunnyweather.android.logic.network.WeatherService
import com.sunnyweather.android.logic.work.MyService
import kotlinx.android.synthetic.main.activity_weather.*
import kotlinx.android.synthetic.main.forecast.*
import kotlinx.android.synthetic.main.life_index.*
import kotlinx.android.synthetic.main.now.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class WeatherActivity : AppCompatActivity() {

    private fun getSavePlace() = Repository.getSavedPlace()

    val viewModel by lazy { ViewModelProvider(this).get(WeatherViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //实现沉浸式，透明状态栏
        if (Build.VERSION.SDK_INT >= 21) {
            val decorView = window.decorView
            decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = Color.TRANSPARENT
        }
        setContentView(R.layout.activity_weather)

        if (viewModel.locationLng.isEmpty()) {
            viewModel.locationLng = intent.getStringExtra("location_lng") ?: ""
        }
        if (viewModel.locationLat.isEmpty()) {
            viewModel.locationLat = intent.getStringExtra("location_lat") ?: ""
        }
        if (viewModel.placeName.isEmpty()) {
            viewModel.placeName = intent.getStringExtra("place_name") ?: ""
        }
        viewModel.weatherLiveData.observe(this, { result ->
            val weather = result.getOrNull()
            if (weather != null) {
                showWeatherInfo(weather)  //显示天气信息
                request()
            } else {
                Toast.makeText(this, "无法成功获取天气信息", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
            swipeRefresh.isRefreshing = false
        })
        swipeRefresh.setColorSchemeResources(R.color.design_default_color_primary)
        refreshWeather()
        swipeRefresh.setOnRefreshListener {
            refreshWeather()
            val intent1 = Intent(this, MyService::class.java)
            startService(intent1)
        }

        //让搜索地点页面从左边显示
        navBtn.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener{
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerOpened(drawerView: View) {
            }

            override fun onDrawerClosed(drawerView: View) {
                val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                manager.hideSoftInputFromWindow(drawerView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            }

            override fun onDrawerStateChanged(newState: Int) {
            }

        })
    }

    override fun onStop() {
        super.onStop()
        finish()
        LogUtil.v("WeatherActivity", "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.v("WeatherActivity", "onDestroy")
    }

    fun refreshWeather() {
        viewModel.refreshWeather(viewModel.locationLng, viewModel.locationLat)
        swipeRefresh.isRefreshing = true
    }

    private fun showWeatherInfo(weather: Weather) {
        placeName.text = viewModel.placeName
        val realtime = weather.realtime
        val daily = weather.daily
        //填充now.xml中的数据
        val currentTempText = "${realtime.temperature.toInt()}℃"
        currentTemp.text = currentTempText  //气温
        currentSky.text = getSky(realtime.skycon).info  //天气描述，晴，阴，多云……

        val currentPM25Text = "空气指数 ${realtime.airQuality.aqi.chn.toInt()}"
        currentAQI.text = currentPM25Text
        nowLayout.setBackgroundResource(getSky(realtime.skycon).bg)

        // 填充forecast.xml布局中的数据
        forecastLayout.removeAllViews()
        val days = daily.skycon.size
        for (i in 0 until days) {
            val skycon = daily.skycon[i]
            val temperature = daily.temperature[i]
            val view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false)

            val dateInfo = view .findViewById(R.id.dateInfo ) as TextView
            val skyIcon = view.findViewById(R.id.skyIcon) as ImageView
            val skyInfo = view.findViewById(R.id.skyInfo) as TextView
            val temperatureInfo = view.findViewById(R.id.temperatureInfo) as TextView
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateInfo.text = simpleDateFormat.format(skycon.date)
            val sky = getSky(skycon.value)
            skyIcon.setImageResource(sky.icon)
            skyInfo.text = sky.info

            val tempText = "${temperature.min.toInt()} ~ ${temperature.max.toInt()} °"
            temperatureInfo.text = tempText
            forecastLayout.addView(view)

            //填充life_index.xml布局中的数据
            val lifeIndex = daily.lifeIndex
            coldRiskText.text = lifeIndex.coldRisk[0].desc
            dressingText.text = lifeIndex.dressing[0].desc
            ultravioletText.text = lifeIndex.ultraviolet[0].desc
            carWashingText.text = lifeIndex.carWashing[0].desc
            weatherLayout.visibility = View.VISIBLE
        }
    }

    private fun request() {
        val weatherService = ServiceCreator.create<WeatherService>()
        weatherService.getRealtimeWeather(getSavePlace().location.lng, getSavePlace().location.lat)
            .enqueue(object : Callback<RealtimeResponse> {
                override fun onResponse(
                    call: Call<RealtimeResponse>,
                    response: Response<RealtimeResponse>
                ) {
                    val body = response.body()
                    if (body != null) {
                        val temp = body.result.realtime.temperature.toInt()
                        val sky = getSky(body.result.realtime.skycon).info
                        val skyBg = body.result.realtime.skycon
                        saveTemp(temp.toString())
                        saveSky(sky)
                        saveSkyIc(skyBg)
                    }
                }

                override fun onFailure(call: Call<RealtimeResponse>, t: Throwable) {
                    t.printStackTrace()
                }

            })
    }

    private fun saveTemp(body: String) {
        try {
            val output = SunnyWeatherApplication.context
                .openFileOutput("data_temp", Context.MODE_PRIVATE)
            val writer = BufferedWriter(OutputStreamWriter(output))
            writer.use {
                it.write(body)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun saveSky(body: String) {
        try {
            val output = SunnyWeatherApplication.context
                .openFileOutput("data_sky", Context.MODE_PRIVATE)
            val writer = BufferedWriter(OutputStreamWriter(output))
            writer.use {
                it.write(body)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun saveSkyIc(body: String) {
        try {
            val output = SunnyWeatherApplication.context
                .openFileOutput("data_skyIc", Context.MODE_PRIVATE)
            val writer = BufferedWriter(OutputStreamWriter(output))
            writer.use {
                it.write(body)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}