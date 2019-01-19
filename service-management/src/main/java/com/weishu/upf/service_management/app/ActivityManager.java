package com.weishu.upf.service_management.app;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhangguangjin on 2018-3-8.
 */

public class ActivityManager {
    private static volatile ActivityManager sInstance;
    // 存储插件的Service信息
    private Map<ComponentName, ActivityInfo> mActivityInfoMap = new HashMap<ComponentName, ActivityInfo>();

    public synchronized static ActivityManager getInstance() {
        if (sInstance == null) {
            sInstance = new ActivityManager();
        }
        return sInstance;
    }


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


        // generateActivityInfo 方法, 把PackageParser.Activity转换成ActivityInfo
        Class<?> packageParser$ActivityClass = Class.forName("android.content.pm.PackageParser$Activity");
        Class<?> packageUserStateClass = Class.forName("android.content.pm.PackageUserState");
        Class<?> userHandler = Class.forName("android.os.UserHandle");
        Method getCallingUserIdMethod = userHandler.getDeclaredMethod("getCallingUserId");
        int userId = (Integer) getCallingUserIdMethod.invoke(null);
        Object defaultUserState = packageUserStateClass.newInstance();

        Method generateActivityInfo = packageParserClass.getDeclaredMethod("generateActivityInfo",
                packageParser$ActivityClass, int.class, packageUserStateClass, int.class);
        List<ActivityInfo> activtyInfos = new ArrayList<ActivityInfo>();


        for (Object activity : activities) {
            ActivityInfo info = (ActivityInfo) generateActivityInfo.invoke(packageParser, activity, 0, defaultUserState, userId);
            mActivityInfoMap.put(new ComponentName(info.packageName, info.name),info);
        }

    }
}
