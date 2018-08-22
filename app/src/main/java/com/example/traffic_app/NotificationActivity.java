package com.example.traffic_app;

import android.app.NotificationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class NotificationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        NotificationManager notiMgr = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        // 取消狀態列的通知訊息
        notiMgr.cancel(getIntent().getExtras().getInt("NOTIFICATION_ID"));
    }
}

