package com.brotherd.servicedemo

import android.app.Application
import android.content.Context
import android.util.Log
import com.brotherd.servicedemo.service.plugin.HookHelper
import java.lang.Exception

/**
 * Created by dumingwei on 2019/2/24
 * Desc:
 */
class App : Application() {

    private val TAG = javaClass.simpleName

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        try {
            HookHelper.hookAMS()
        } catch (e: Exception) {
            Log.e(TAG, "attachBaseContext: ${e.message}")
        }
    }
}