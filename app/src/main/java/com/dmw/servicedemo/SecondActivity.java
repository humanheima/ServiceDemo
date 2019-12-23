package com.dmw.servicedemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.dmw.servicedemo.service.MyService;

public class SecondActivity extends AppCompatActivity {

    private final String TAG=getClass().getName();

    public static void launch(Context context) {
        Intent intent = new Intent(context, SecondActivity.class);
        context.startActivity(intent);
    }

    private MyService.DownloadBinder binder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e(TAG, "onServiceConnected: ");
            binder = (MyService.DownloadBinder) service;
            binder.startDownload();
            binder.onDownloadProgress();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "onServiceDisconnected: ");
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.e(TAG, "onBindingDied: ");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
    }

    public void startService(View view) {
        Intent intent = new Intent(this, MyService.class);
        startService(intent);
    }

    public void stopService(View view) {
        Intent intent = new Intent(this, MyService.class);
        stopService(intent);
    }

    public void bindService(View view) {
        Intent intent = new Intent(this, MyService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    public void unbindService(View view) {
        unbindService(connection);
    }


}
