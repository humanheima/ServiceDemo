package com.brotherd.servicedemo.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.brotherd.servicedemo.MainActivity;
import com.brotherd.servicedemo.R;

public class MyService extends Service {

    private static final String TAG = "MyService";
    public static final String MY_SERVICE_CHANNEL = "MyServiceChannel";
    private DownloadBinder binder = new DownloadBinder();

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind: ");
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate: ");
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new NotificationCompat.Builder(this, MY_SERVICE_CHANNEL)
                .setContentTitle("title")
                .setContentText("content text")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentIntent(pi)
                .build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand: " + this.toString());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: ");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind: " + this.toString());
        return super.onUnbind(intent);
    }

    public class DownloadBinder extends Binder {

        public void startDownload() {
            Log.e(TAG, "startDownload: ");
        }

        public int onDownloadProgress() {
            Log.e(TAG, "onDownloadProgress: ");
            return 0;
        }
    }
}
