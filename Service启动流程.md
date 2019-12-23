先来一张不专业的时序图
![Service启动流程.png](https://upload-images.jianshu.io/upload_images/3611193-6e097d2d2f6e64c9.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

**基于源码9.0**

比如我们在Activity中要启动Service，我们要调用Context的startService方法，这是一个抽象方法，ContextWrapper类继承了Context类实现了startService方法。
```
public abstract ComponentName startService(Intent service);
```
ContextWrapper的startService方法。
```
@Override
 public ComponentName startService(Intent service) {
      //调用mBase的startService方法
      return mBase.startService(service);
}
```
方法内部调用了mBase的startService方法。mBase的声明类型是Context，运行时类型是一个ContextImpl对象。可以参考[从点击桌面应用图标到MainActivity的onResume过程分析](https://www.jianshu.com/p/f938fd44f1d4)文章。

ContextImpl的startService方法

```
@Override
public ComponentName startService(Intent service) {
    //...
    //调用startServiceCommon方法
    return startServiceCommon(service, false, mUser);
}
```
ContextImpl的startServiceCommon方法。

```
private ComponentName startServiceCommon(Intent service, boolean requireForeground,
            UserHandle user) {
    try {
        validateServiceIntent(service);
        service.prepareToLeaveProcess(this);
        //调用ActivityManagerService的startService方法
        ComponentName cn = ActivityManager.getService().startService(
            mMainThread.getApplicationThread(), service, service.resolveTypeIfNeeded(
                        getContentResolver()), requireForeground,
                        getOpPackageName(), user.getIdentifier());
        //...
        return cn;
    } catch (RemoteException e) {
        throw e.rethrowFromSystemServer();
    }
}

```
ContextImpl的startService方法内部调用了ActivityManagerService的startService方法来启动Service。

ActivityManagerService的startService方法
```
@Override
public ComponentName startService(IApplicationThread caller, Intent service,
            String resolvedType, boolean requireForeground, String callingPackage,
            int userId) throws TransactionTooLargeException {
    //...
    synchronized(this) {
        final int callingPid = Binder.getCallingPid();
        final int callingUid = Binder.getCallingUid();
        final long origId = Binder.clearCallingIdentity();
        ComponentName res;
        try {
            //注释1. 调用ActiveServices的startServiceLocked方法
            res = mServices.startServiceLocked(caller, service,
                    resolvedType, callingPid, callingUid,
                    requireForeground, callingPackage, userId);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
        return res;
    }
}
```
在注释1处，调用了  mServices的startServiceLocked方法。 mServices的类型是ActiveServices。

ActiveServices的startServiceLocked方法
```
ComponentName startServiceLocked(IApplicationThread caller, Intent service, 
String resolvedType, int callingPid, int callingUid, boolean fgRequired, 
String callingPackage, final int userId) throws TransactionTooLargeException {
     //...
     //注释2
     ServiceLookupResult res =retrieveServiceLocked(service, resolvedType, callingPackage,
                    callingPid, callingUid, userId, true, callerFg, false, false);
   //...
   //注释3
   ComponentName cmp = startServiceInnerLocked(smap, service, r, callerFg, addToStarting);
        return cmp;
}
```
在注释2处，调用retrieveServiceLocked来查找是否有目标service对应的ServiceRecord，查找时先在本地的ServiceMap中查询，如果没有找到，就会调用PackageManagerService去获取目标service对应的信息，并封装到ServiceRecord中，最后将ServiceRecord封装为ServiceLookupResult返回。

继续往下走注释3处，调用了ActiveServices的startServiceInnerLocked方法 

ActiveServices的startServiceInnerLocked方法 
```
ComponentName startServiceInnerLocked(ServiceMap smap, Intent service, ServiceRecord r,
          boolean callerFg, boolean addToStarting) throws TransactionTooLargeException {
    ServiceState stracker = r.getTracker();
    //...
    //注释4，调用bringUpServiceLocked方法
    String error = bringUpServiceLocked(r, service.getFlags(), callerFg, false, false);
    if (error != null) {
        return new ComponentName("!!", error);
    }
  //...
  return r.name;
}
```
ActiveServices的bringUpServiceLocked方法 
```
private String bringUpServiceLocked(ServiceRecord r, int intentFlags, boolean execInFg,
            boolean whileRestarting, boolean permissionsReviewRequired)
            throws TransactionTooLargeException {
        //...
        ProcessRecord app;

        if (!isolated) {
            app = mAm.getProcessRecordLocked(procName, r.appInfo.uid, false);
            //注释4，如果要启动的Service所在的进程存在
            if (app != null && app.thread != null) {
                try {
                    app.addPackage(r.appInfo.packageName, r.appInfo.longVersionCode, mAm.mProcessStats);
                    //注释5，调用realStartServiceLocked方法
                    realStartServiceLocked(r, app, execInFg);
                    return null;
                } catch (TransactionTooLargeException e) {
                    throw e;
                } catch (RemoteException e) {
                    Slog.w(TAG, "Exception when starting service " + r.shortName, e);
                }
            }
        }
        //...
        //注释6，如果要启动的Service所在的进程不存在，先启动进程
        if (app == null && !permissionsReviewRequired) {
            //注释7
            if ((app=mAm.startProcessLocked(procName, r.appInfo, true, intentFlags,
                    hostingType, r.name, false, isolated, false)) == null) {
               //...
            }
          //...
        }
        //...

}
```
在注释4处，如果要启动的Service所在的进程存在，那么直接调用realStartServiceLocked方法启动目标service。如果要启动的Service所在的进程不存在，那么在注释7处先调用ActivityManagerService的startProcessLocked启动进程。这里我们暂时只看启动的Service所在的进程存在的情况。

ActiveServices的realStartServiceLocked方法 
```
 private final void realStartServiceLocked(ServiceRecord r,
            ProcessRecord app, boolean execInFg) throws RemoteException {
   //...
  //注释8处，调用ApplicationThread的scheduleCreateService方法
   app.thread.scheduleCreateService(r, r.serviceInfo,
                 mAm.compatibilityInfoForPackageLocked(r.serviceInfo.applicationInfo),
                    app.repProcState);
   //...
   //注释9，
   sendServiceArgsLocked(r, execInFg, true);

}
```
在realStartServiceLocked方法中，首先在注释8处，调用ApplicationThread的scheduleCreateService方法。

ApplicationThread的scheduleCreateService方法
```
 public final void scheduleCreateService(IBinder token,
                ServiceInfo info, CompatibilityInfo compatInfo, int processState) {
    updateProcessState(processState, false);
    CreateServiceData s = new CreateServiceData();
    s.token = token;
    s.info = info;
    s.compatInfo = compatInfo;
        
    sendMessage(H.CREATE_SERVICE, s);
}
```
scheduleCreateService方法最终还是调用了`mH.sendMessage(msg)`来发送消息，这个过程就是handler发送消息和处理消息的过程，不再详细叙述，我们直接看mH的handleMessage方法即可。（这里插一句，ApplicationThread是ActivityThread的内部类，mH是ActivityThread的一个成员变量）
```
public void handleMessage(Message msg) {
    switch (msg.what) { 
         case CREATE_SERVICE:
         handleCreateService((CreateServiceData)msg.obj);
         break;
    }
 }
```
handleMessage方法内部调用了ActivityThread的handleCreateService方法

ActivityThread的handleCreateService方法
```
private void handleCreateService(CreateServiceData data) {
    //...
    LoadedApk packageInfo = getPackageInfoNoCheck(
            data.info.applicationInfo, data.compatInfo);
    Service service = null;
    try {
        //构建Service对象
        java.lang.ClassLoader cl = packageInfo.getClassLoader();
        service = packageInfo.getAppFactory()
                .instantiateService(cl, data.info.name, data.intent);
    } catch (Exception e) {
        //...
    }

    try {
        //创建service的上下文环境
        ContextImpl context = ContextImpl.createAppContext(this, packageInfo);
        context.setOuterContext(service);

        Application app = packageInfo.makeApplication(false, mInstrumentation);
        //service初始化
        service.attach(context, this, data.info.name, data.token, app,
                ActivityManager.getService());
        //调用service的onCreate方法
        service.onCreate();
        mServices.put(data.token, service);
       //...
    } 
}

```
在这个方法中创建了service的实例，创建service的上下文环境，调用service的attach方法来初始化service。并调用了service的onCreate方法了，到这里service已经启动了，那么Service的onStartCommond方法是在哪里调用的呢？

让我们回到ActiveServices的realStartServiceLocked方法中的注释9处
```
private final void realStartServiceLocked(ServiceRecord r,
            ProcessRecord app, boolean execInFg) throws RemoteException {
   //...
  //注释8处，调用ApplicationThread的scheduleCreateService方法
   app.thread.scheduleCreateService(r, r.serviceInfo,
                 mAm.compatibilityInfoForPackageLocked(r.serviceInfo.applicationInfo),
                    app.repProcState);
   //...
   //注释9处
   sendServiceArgsLocked(r, execInFg, true);

}

```
在调用完ApplicationThread的scheduleCreateService方法以后，在注释9处调用了sendServiceArgsLocked方法。

ActiveServices的sendServiceArgsLocked方法
```
private final void sendServiceArgsLocked(ServiceRecord r, boolean execInFg,
            boolean oomAdjusted) throws TransactionTooLargeException {
     //...
    //注释10处，调用ApplicationThread的scheduleServiceArgs方法
    r.app.thread.scheduleServiceArgs(r, slice);
    //...
}
```
在注释10处，调用了ApplicationThread的scheduleServiceArgs方法

ApplicationThread的scheduleServiceArgs方法 
```
public final void scheduleServiceArgs(IBinder token, ParceledListSlice args) {
    List<ServiceStartArgs> list = args.getList
    for (int i = 0; i < list.size(); i++) {
        ServiceStartArgs ssa = list.get(i);
        ServiceArgsData s = new ServiceArgsData();
        s.token = token;
        s.taskRemoved = ssa.taskRemoved;
        s.startId = ssa.startId;
        s.flags = ssa.flags;
        s.args = ssa.args;

        sendMessage(H.SERVICE_ARGS, s);
    }
}
```
该方法内部也是调用`mH.sendMessage(msg)`来发送消息,接下来直接看mH的handleMessage方法即可。
```
public void handleMessage(Message msg) {
    switch (msg.what) { 
        case SERVICE_ARGS:
            //调用ActivityThread的handleServiceArgs方法
            handleServiceArgs((ServiceArgsData)msg.obj);
            break;
    }
}
```
ActivityThread的handleServiceArgs方法
```
 private void handleServiceArgs(ServiceArgsData data) {
     Service s = mServices.get(data.token);
     if (s != null) {
         try {
             if (data.args != null) {
                 data.args.setExtrasClassLoader(s.getClassLoader());
                 data.args.prepareToEnterProcess();
             }
             int res;
             if (!data.taskRemoved) {
                 //调用Service的onStartCommand方法
                 res = s.onStartCommand(data.args, data.flags, data.startId);
             }
         }
     }
}
```
在这里调用了Service的onStartCommand方法。

结尾：整篇文章看下来感觉需要叙述的地方不多，直接一路看代码就行了，哈哈。

参考
* 《Android进阶解密》
* [startService的Service启动过程分析](https://www.jianshu.com/p/fd31917c518a)
* [Android深入四大组件（二）Service的启动过程](http://liuwangshu.cn/framework/component/2-service-start.html)


