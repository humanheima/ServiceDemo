package com.dmw.servicedemo.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.dmw.servicedemo.MainActivity;
import com.dmw.servicedemo.R;
import com.dmw.servicedemo.SecondActivity;

public class MyService extends Service {

    public static final String PRIMARY_CHANNEL_ID = "default";
    public static final String PRIMARY_CHANNEL_NAME = "com.brotherd.servicedemo.PrimaryMyChannelName";
    private static final String TAG = "MyService";
    private DownloadBinder binder = new DownloadBinder();
    private NotificationManager manager;

    private int index = 0;

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
        Log.e(TAG, "onCreate: current thread is：" + Thread.currentThread().getName());
    }

    private NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand: current thread is：" + Thread.currentThread().getName());
        Log.e(TAG, "onStartCommand: " + startId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel primaryChannel = new NotificationChannel(PRIMARY_CHANNEL_ID, PRIMARY_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            getManager().createNotificationChannel(primaryChannel);
        }
        Intent notificationIntent = new Intent(this, SecondActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, PRIMARY_CHANNEL_ID)
                .setContentTitle("title" + index++)
                .setContentText("content text" + index++)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pi)
                .build();
        startForeground(1, notification);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: current thread is: " + Thread.currentThread().getName());
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
