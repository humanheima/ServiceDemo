package com.dmw.servicedemo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import com.dmw.servicedemo.service.MyIntentService;
import com.dmw.servicedemo.service.MyService;
import com.dmw.servicedemo.service.plugin.TargetService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private MyService.DownloadBinder binder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e(TAG, "onServiceConnected: current thread is：" + Thread.currentThread().getName());
            binder = (MyService.DownloadBinder) service;
            binder.startDownload();
            binder.onDownloadProgress();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "onServiceDisconnected: current thread is：" + Thread.currentThread().getName());
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.e(TAG, "onBindingDied: ");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void testIntentService(View view) {
        Intent intent = new Intent(this, MyIntentService.class);
        startService(intent);
        /**
         * 即使在子线程启动Service，启动后的 Service 也是运行在主线程的。
         * new Thread(new Runnable() {
         *             @Override
         *             public void run() {
         *                 Intent intent = new Intent(MainActivity.this, MyService.class);
         *                 startService(intent);
         *             }
         *         }).start();
         */
    }

    public void startService(View view) {
        Intent intent = new Intent(this, MyService.class);
        startService(intent);
    }

    int index = 10086;

    public void stopService(View view) {
//        Intent intent = new Intent(this, MyService.class);
//        stopService(intent);

        Intent notificationIntent = new Intent(this, SecondActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, MyService.PRIMARY_CHANNEL_ID)
                .setContentTitle("title" + index++)
                .setContentText("content text" + index++)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pi)
                .build();

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, notification);
    }

    public void bindService(View view) {
        Intent intent = new Intent(this, MyService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    public void unbindService(View view) {
        unbindService(connection);
    }

    public void launchSecond(View view) {
        SecondActivity.launch(this);
    }

    public void testPluginService(View view) {
        Intent intent = new Intent(MainActivity.this, TargetService.class);
        startService(intent);
    }
}
