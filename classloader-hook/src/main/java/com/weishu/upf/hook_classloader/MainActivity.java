package com.weishu.upf.hook_classloader;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.weishu.upf.hook_classloader.ams_hook.AMSHookHelper;
import com.weishu.upf.hook_classloader.classloder_hook.BaseDexClassLoaderHookHelper;
import com.weishu.upf.hook_classloader.classloder_hook.LoadedApkClassLoaderHookHelper;

import java.io.File;
import java.lang.reflect.Method;

/**
 * @author weishu
 * @date 16/3/28
 */
public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private static final int PATCH_BASE_CLASS_LOADER = 1;

    private static final int CUSTOM_CLASS_LOADER = 2;

    private static final int HOOK_METHOD = PATCH_BASE_CLASS_LOADER;


    private AssetManager mAssetManager;

    private Resources mResources;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Button t = new Button(this);
        t.setText("test button");

        setContentView(t);

        Log.d(TAG, "context classloader: " + getApplicationContext().getClassLoader());
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startIntent();
            }
        });

        startIntent();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void startIntent() {
        //  try {
        Log.i("jin", "startActivity  111");
        Intent t = new Intent();
        if (HOOK_METHOD == PATCH_BASE_CLASS_LOADER) {
            t.setComponent(new ComponentName("com.weishu.upf.demo.app2",
                    "com.weishu.upf.demo.app2.MainActivity"));
        } else {
            t.setComponent(new ComponentName("com.weishu.upf.ams_pms_hook.app",
                    "com.weishu.upf.ams_pms_hook.app.MainActivity"));
        }
        startActivity(t);
        Log.i("jin", "startActivity");
//        } catch (Throwable e) {
//            Log.i("jin","Throwable");
//            e.printStackTrace();
//        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        AMSHookHelper.hookActivityManagerNative();
        try {
            Utils.extractAssets(newBase, "dynamic-proxy-hook.apk");
            Utils.extractAssets(newBase, "ams-pms-hook.apk");
            Utils.extractAssets(newBase, "test.apk");

            if (HOOK_METHOD == PATCH_BASE_CLASS_LOADER) {
                File dexFile = getFileStreamPath("test.apk");
                File optDexFile = getFileStreamPath("test.dex");

                try {
                    AssetManager assetManager = AssetManager.class.newInstance();
                    Method addAssetPathMethod = assetManager.getClass().getMethod(
                            "addAssetPath", String.class);
                    addAssetPathMethod.invoke(assetManager, dexFile.getAbsolutePath());
                    Log.i("jin", "111  dexFile == " + dexFile.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Resources superRes = super.getResources();
                mResources = new Resources(mAssetManager, superRes.getDisplayMetrics(),
                        superRes.getConfiguration());
//                mTheme = mResources.newTheme();
//                mTheme.setTo(super.getTheme());


                Log.i("jin", "dexFile == " + dexFile.getAbsolutePath());
                BaseDexClassLoaderHookHelper.patchClassLoader(getClassLoader(), dexFile, optDexFile);
            } else {
                LoadedApkClassLoaderHookHelper.hookLoadedApkInActivityThread(getFileStreamPath("ams-pms-hook.apk"));
            }

            AMSHookHelper.hookActivityManagerNative();
            AMSHookHelper.hookActivityThreadHandler();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    @Override
    public AssetManager getAssets() {
        return mAssetManager == null ? super.getAssets() : mAssetManager;
    }

    @Override
    public Resources getResources() {
        return mResources == null ? super.getResources() : mResources;
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://host/path"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
