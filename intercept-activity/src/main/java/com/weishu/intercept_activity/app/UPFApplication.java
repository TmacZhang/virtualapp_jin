package com.weishu.intercept_activity.app;

import android.app.Application;
import android.content.Context;

import com.weishu.intercept_activity.app.hook.AMSHookHelper;
import com.weishu.intercept_activity.app.hook.BaseDexClassLoaderHookHelper;
import com.weishu.intercept_activity.app.hook.ProviderHelper;

import java.io.File;

/**
 * 这个类只是为了方便获取全局Context的.
 *
 * @author weishu
 * @date 16/3/29
 */
public class UPFApplication extends Application {

    private static Context sContext;

    public static Context getContext() {
        return sContext;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sContext = base;

        try {
            AMSHookHelper.hookActivityManagerNative();
            AMSHookHelper.hookActivityThreadHandler();
            AMSHookHelper.hookPackageManager(base);
        } catch (Throwable throwable) {
            throw new RuntimeException("hook failed", throwable);
        }

        try {
            // 拦截startService, stopService等操作
            //AMSHookHelper.hookActivityManagerNative();


            Utils.extractAssets(base, "family_number.jar");
            File apkFile1 = getFileStreamPath("family_number.jar");
            File odexFile1 = getFileStreamPath("family_number.odex");

            // Hook ClassLoader, 让插件中的类能够被成功加载
            BaseDexClassLoaderHookHelper.patchClassLoader(getClassLoader(), apkFile1, odexFile1);
            ActivityManager.getInstance().preloadActivitys(apkFile1);

            ProviderHelper.installProviders(this, apkFile1);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
