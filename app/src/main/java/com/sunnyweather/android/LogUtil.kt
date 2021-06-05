package com.sunnyweather.android

import android.util.Log

/**
 * 定制的日志工具
 * 自由地控制日志的打印，当程序处于开发阶段时就让日志打印出来，当程序上线之后就把日志屏蔽掉
 * 在开发阶段将level指定成VERBOSE，当项目正式上线的时候将level指定成ERROR就可以了
 */
object LogUtil {
    private const val VERBOSE = 1
    private const val DEBUG = 2
    private const val INFO = 3
    private const val WARN = 4
    private const val ERROR = 5

    private var level = VERBOSE   //在开发阶段将level指定成VERBOSE，
    // 当项目正式上线的时候将level指定成ERROR就可以了

    /**
     * Log Verbose
     */
    fun v(tag: String, msg: String) {
        if (level <= VERBOSE) {
            Log.v(tag, msg)
        }
    }
    /**
     * Log Debug
     */
    fun d(tag: String, msg: String) {
        if (level <= DEBUG) {
            Log.d(tag, msg)
        }
    }

    /**
     * Log Info
     */
    fun i(tag: String, msg: String) {
        if (level <= INFO) {
            Log.i(tag, msg)
        }
    }

    /**
     * Log Warn
     */
    fun w(tag: String, msg: String) {
        if (level <= WARN) {
            Log.w(tag, msg)
        }
    }

    /**
     * Log Error
     */
    fun e(tag: String, msg: String) {
        if (level <= ERROR) {
            Log.e(tag, msg)
        }
    }
}