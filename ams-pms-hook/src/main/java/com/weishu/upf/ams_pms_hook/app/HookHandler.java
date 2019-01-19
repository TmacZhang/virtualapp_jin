package com.weishu.upf.ams_pms_hook.app;

import android.app.Activity;
import android.util.Log;
import android.view.ViewGroup;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 一个简单的用来演示的动态代理对象 (PMS和AMS都使用这一个类)
 * 只是打印日志和参数; 以后可以修改参数等达到更加高级的功能
 */
class HookHandler implements InvocationHandler {

    private static final String TAG = "HookHandler";

    private Object mBase;
    private Activity mContext;

    public HookHandler(Object base) {
        mBase = base;
    }

    public HookHandler(Activity activity, Object base) {
        mBase = base;
        mContext = activity;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Log.d(TAG, "hey, baby; you are hooked!!");
        Log.d(TAG, "method:" + method.getName() + " called with args:" + Arrays.toString(args));

        if ("addView".equals(method.getName())) {
            ViewGroup viewGroup = (ViewGroup) mContext.getLayoutInflater().inflate(R.layout.hookview, null, false);
            args[0] = viewGroup;
            return  method.invoke(mBase, args);

        }

        return method.invoke(mBase, args);
    }
}
