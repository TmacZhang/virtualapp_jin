package com.weishu.intercept_activity.app.hook;

import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.util.Log;

import com.weishu.intercept_activity.app.StubActivity;
import com.weishu.intercept_activity.app.UPFApplication;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static com.weishu.intercept_activity.app.hook.ProviderHelper.ret;

/**
 * @author weishu
 * @dete 16/1/7.
 */
/* package */ class IActivityManagerHandler implements InvocationHandler {

    private static final String TAG = "jin";

    Object mBase;

    public IActivityManagerHandler(Object base) {
        mBase = base;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Log.i("jin", "invoke  1111111111111111 " + method.getName());
        if ("startActivity".equals(method.getName())) {
            // 只拦截这个方法
            // 替换参数, 任你所为;甚至替换原始Activity启动别的Activity偷梁换柱
            // API 23:
            // public final Activity startActivityNow(Activity parent, String id,
            // Intent intent, ActivityInfo activityInfo, IBinder token, Bundle state,
            // Activity.NonConfigurationInstances lastNonConfigurationInstances) {

            // 找到参数里面的第一个Intent 对象

            Intent raw;
            int index = 0;

            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Intent) {
                    index = i;
                    break;
                }
            }
            raw = (Intent) args[index];
            ComponentName componentName1 = raw.getComponent();

            if (componentName1 == null || !"com.android.hmct.familynumber".equals(componentName1.getPackageName())) {
                return method.invoke(mBase, args);
            }

            if (componentName1 != null) {
                Log.i("jin", "componentName=" + componentName1.getClassName(), new Exception());
            }

            Intent newIntent = new Intent();

            // 替身Activity的包名, 也就是我们自己的包名
            String stubPackage = "com.weishu.intercept_activity.app";

            // 这里我们把启动的Activity临时替换为 StubActivity
            ComponentName componentName = new ComponentName(stubPackage, StubActivity.class.getName());
            newIntent.setComponent(componentName);

            // 把我们原始要启动的TargetActivity先存起来
            newIntent.putExtra(AMSHookHelper.EXTRA_TARGET_INTENT, raw);

            // 替换掉Intent, 达到欺骗AMS的目的
            args[index] = newIntent;

            Log.d(TAG, "hook success");
            return method.invoke(mBase, args);

        }


        if ("getContentProvider".equals(method.getName())) {
            Log.i("jin", "getContentProvider  == " + args[1].toString());
            //需要hook掉这个方法，才能实现provider的免安装运行。
            //1 ProviderHelper完成provider的安装
            //2 这里构造没有安装的provider的这个方法的返回值。
            Log.i("jin", " ret.size()  == " +  ret.size());
            if (args[1].toString().startsWith("com.android.hmct.familynumber")) {
                //hook code
                ProviderInfo providerInfo = null;
                for (int i = 0; i < ret.size(); i++) {
                    providerInfo = ProviderHelper.ret.get(i);//get the target providerinfo
                    if (providerInfo.packageName.equals(args[1].toString())) {
                        //这里用预先填好的坑，代替真正的，不然holder == null
                        args[1] = "com.android.contacts";
                        Object holder = method.invoke(mBase, args);
                        Log.i("jin", "holder = "+holder);
                        if (holder == null) {
                            Log.i("jin","holder == null");
                            return null;
                        }

                        Class ContentProviderHolderclazz = Class.forName("android.app.IActivityManager$ContentProviderHolder");
                        Field providerField = ContentProviderHolderclazz.getDeclaredField("provider");
                        providerField.setAccessible(true);
                        IInterface provider = (IInterface) providerField.get(holder);

                        Field infoField = ContentProviderHolderclazz.getDeclaredField("info");
                        infoField.setAccessible(true);

                        if (provider != null) {
                            // 这里是重点，远程调用了 VAMS 的 acquireProviderClient
                            IBinder providerBinder = acquireProviderClient(providerInfo);
                            Class clazz = Class.forName("android.content.ContentProviderNative");
                            Method asInterfacemethod = clazz.getDeclaredMethod("asInterface", IBinder.class);
                            provider = (IInterface) asInterfacemethod.invoke(null, providerBinder);
                        }
                        providerField.set(holder, provider);
                        infoField.set(holder, providerInfo);
                        return holder;

                    }
                }

            }
        }

        return method.invoke(mBase, args);
    }

    public IBinder acquireProviderClient(ProviderInfo info) throws Exception {

        // 准备 ContentProviderClient
        IInterface provider = null;
        Log.i("jin","info.authority = " +info.authority);
        String[] authorities = info.authority.split(";");
        String authority = authorities.length == 0 ? info.authority : authorities[0];
        ContentResolver resolver = UPFApplication.getContext().getContentResolver();
        ContentProviderClient client = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                client = resolver.acquireUnstableContentProviderClient(authority);
            } else {
                client = resolver.acquireContentProviderClient(authority);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        Log.i("jin","client = " +client);

        if (client != null) {
            // 反射获取 provider
            Class ContentProviderClientClazz = Class.forName("android.content.ContentProviderClient");
            Field mContentProviderField = ContentProviderClientClazz.getDeclaredField("mContentProvider");
            provider = (IInterface) mContentProviderField.get(client);
            client.release();
        }
        return provider != null ? provider.asBinder() : null;
    }
}
