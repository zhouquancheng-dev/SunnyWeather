package com.sunnyweather.android.ui.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.*
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.IBinder
import android.os.SystemClock
import android.widget.RemoteViews
import com.sunnyweather.android.LogUtil
import com.sunnyweather.android.MainActivity
import com.sunnyweather.android.R
import com.sunnyweather.android.SunnyWeatherApplication
import java.io.*
import kotlin.concurrent.thread

@Suppress( "UNREACHABLE_CODE", "DEPRECATION")
class WidgetService : Service() {

    private val anTime = 5000
    private var count = 0

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        //Widget被删除时调用onDestroy()时停止发送alarm
        val intentAlarm = Intent(this, MyWidget::class.java)
        val pi = PendingIntent.getBroadcast(this, 0, intentAlarm, 0)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pi)
        pi.cancel()

        //Widget被删除时调用onDestroy()时停止服务
        val i = Intent(this, WidgetService::class.java)
        stopService(i)

        LogUtil.i("WidgetService", "onDestroy: WidgetService")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        thread {
            showWidgetRev()
        }
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtTime: Long = SystemClock.elapsedRealtime() + anTime
        val intentAlarm = Intent(this, MyWidget::class.java)
        val pi = PendingIntent.getBroadcast(this, 0, intentAlarm, 0)
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi)
        return super.onStartCommand(intent, flags, startId)
        return START_STICKY     //Service如果被kill了，系统会在合适的时候会自动重启Service
    }

    private fun isWifiConnect(): Boolean? {
        val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        return wifiInfo?.isConnected
    }

    private fun showWidgetRev() {
        val inputTextTemp = loadTemp()
        val inputTextSky = loadSky()
        val inputTextSkyIc = loadSkyIc()

        val manager = AppWidgetManager.getInstance(applicationContext)
        val componentName = ComponentName(applicationContext, MyWidget::class.java)
        val pendingIntent: PendingIntent = Intent(applicationContext,
            MainActivity::class.java).let { intent ->
            PendingIntent.getActivity(applicationContext, 0, intent, 0)
        }
        val rv = RemoteViews(packageName, R.layout.my_new_app_widget).apply {
            setOnClickPendingIntent(R.id.widget_layout, pendingIntent)
            if (inputTextTemp.isNotEmpty()) {
                setTextViewText(R.id.temperatureInfo_widget, "${inputTextTemp}℃"
                )
            }
            if (inputTextSky.isNotEmpty()) {
                setTextViewText(R.id.currentSky_widget, inputTextSky)
            }
            if (inputTextSkyIc.isNotEmpty()) {
                when (inputTextSkyIc) {
                    "CLEAR_DAY" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_clear_day) }
                    "CLEAR_NIGHT" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_clear_night) }
                    "PARTLY_CLOUDY_DAY" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_partly_cloud_day) }
                    "PARTLY_CLOUDY_NIGHT" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_partly_cloud_night) }
                    "CLOUDY" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_cloudy) }
                    "WIND" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_cloudy) }
                    "LIGHT_RAIN" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_light_rain) }
                    "MODERATE_RAIN" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_moderate_rain) }
                    "HEAVY_RAIN" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_heavy_rain) }
                    "STORM_RAIN" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_storm_rain) }
                    "THUNDER_SHOWER" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_thunder_shower) }
                    "SLEET" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_sleet) }
                    "LIGHT_SNOW" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_light_snow) }
                    "MODERATE_SNOW" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_moderate_snow) }
                    "HEAVY_SNOW" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_heavy_snow) }
                    "STORM_SNOW" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_storm_rain) }
                    "HAIL" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_hail) }
                    "LIGHT_HAZE" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_light_haze) }
                    "MODERATE_HAZE" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_moderate_haze) }
                    "HEAVY_HAZE" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_heavy_haze) }
                    "FOG" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_fog) }
                    "DUST" -> { setImageViewResource(R.id.skyIcon_widget, R.drawable.ic_fog) }
                }
            }
            if (isWifiConnect() == true) {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                val level = wifiInfo.rssi
                LogUtil.d("TAG", "wifi信号值(0 ~ -100) 当前为: $level")
                when {
                    level > -50 && level < 0 -> {
                        setImageViewResource(R.id.signalShow_widget, R.drawable.ic_wifi_4)
                        LogUtil.d("TAG", "当前WIFI信号: 最强")
                    }
                    level > -70 && level < -50 -> {
                        setImageViewResource(R.id.signalShow_widget, R.drawable.ic_wifi_3)
                        LogUtil.d("TAG", "当前WIFI信号: 较强")
                    }
                    level > -80 && level < -70 -> {
                        setImageViewResource(R.id.signalShow_widget, R.drawable.ic_wifi_2)
                        LogUtil.d("TAG", "当前WIFI信号: 较弱")
                    }
                    level > -100 && level < -80 -> {
                        setImageViewResource(R.id.signalShow_widget, R.drawable.ic_wifi_1)
                        LogUtil.d("TAG", "当前WIFI信号: 微弱")
                    }
                    else -> {
                        setImageViewResource(R.id.signalShow_widget, R.drawable.ic_wifi_off)
                        LogUtil.d("TAG", "当前无WIFI连接")
                    }
                }
            }
        }
        manager.updateAppWidget(componentName, rv)
        count++
        LogUtil.i("run", "run: $count")
        if (count >= 10) { count = 0 }
    }

    private fun loadTemp(): String {
        val content = StringBuilder()
        try {
            val input = SunnyWeatherApplication.context
                .openFileInput("data_temp")
            val reader = BufferedReader(InputStreamReader(input))
            reader.use {
                reader.forEachLine {
                    content.append(it)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return content.toString()
    }

    private fun loadSky(): String {
        val content = StringBuilder()
        try {
            val input = SunnyWeatherApplication.context
                .openFileInput("data_sky")
            val reader = BufferedReader(InputStreamReader(input))
            reader.use {
                reader.forEachLine {
                    content.append(it)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return content.toString()
    }

    private fun loadSkyIc(): String {
        val content = StringBuilder()
        try {
            val input = SunnyWeatherApplication.context
                .openFileInput("data_skyIc")
            val reader = BufferedReader(InputStreamReader(input))
            reader.use {
                reader.forEachLine {
                    content.append(it)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return content.toString()
    }

}