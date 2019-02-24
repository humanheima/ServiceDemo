package com.brotherd.servicedemo.service.plugin

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class TargetService : Service() {


    private val TAG = javaClass.simpleName

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: hello")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand: ")
        return super.onStartCommand(intent, flags, startId)

    }

}
