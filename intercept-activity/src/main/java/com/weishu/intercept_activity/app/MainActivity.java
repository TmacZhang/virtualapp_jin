package com.weishu.intercept_activity.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

/**
 * @author weishu
 * @date 16/1/7.
 */
public class MainActivity extends Activity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Button button = new Button(this);
        button.setText("启动TargetActivity");

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("jin","onClick");
                // 启动目标Activity; 注意这个Activity是没有在AndroidManifest.xml中显式声明的
                // 但是调用者并不需要知道, 就像一个普通的Activity一样
                //startActivity(new Intent(MainActivity.this, TargetActivity.class));
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.android.hmct.familynumber", "com.android.hmct.familynumber.PasswordActivity"));
                startActivity(intent);
                Log.i("jin","onClick  222222222");
            }
        });
        setContentView(button);
//        try {
//            //Context contactsContext = createPackageContext("com.android.contacts",0);
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }

    }
}
