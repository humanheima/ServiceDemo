### IntentService 

8.0以后，优先使用JobIntentService

IntentService自动在工作线程执行任务，避免阻塞主线程。执行完毕任务以后自动停止，内部使用Handler的方式处理用户发送的请求

使用方式 实现onHandleIntent方法，逻辑在这里进行处理

```java
public class MyIntentService extends IntentService {
    private static final String TAG = "MyIntentService";

    public MyIntentService() {
        super("MyIntentService");
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: "+startId);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        //注释1处，处理具体逻辑
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "onHandleIntent: thread id="+Thread.currentThread().getId());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: MyIntentService");
    }
}
```

注释1处，在onHandleIntent方法中，处理具体逻辑，注意是在后台线程。

IntentService 源码分析

每次启动service的时候，都会调用onStartCommand方法。也就是说IntentService是可以处理多个任务的。

```java
@Override
public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
    onStart(intent, startId);
    return mRedelivery ? START_REDELIVER_INTENT : START_NOT_STICKY;
}
```

内部调用了onStart方法。

```java
@Override
public void onStart(@Nullable Intent intent, int startId) {
    Message msg = mServiceHandler.obtainMessage();
    msg.arg1 = startId;
    msg.obj = intent;
    //注释1处，通过ServiceHandler来发送消息到后台执行。
    mServiceHandler.sendMessage(msg);
}
```

注释1处，通过ServiceHandler来发送消息到后台执行。我们来看下ServiceHandler。


```java

private volatile Looper mServiceLooper;
private volatile ServiceHandler mServiceHandler;
private String mName;
private boolean mRedelivery;

//内部使用handler来处理用户的请求
private final class ServiceHandler extends Handler {

    public ServiceHandler(Looper looper) {
        super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
        //注释1处，抽象方法，处理逻辑，是在工作线程执行的
        onHandleIntent((Intent)msg.obj);
        //执行完毕停止服务
        stopSelf(msg.arg1);
    }
}
```

注释1处，抽象方法，处理逻辑，是在工作线程执行的。

mServiceHandler的初始化

```java
@Override
public void onCreate() {
    super.onCreate();
    //使用HandlerThread 方便的获取一个Looper
    HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
    //调用start方法以后，Looper调用loop()方法开始分发消息
    thread.start();

    mServiceLooper = thread.getLooper();
    mServiceHandler = new ServiceHandler(mServiceLooper);
}
```

Service创建的时候会调用onCreate方法创建一个具有Looper对象的后台线程HandlerThread，然后来初始化mServiceHandler。

关于stopSelf(int startId)方法，每次启动的时候都会有一个对应的startId，在onStartCommand方法回调里面有这个startId，
`onStartCommand(@Nullable Intent intent, int flags, int startId)`，stopSelf方法中会判断只有当startId，
是我们传入的最后一个startId的时候，才会真正停止。比如说我们快速startService6次，
生成的startId的分别是1,2,3,4,5,6那么，stopSelf(int startId)方法只有当参数为6的时候才会停止服务。当服务停止以后会回调
service的onDestroy方法，在这里mServiceLooper会退出，所以mServiceHandler的handleMessage也不会被调用了。
 
```java
@Override
public void onDestroy() {
    mServiceLooper.quit();
}
```
