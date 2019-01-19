package com.weishu.upf.dynamic_proxy_hook.app2.hook;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Instrumentation;
import android.util.Log;

import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author weishu
 * @date 16/1/28
 */
public class HookHelper {

    public static void attachContext() throws Exception {
        // 先获取到当前的ActivityThread对象
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
        currentActivityThreadMethod.setAccessible(true);
        Object currentActivityThread = currentActivityThreadMethod.invoke(null);

        // 拿到原始的 mInstrumentation字段
        Field mInstrumentationField = activityThreadClass.getDeclaredField("mInstrumentation");
        mInstrumentationField.setAccessible(true);
        Instrumentation mInstrumentation = (Instrumentation) mInstrumentationField.get(currentActivityThread);

        // 创建代理对象
        Instrumentation evilInstrumentation = new EvilInstrumentation(mInstrumentation);
        ActivityManager activityManager
                ;
        // 偷梁换柱
        mInstrumentationField.set(currentActivityThread, evilInstrumentation);
    }


    public static void attachActivity(Activity  activity) throws Exception {
        Log.i("EvilInstrumentation",activity.toString());
        Class activityClass = Class.forName("android.app.Activity");
        Field field = activityClass.getDeclaredField("mInstrumentation");

        field.setAccessible(true);
        Instrumentation instrumentation =  (Instrumentation) field.get(activity);
        Log.i("EvilInstrumentation",instrumentation.toString());
        // 创建代理对象
        Instrumentation evilInstrumentation = new EvilInstrumentation(instrumentation);
        field.set(activity,evilInstrumentation);
    }
}
