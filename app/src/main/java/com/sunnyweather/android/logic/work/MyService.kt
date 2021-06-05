package com.sunnyweather.android.logic.work

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.sunnyweather.android.LogUtil
import com.sunnyweather.android.MainActivity
import com.sunnyweather.android.R
import com.sunnyweather.android.SunnyWeatherApplication
import com.sunnyweather.android.logic.Repository
import com.sunnyweather.android.logic.model.RealtimeResponse
import com.sunnyweather.android.logic.model.getSky
import com.sunnyweather.android.logic.network.ServiceCreator
import com.sunnyweather.android.logic.network.WeatherService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import kotlin.concurrent.thread

class MyService : Service() {

    private fun getSavePlace() = Repository.getSavedPlace()

    //于google对电量做了优化，5S是最小轮询单位
    //5S以内不管怎么设置时间，都是5S执行一次,
    //凡是>5S的，都是可以在5S以上时间执行的

    private val anTime = 15*60*1000    //15min
//    private val anTime = 10000


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("my_service", "前台天气服务",
            NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, 0)
        val notification = NotificationCompat.Builder(this, "my_service")
            .setContentTitle("Weather")
            .setContentText("body")
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground))
            .setContentIntent(pi)
            .build()
        startForeground(1, notification)  //调用startForeground()方法后就会让MyService变成一个前台Service
    }

    override fun onDestroy() {
        super.onDestroy()
        val intentAlarm = Intent(this, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(this, 0, intentAlarm, 0)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pi)
        pi.cancel()

        val intent = Intent(this, MyService::class.java)
        stopService(intent)
    }

    @Suppress("UNREACHABLE_CODE")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        thread {
            Repository.refreshWeather(
                getSavePlace().location.lng,
                getSavePlace().location.lat
            )
            LogUtil.i("MyService", "onStartCommand lng: ${getSavePlace().location.lng}")
            LogUtil.i("MyService", "onStartCommand lat: ${getSavePlace().location.lat}")

            request()  //调用网络刷新，然后得到返回的json等数据
        }
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtTime: Long = SystemClock.elapsedRealtime() + anTime
        val intentAlarm = Intent(this, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(this, 0, intentAlarm, 0)
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi)
        return super.onStartCommand(intent, flags, startId)
        return START_STICKY
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
                        LogUtil.i("MyService", "onResponse response temperature is: $temp")
                        LogUtil.i("MyService", "onResponse response temperatureSky is: $sky")
                        LogUtil.i("MyService", "onResponse response temperatureSkyVal is: $skyBg")
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