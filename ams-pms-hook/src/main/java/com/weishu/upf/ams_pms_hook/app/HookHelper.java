package com.weishu.upf.ams_pms_hook.app;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author weishu
 * @date 16/3/7
 */
public final class HookHelper {

    public static void hookActivityManager() {
        try {
            Class<?> activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");

            // 获取 gDefault 这个字段, 想办法替换它
            Field gDefaultField = activityManagerNativeClass.getDeclaredField("gDefault");
            gDefaultField.setAccessible(true);

            Object gDefault = gDefaultField.get(null);

            // 4.x以上的gDefault是一个 android.util.Singleton对象; 我们取出这个单例里面的字段
            Class<?> singleton = Class.forName("android.util.Singleton");
            Field mInstanceField = singleton.getDeclaredField("mInstance");
            mInstanceField.setAccessible(true);

            // ActivityManagerNative 的gDefault对象里面原始的 IActivityManager对象
            Object rawIActivityManager = mInstanceField.get(gDefault);

            // 创建一个这个对象的代理对象, 然后替换这个字段, 让我们的代理对象帮忙干活
            Class<?> iActivityManagerInterface = Class.forName("android.app.IActivityManager");
            Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class<?>[]{iActivityManagerInterface}, new HookHandler(rawIActivityManager));
            mInstanceField.set(gDefault, proxy);

        } catch (Exception e) {
            throw new RuntimeException("Hook Failed", e);
        }

    }

    public static void hookPackageManager(Context context) {
        try {
            // 获取全局的ActivityThread对象
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            Object currentActivityThread = currentActivityThreadMethod.invoke(null);

            // 获取ActivityThread里面原始的 sPackageManager
            Field sPackageManagerField = activityThreadClass.getDeclaredField("sPackageManager");
            sPackageManagerField.setAccessible(true);
            Object sPackageManager = sPackageManagerField.get(currentActivityThread);

            // 准备好代理对象, 用来替换原始的对象
            Class<?> iPackageManagerInterface = Class.forName("android.content.pm.IPackageManager");
            Object proxy = Proxy.newProxyInstance(iPackageManagerInterface.getClassLoader(),
                    new Class<?>[]{iPackageManagerInterface},
                    new HookHandler(sPackageManager));

            // 1. 替换掉ActivityThread里面的 sPackageManager 字段
            sPackageManagerField.set(currentActivityThread, proxy);

            // 2. 替换 ApplicationPackageManager里面的 mPm对象
            PackageManager pm = context.getPackageManager();
            Field mPmField = pm.getClass().getDeclaredField("mPM");
            mPmField.setAccessible(true);
            mPmField.set(pm, proxy);
        } catch (Exception e) {
            throw new RuntimeException("hook failed", e);
        }
    }


    public static void hookWindowManagerWms(Activity activity) {
        try {

//            Class<?>activityClazz = Class.forName("android.app.Activity");
//            Field field = activityClazz.getDeclaredField("mWindow");
//            field.setAccessible(true);
//            field.get(activity).getClass().getName();
//
//            Log.i("test","hookWindowManager  begin "+field.get(activity).getClass().getName());
            Log.i("test", "hookWindowManager  begin");
            Class<?> activityClass = Class.forName("com.android.internal.policy.PhoneWindow$WindowManagerHolder");
            Field windowMangerFiled = activityClass.getDeclaredField("sWindowManager");
            windowMangerFiled.setAccessible(true);
            Object windowManager = windowMangerFiled.get(null);


            Class<?> IWindowManager = Class.forName("android.view.IWindowManager");
            Object proxy = Proxy.newProxyInstance(IWindowManager.getClassLoader(), new Class[]{IWindowManager}, new HookHandler(windowManager));
            windowMangerFiled.set(null, proxy);
            Log.i("test", "hookWindowManager  end");
        } catch (Exception e) {
            Log.i("test", "hookWindowManager  exception == " + e.toString());
        }
    }

    public static void hookWindowManager(Activity activity) {
        try {

            Class<?> activityClazz = Class.forName("android.app.Activity");
            Field field = activityClazz.getDeclaredField("mWindowManager");
            field.setAccessible(true);
            WindowManager windowManagerMember = (WindowManager)field.get(activity);


            Class<?> windowManager = Class.forName("android.view.WindowManager");
            Class<?> viewManager = Class.forName("android.view.ViewManager");
            Object proxy = Proxy.newProxyInstance(windowManager.getClassLoader(), new Class[]{windowManager, viewManager},
                    new HookHandler(activity,windowManagerMember));
            field.set(activity, proxy);
            Log.i("test", "hookWindowManager  end");
        } catch (Exception e) {
            Log.i("test", "hookWindowManager  exception == " + e.toString());
        }
    }
}
