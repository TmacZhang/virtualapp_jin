package com.weishu.intercept_activity.app;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.IInterface;
import android.util.Log;

import com.weishu.intercept_activity.app.hook.AMSHookHelper;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by zhangguangjin on 2018-3-8.
 */

public class ActivityManager {
    public static ApplicationInfo applicationInfo;
    public static PackageInfo packageInfo;
    private static volatile ActivityManager sInstance;
    // 存储插件的Service信息
    public Map<String, ActivityInfo> mActivityInfoMap = new HashMap<String, ActivityInfo>();

    public synchronized static ActivityManager getInstance() {
        if (sInstance == null) {
            sInstance = new ActivityManager();
        }
        return sInstance;
    }

    public static Class<?>[] getAllInterface(Class clazz) {
        HashSet<Class<?>> classes = new HashSet<Class<?>>();
        getAllInterfaces(clazz, classes);
        Class<?>[] result = new Class[classes.size()];
        classes.toArray(result);
        Log.i("jin1","length = " + result.length);
        for(int i =0 ;i <result.length;i++){
            Log.i("jin1",result[i].getSimpleName());
        }
        return result;
    }

    public static void getAllInterfaces(Class clazz, HashSet<Class<?>> interfaceCollection) {
        Class<?>[] classes = clazz.getInterfaces();
        if (classes.length != 0) {
            interfaceCollection.addAll(Arrays.asList(classes));
        }
        if (clazz.getSuperclass() != Object.class) {
            getAllInterfaces(clazz.getSuperclass(), interfaceCollection);
        }
    }


    //1  接下来需要做的就是hook掉PackageManagerService的getApplicationInfo，来构建上下文，createPackageContext
    //2  然后通过生成的上下文，修改一些类的成员变量，然后反射生成Application，然后，再修改一些成员变量。
    //3  如何解决资源找不到的问题？？

    public void preloadActivitys(File apkFile) throws Exception {
        Class<?> packageParserClass = Class.forName("android.content.pm.PackageParser");
        Method parsePackageMethod = packageParserClass.getDeclaredMethod("parsePackage", File.class, int.class);

        Object packageParser = packageParserClass.newInstance();

        // 首先调用parsePackage获取到apk对象对应的Package对象
        Object packageObj = parsePackageMethod.invoke(packageParser, apkFile, PackageManager.GET_ACTIVITIES);


        // 读取Package对象里面的providers字段
        // 接下来要做的就是根据这个List<Provider> 获取到Provider对应的ProviderInfo
        Field activitiesField = packageObj.getClass().getDeclaredField("activities");
        List activities = (List) activitiesField.get(packageObj);


        // generateActivityInfo 方法, 把PackageParser.Activity 转换成 ActivityInfo
        Class<?> packageParser$ActivityClass = Class.forName("android.content.pm.PackageParser$Activity");
        Class<?> packageUserStateClass = Class.forName("android.content.pm.PackageUserState");
        Class<?> userHandler = Class.forName("android.os.UserHandle");
        Method getCallingUserIdMethod = userHandler.getDeclaredMethod("getCallingUserId");
        int userId = (Integer) getCallingUserIdMethod.invoke(null);
        Object defaultUserState = packageUserStateClass.newInstance();

        Method generateActivityInfo = packageParserClass.getDeclaredMethod("generateActivityInfo",
                packageParser$ActivityClass, int.class, packageUserStateClass, int.class);
        List<ActivityInfo> activtyInfos = new ArrayList<ActivityInfo>();

        String packageName = "";

        Field applicationInfoField = packageObj.getClass().getDeclaredField("applicationInfo");
        applicationInfo = (ApplicationInfo) applicationInfoField.get(packageObj);
        applicationInfo.sourceDir = "/data/user/0/com.weishu.intercept_activity.app/files/family_number.jar";
        applicationInfo.publicSourceDir = "/data/user/0/com.weishu.intercept_activity.app/files/family_number.jar";// 2018-3-28加入这句，实现资源可以访问
        Log.i("jin", "applicationInfo.toString:" + applicationInfo.toString());
        Log.i("jin", "applicationInfo.publicSourceDir:" + applicationInfo.publicSourceDir);
        Log.i("jin", "applicationInfo.sourceDir:" + applicationInfo.sourceDir);

        for (Object activity : activities) {
            ActivityInfo info = (ActivityInfo) generateActivityInfo.invoke(packageParser, activity, 0, defaultUserState, userId);
            packageName = info.packageName;
            mActivityInfoMap.put(info.packageName + info.name, info);
            Log.i("jin", "info:" + info.applicationInfo.dataDir);
        }


        Log.i("jin", "preloadActivitys:" + mActivityInfoMap.toString());
        Context realContext = createPackageContext(packageName);
        Log.i("jin", "realContext = " + realContext);
        Class contextClazz = Class.forName("android.app.ContextImpl");
        Field info = contextClazz.getDeclaredField("mPackageInfo");
        info.setAccessible(true);
        Object loadapk = info.get(realContext);
        Log.i("jin", "loadapk:" + loadapk.toString());
        Class appBindDataClazz = Class.forName("android.app.ActivityThread$AppBindData");
        Object activityThread = AMSHookHelper.hookActivityThreadHandler();
        Class activityThreadClazz = Class.forName("android.app.ActivityThread");
        Field mBoundApplicationFiled = activityThreadClazz.getDeclaredField("mBoundApplication");
        mBoundApplicationFiled.setAccessible(true);
        Object boundApp = mBoundApplicationFiled.get(activityThread);
        Field infoField = appBindDataClazz.getDeclaredField("info");
        infoField.setAccessible(true);
        infoField.set(boundApp, loadapk);


        //PackageParser.generatePackageInfo 解析packageInfo
        Class<?> packageParser$PackageClass = Class.forName("android.content.pm.PackageParser$Package");
        Method generatePackageInfoMethod = packageParserClass.getDeclaredMethod("generatePackageInfo",
                packageParser$PackageClass, int[].class, int.class, long.class, long.class, Set.class, packageUserStateClass);
        packageInfo = (PackageInfo) generatePackageInfoMethod.invoke(packageParser, packageObj, new int[]{}, 0, 0, 0, null, defaultUserState);
        Log.i("jin", "packageInfo:" + packageInfo);

        Class loadedApkClazz = Class.forName("android.app.LoadedApk");
        Method makeApplicationMethod = loadedApkClazz.getDeclaredMethod("makeApplication", boolean.class, Instrumentation.class);
        Application application = (Application) makeApplicationMethod.invoke(loadapk, false, null);


        Field resDirField = loadedApkClazz.getDeclaredField("mResDir");
        resDirField.setAccessible(true);
        String resDir = (String) resDirField.get(loadapk);
        Log.i("jin", "resDir:" + resDir);
        resDirField.set(loadapk, "/data/user/0/com.weishu.intercept_activity.app/files/family_number.jar");

        Field mInitialApplicationFiled = activityThreadClazz.getDeclaredField("mInitialApplication");
        mInitialApplicationFiled.setAccessible(true);
        mInitialApplicationFiled.set(activityThread, application);
        Log.i("jin", "application:" + application.toString());
        Log.i("jin", "application:" + application.getPackageCodePath() + "|" + application.getPackageName());

        Class contextImplClazz = Class.forName("android.app.ContextImpl");
        Field packageManagerField = contextImplClazz.getDeclaredField("mPackageManager");
        packageManagerField.setAccessible(true);
        packageManagerField.set(realContext, null);
        realContext.getPackageManager();
        //测试资源id。 
        int id = 0x7f030002;
        Log.i("jin", "application 0x7f030002 =:" + realContext.getResources().getLayout(id));
        //appops 权限管理,这里需要hook掉系统的权限管理 AppOpsManager，可以通过动态代理，加反射，直接hook掉这个AppOpsManager service,
        //然后把noteOperation中的packagename换成宿主的，因为宿主加了所有权限了，问题解决了。貌似不对
        Field mBasePackageNameField = contextImplClazz.getDeclaredField("mBasePackageName");
        mBasePackageNameField.setAccessible(true);
        mBasePackageNameField.set(realContext, UPFApplication.getContext().getPackageName());


        hookAppOpsmanager();
    }

    private Context createPackageContext(String packageName) {
        try {
            Log.i("jin", "createPackageContext   111111");
            return UPFApplication.getContext().createPackageContext(packageName, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Log.i("jin", "PackageManager.NameNotFoundException", e);
        }
        return null;
    }

    private void hookAppOpsmanager() throws Exception {
        Log.i("jin", "hookAppOpsmanager 11111111");
        Class clazz = Class.forName("android.os.ServiceManager");
        Method method = clazz.getMethod("getService", String.class);
        IBinder iBinder = (IBinder) method.invoke(null, Context.APP_OPS_SERVICE);


        Log.i("jin", "iInterface 11111111 = " + iBinder.getClass().getName());
        Class clazzIAppOpsService$Stub = Class.forName("com.android.internal.app.IAppOpsService$Stub");
        Method asInterfaceMethod = clazzIAppOpsService$Stub.getMethod("asInterface", IBinder.class);
        final IInterface iInterface = (IInterface) asInterfaceMethod.invoke(null, iBinder);
        Log.i("jin", "iInterface 11111111 = " + iInterface.getClass().getName());

        IInterface iInterface1 = (IInterface) Proxy.newProxyInstance(iInterface.getClass().getClassLoader(), getAllInterface(iInterface.getClass()), new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Log.i("jin1", "hook   AppOpsService  method.getName() = " + method.getName(),new Exception());
                if (isHookedMethod(method.getName())) {
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] instanceof String) {
                            args[i] = UPFApplication.getContext().getPackageName();
                        }
                    }
                }

                return method.invoke(iInterface, args);
            }
        });
        Log.i("jin", "iInterface1 11111111 = " + iInterface1.getClass().getName());
        Log.i("jin", "hookAppOpsmanager 2222222");
        BinderStub binderStub = new BinderStub(iBinder);
        binderStub.setIIerface(iInterface1);

        Field cacheField = clazz.getDeclaredField("sCache");
        cacheField.setAccessible(true);
        Map<String, IBinder> sCache = (Map<String, IBinder>) cacheField.get(null);
        sCache.put(Context.APP_OPS_SERVICE, binderStub);
        Log.i("jin", "hookAppOpsmanager 33333");
        Map<String, IBinder> sCache1 = (Map<String, IBinder>) cacheField.get(null);
        Log.i("jin", "hookAppOpsmanager 444 = " + sCache1.get(Context.APP_OPS_SERVICE));
    }

    private boolean isHookedMethod(String chechName) {
        Log.i("jin1", "chechName = " + chechName);
        String[] names = {"checkOperation", "noteProxyOperation", "noteOperation", "startOperation", "finishOperation", "startWatchingMode", "checkPackage", "getOpsForPackage"};
        for (String name : names) {
            if (name.equals(chechName)) {
                return true;
            }
        }
        return false;
    }
}
