package com.weishu.upf.service_management.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhangguangjin on 2016/5/24.
 */

public class ProviderManager {

    private static volatile ProviderManager sInstance;
    // 存储插件的Service信息
    private Map<ComponentName, ProviderInfo> mProviderInfoMap = new HashMap<ComponentName, ProviderInfo>();

    public synchronized static ProviderManager getInstance() {
        if (sInstance == null) {
            sInstance = new ProviderManager();
        }
        return sInstance;
    }

    public void preloadProviders(File apkFile) throws Exception {
        Class<?> packageParserClass = Class.forName("android.content.pm.PackageParser");
        Method parsePackageMethod = packageParserClass.getDeclaredMethod("parsePackage", File.class, int.class);

        Object packageParser = packageParserClass.newInstance();

        // 首先调用parsePackage获取到apk对象对应的Package对象
        Object packageObj = parsePackageMethod.invoke(packageParser, apkFile, PackageManager.GET_PROVIDERS);

        // 读取Package对象里面的providers字段
        // 接下来要做的就是根据这个List<Provider> 获取到Provider对应的ProviderInfo
        Field providersField = packageObj.getClass().getDeclaredField("providers");
        List providers = (List) providersField.get(packageObj);

        // generateProviderInfo 方法, 把PackageParser.Provider转换成ProviderInfo
        Class<?> packageParser$ProviderClass = Class.forName("android.content.pm.PackageParser$Provider");
        Class<?> packageUserStateClass = Class.forName("android.content.pm.PackageUserState");
        Class<?> userHandler = Class.forName("android.os.UserHandle");
        Method getCallingUserIdMethod = userHandler.getDeclaredMethod("getCallingUserId");
        int userId = (Integer) getCallingUserIdMethod.invoke(null);
        Object defaultUserState = packageUserStateClass.newInstance();

        // 需要调用 android.content.pm.PackageParser#generateProviderInfo(android.content.pm.PackageParser$Provider, int, android.content.pm.PackageUserState, int)
        Method generateReceiverInfo = packageParserClass.getDeclaredMethod("generateProviderInfo",
                packageParser$ProviderClass, int.class, packageUserStateClass, int.class);
        List<ProviderInfo> providerInfos = new ArrayList<ProviderInfo>();
        // 解析出intent对应的Provider组件
        for (Object provider : providers) {
            ProviderInfo info = (ProviderInfo) generateReceiverInfo.invoke(packageParser, provider, 0, defaultUserState, userId);
            mProviderInfoMap.put(new ComponentName(info.packageName, info.name), info);
            providerInfos.add(info);
            //这里需要修改ProviderInfo.applicationInfo.packageName为当前context.packageName，
            // 目的是在installProvider，骗过ActivityThread，
            // 因为此方法时候会检查包名是否一致，不一致导致provider安装不上。
            info.applicationInfo.packageName = UPFApplication.getContext().getPackageName();
            Log.i("jin", info.toString());
        }

        proxyInstallProviderInfos(providerInfos);
    }


    private void proxyInstallProviderInfos(List<ProviderInfo> providerInfos) throws Exception {
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
        Object currentActivityThread = currentActivityThreadMethod.invoke(null);

//        Field field =activityThreadClass.getDeclaredField("mInitialApplication");
//        field.setAccessible(true);
//        Context contextField = (Context) field.get(currentActivityThread);

        Method installContentProviders = activityThreadClass.getDeclaredMethod("installContentProviders", Context.class, List.class);
        installContentProviders.setAccessible(true);
        installContentProviders.invoke(currentActivityThread, UPFApplication.getContext(), providerInfos);


//        Class<?> contentProviderHolderClass = Class.forName("android.app.IActivityManager$ContentProviderHolder");
//        Method installProviderMethod = activityThreadClass.getDeclaredMethod("installProvider", Context.class, contentProviderHolderClass, ProviderInfo.class, boolean.class, boolean.class, boolean.class);
//        installProviderMethod.setAccessible(true);
//        Object object = installProviderMethod.invoke(currentActivityThread, UPFApplication.getContext(), null, providerInfos.get(0), false, true, true);
//        Log.i("jin", "object == " + object);
    }

}
