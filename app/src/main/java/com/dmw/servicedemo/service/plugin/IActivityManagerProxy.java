package com.dmw.servicedemo.service.plugin;

import android.content.Intent;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class IActivityManagerProxy implements InvocationHandler {
    private Object mActivityManager;
    private static final String TAG = "IActivityManagerProxy";

    public IActivityManagerProxy(Object activityManager) {
        this.mActivityManager = activityManager;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] args) throws Throwable {
        if ("startService".equals(method.getName())) {
            Intent intent = null;
            int index = 0;
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Intent) {
                    index = i;
                    break;
                }
            }
            intent = (Intent) args[index];
            Intent proxyIntent = new Intent();
            String packageName = "com.dmw.servicedemo";
            //注意类的全路径，别写错了
            proxyIntent.setClassName(packageName, packageName + ".service.plugin.ProxyService");
            proxyIntent.putExtra(ProxyService.TARGET_SERVICE, intent.getComponent().getClassName());
            //将启动的intent替换为proxyIntent
            args[index] = proxyIntent;
            Log.d(TAG, "Hook成功");
        }
        return method.invoke(mActivityManager, args);
    }
}