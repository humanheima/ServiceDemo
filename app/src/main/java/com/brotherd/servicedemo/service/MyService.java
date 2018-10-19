package com.brotherd.servicedemo.service;

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
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.brotherd.servicedemo.MainActivity;
import com.brotherd.servicedemo.R;

public class MyService extends Service {

    public static final String PRIMARY_CHANNEL_ID = "default";
    public static final String PRIMARY_CHANNEL_NAME = "com.brotherd.servicedemo.PrimaryMyChannelName";
    private static final String TAG = "MyService";
    private DownloadBinder binder = new DownloadBinder();
    private NotificationManager manager;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel primaryChannel = new NotificationChannel(PRIMARY_CHANNEL_ID, PRIMARY_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            getManager().createNotificationChannel(primaryChannel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new NotificationCompat.Builder(this, PRIMARY_CHANNEL_ID)
                .setContentTitle("title")
                .setContentText("content text")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pi)
                .build();
        startForeground(1, notification);
    }

    private NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand: " + startId);
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
