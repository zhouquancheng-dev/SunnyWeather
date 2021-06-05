package com.sunnyweather.android.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Parcelable
import android.util.Log
import com.sunnyweather.android.LogUtil

@Suppress("DEPRECATION")
class MyWidget : AppWidgetProvider() {

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        val i = Intent(context, WidgetService::class.java)
        context?.startService(i)
    }

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        LogUtil.i("MyWidget", "onUpdate: onUpdate运行了")
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        LogUtil.i("MyWidget", "onDeleted: onDeleted运行了")
    }

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        LogUtil.i("MyWidget", "onEnabled: onEnabled运行了")
        val i = Intent(context, WidgetService::class.java)
        context?.startService(i)
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        LogUtil.i("MyWidget", "onDisabled: onDisabled运行了")
        val i = Intent(context, WidgetService::class.java)
        context?.stopService(i)
    }
}