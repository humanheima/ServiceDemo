先来一张不专业的流程图
![Service的绑定流程.png](https://upload-images.jianshu.io/upload_images/3611193-f9558591921479a3.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

基于源码9.0

我们通常绑定服务的流程是这样的

1. 声明一个Binder,在Service的onBind方法中，返回一个Binder对象用来执行操作。
2. 当我们成功绑定服务以后，就可以在ServiceConnection的onServiceConnected方法中获取Binder对象，然后就可以调用Binder的方法了。

要绑定Service，我们要调用Context的bindService方法，这是一个抽象方法，ContextWrapper类继承了Context类实现了bindService方法。

Context的bindService方法
```
 public abstract boolean bindService(Intent service,ServiceConnection conn,  int flags);
```
ContextWrapper的bindService方法
```
@Override
public boolean bindService(Intent service, ServiceConnection conn,int flags) {
    return mBase.bindService(service, conn, flags);
}
```
我们知道mBase是一个ContextImpl对象，我们看一下ContextImpl的bindService方法
```
@Override
public boolean bindService(Intent service, ServiceConnection conn,int flags) {
   warnIfCallingFromSystemProcess();
    return bindServiceCommon(service, conn, flags, mMainThread.getHandler(), getUser()); 
}
```
内部调用了bindServiceCommon方法
```
private boolean bindServiceCommon(Intent service, ServiceConnection conn, int flags, Handler
            handler, UserHandle user) {
       
    IServiceConnection sd;
    //...
    //注释1处
    sd = mPackageInfo.getServiceDispatcher(conn, getOuterContext(), handler, flags);
    
    try {
         //...
         //注释2处 
        int res = ActivityManager.getService().bindService(
            mMainThread.getApplicationThread(), getActivityToken(), service,
            service.resolveTypeIfNeeded(getContentResolver()),
            sd, flags, getOpPackageName(), user.getIdentifier());
        if (res < 0) {
            throw new SecurityException(
                    "Not allowed to bind to service " + service);
        }
        return res != 0;
    }
}

```
在注释1处，调用了LoadedApk类型的对象mPackageInfo的getServiceDispatcher方法，它的主要作用是把ServiceConnection类型对象conn封装为IServiceConnection类型的对象sd。IServiceConnection实现了Binder机制，这样Service的绑定过程就支持跨进程了。

在注释2处，调用了ActivityManagerService的bindService方法。

ActivityManagerService的bindService方法。
```
public int bindService(IApplicationThread caller, IBinder token, Intent service,
            String resolvedType, IServiceConnection connection, int flags, String callingPackage,
            int userId) throws TransactionTooLargeException {
    //...
    synchronized(this) {
        //注释3处
        return mServices.bindServiceLocked(caller, token, service,
                resolvedType, connection, flags, callingPackage, userId);
    }
}
```
在注释3处，调用了ActiveServices类型对象mServices的bindServiceLocked方法

ActiveServices的bindServiceLocked方法
```
 int bindServiceLocked(IApplicationThread caller, IBinder token, Intent service,
            String resolvedType, final IServiceConnection connection, int flags,
            String callingPackage, final int userId) throws TransactionTooLargeException {

    //...
    //注释4
     ServiceLookupResult res =
            retrieveServiceLocked(service, resolvedType, callingPackage, Binder.getCallingPid(),
                    Binder.getCallingUid(), userId, true, callerFg, isBindExternal, allowInstant);

     ServiceRecord s = res.record;

    //...
    //注释5
    AppBindRecord b = s.retrieveAppBindingLocked(service, callerApp);

    //...
    if ((flags&Context.BIND_AUTO_CREATE) != 0) {
                s.lastActivity = SystemClock.uptimeMillis();
                //注释6
                if (bringUpServiceLocked(s, service.getFlags(), callerFg, false,
                        permissionsReviewRequired) != null) {
                    return 0;
                }
    //...
     if (s.app != null && b.intent.received) {//注释7
                // 服务已在运行，因此我们可以立即发布连接。
                try {
                    //注释8
                    c.conn.connected(s.name, b.intent.binder, false);
                } 
               //...
            } else if (!b.intent.requested) {//注释9
                requestServiceBindingLocked(s, b.intent, callerFg, false);
            }
    //...
    return 1;
       
}
```
在注释4处，调用retrieveServiceLocked来查找是否有目标service对应的ServiceRecord，查找时先在本地的ServiceMap中查询，如果没有找到，就会调用PackageManagerService去获取目标service对应的信息，并封装到ServiceRecord中，最后将ServiceRecord封装为ServiceLookupResult返回。

在注释5处，调用了ServiceRecord类型对象s的retrieveAppBindingLocked方法来获取一个AppBindRecord对象。这里交代一下几个类的作用。

* AppBindRecord 用于描述一个service和service的一个客户端应用之间的联系。
* ProcessRecord 用于描述一个正在运行的特定应用进程的全部信息。
* ServiceRecord 用于描述一个运行的应用service。
* IntentBindRecord 用于描述一个特定的已经和一个service绑定的Intent。

我们看一下ServiceRecord类的retrieveAppBindingLocked方法

```
public AppBindRecord retrieveAppBindingLocked(Intent intent,
            ProcessRecord app) {
    Intent.FilterComparison filter = new Intent.FilterComparison(intent);
    IntentBindRecord i = bindings.get(filter);
    if (i == null) {
        //构建一个IntentBindRecord对象
        i = new IntentBindRecord(this, filter);
        bindings.put(filter, i);
    }
    AppBindRecord a = i.apps.get(app);
    if (a != null) {
        return a;
    }
    //构建一个AppBindRecord对象
    a = new AppBindRecord(this, i, app);
    i.apps.put(app, a);
    return a;
}
```

在注释6处，调用了bringUpServiceLocked方法，在这个方法内部会启动Service，在[Service启动流程](https://www.jianshu.com/p/58dfeafa544a)这篇文章中已经分析过了。 
在注释7处，s.app!=null,表示Service已经在运行；b.intent.received表示当前应用程序进程已经接收到了绑定Service时返回的Binder了。这时候我们肯定是没有接收到接收到了绑定Service时返回的Binder的。所以不会运行注释8处的代码。

在注释9处如果没有发送过绑定Service的请求，那么!b.intent.requested会为true。
那么就会调用requestServiceBindingLocked方法。

```
 private final boolean requestServiceBindingLocked(ServiceRecord r, IntentBindRecord i,
            boolean execInFg, boolean rebind) throws TransactionTooLargeException {
    //...
    r.app.thread.scheduleBindService(r, i.intent.getIntent(), rebind,
                        r.app.repProcState);
}
```
方法内部调用了ApplicationThread的scheduleBindService方法，我们来看一下。

```
public final void scheduleBindService(IBinder token, Intent intent,
                boolean rebind, int processState) {
    updateProcessState(processState, false);
    BindServiceData s = new BindServiceData();
    s.token = token;
    s.intent = intent;
    s.rebind = rebind;
             
    sendMessage(H.BIND_SERVICE, s);
}
```
方法内部还是使用mH发送消息，然后mH的handleMessage方法中是调用了ActivityThread的handleBindService方法
```
 private void handleBindService(BindServiceData data) {
        Service s = mServices.get(data.token);
      
     //调用Service的onBind方法，获取返回的IBinder对象。
     IBinder binder = s.onBind(data.intent);
     //调用ActivityManagerService的publishService方法
     ActivityManager.getService().publishService(data.token, data.intent, binder);
}
```
方法内部首先调用了Service的onBind方法，获取返回的IBinder对象。然后调用ActivityManagerService的publishService方法。

ActivityManagerService的publishService方法

```
public void publishService(IBinder token, Intent intent, IBinder service) {
  //...
  synchronized(this) {
       //调用了ActiveServices的publishServiceLocked方法
       mServices.publishServiceLocked((ServiceRecord)token, intent, service);
    }
}
```
ActiveServices的publishServiceLocked方法
```
void publishServiceLocked(ServiceRecord r, Intent intent, IBinder service) {
    //...
    c.conn.connected(r.name, service, false);
}
```
方法内部调用了IServiceConnection类型对象的connected对象。IServiceConnection的实现类是InnerConnection。

InnerConnection的connected方法
```
public void connected(ComponentName name, IBinder service, boolean dead)
                    throws RemoteException {
    LoadedApk.ServiceDispatcher sd = mDispatcher.get();
    if (sd != null) {
        //调用了ServiceDispatcher的connected方法
        sd.connected(name, service, dead);
    }
 }
```

ServiceDispatcher的connected方法
```
public void connected(ComponentName name, IBinder service, boolean dead) {
    //...
    mActivityThread.post(new RunConnection(name, service, 0, dead));
          
}
```
mActivityThread是一个Handler对象，我们调用post方法以后，最终会执行RunConnection的run方法。
```
private final class RunConnection implements Runnable {
    //...

    public void run() {
        if (mCommand == 0) {
            //调用了ServiceDispatcher的connected方法
            doConnected(mName, mService, mDead);
        } 
    }
}
```
ServiceDispatcher的connected方法
```

 public void doConnected(ComponentName name, IBinder service, boolean dead) {
   //
   if (service != null) {
            //调用了ServiceConnection的onServiceConnected
            mConnection.onServiceConnected(name, service);
   } 
}
```
在方法内部调用了ServiceConnection的onServiceConnected，完成了Service的绑定流程。

参考：
* 《Android进阶解密》


