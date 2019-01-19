package com.weishu.upf.service_management.app;

import android.app.Application;
import android.content.Context;

import com.weishu.upf.service_management.app.hook.AMSHookHelper;
import com.weishu.upf.service_management.app.hook.BaseDexClassLoaderHookHelper;

import java.io.File;

/**
 * 这个类只是为了方便获取全局Context的.
 *
 * @author weishu
 * @date 16/3/29
 */
public class UPFApplication extends Application {

    private static Context sContext;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sContext = base;
        try {
            // 拦截startService, stopService等操作
            AMSHookHelper.hookActivityManagerNative();
            Utils.extractAssets(base, "test.jar");
            File apkFile = getFileStreamPath("test.jar");
            File odexFile = getFileStreamPath("test.odex");

            // Hook ClassLoader, 让插件中的类能够被成功加载
            BaseDexClassLoaderHookHelper.patchClassLoader(getClassLoader(), apkFile, odexFile);
            // 解析插件中的Service组件
            ServiceManager.getInstance().preLoadServices(apkFile);


            Utils.extractAssets(base, "family_number.jar");
            File apkFile1 = getFileStreamPath("family_number.jar");
            File odexFile1 = getFileStreamPath("family_number.odex");

            // Hook ClassLoader, 让插件中的类能够被成功加载
            BaseDexClassLoaderHookHelper.patchClassLoader(getClassLoader(), apkFile1, odexFile1);
            ActivityManager.getInstance().preloadActivitys(apkFile1);
            ProviderManager.getInstance().preloadProviders(apkFile1);
        } catch (Exception e) {
           e.printStackTrace();
        }
    }

    public static Context getContext() {
        return sContext;
    }
}
