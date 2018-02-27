package com.brotherd.servicedemo.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by dumingwei on 2017/5/21.
 */
public class MyIntentService extends IntentService {

    private static final String TAG = "MyIntentService";

    public MyIntentService() {
        super("MyIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        //处理具体逻辑
        Log.d(TAG, "onHandleIntent: thread id="+Thread.currentThread().getId());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: MyIntentService");
    }
}
