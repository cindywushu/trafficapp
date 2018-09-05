package com.example.traffic_app;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

public class Noti_selectionActivity extends AppCompatActivity {

    CheckBox allcheck;
    CheckBox A1check;
    CheckBox A2check;
    CheckBox speednoticheck;
    Boolean all;
    Boolean A1;
    Boolean A2;
    Boolean speednoti;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_noti_selection);

        allcheck = (CheckBox)findViewById(R.id.all);
        A1check = (CheckBox)findViewById(R.id.A1);
        A2check = (CheckBox)findViewById(R.id.A2);
        speednoticheck = (CheckBox)findViewById(R.id.speed);
    }

    public void goto_Map(View view) {
        all = allcheck.isChecked();
        A1 = A1check.isChecked();
        A2 = A2check.isChecked();
        speednoti = speednoticheck.isChecked();

        if (!all && !A1 && !A2 && !speednoti){ //若勾選為空
            AlertDialog.Builder builder = new AlertDialog.Builder(Noti_selectionActivity.this);
            builder.setTitle("注意！")
                    .setMessage("請選擇服務項目！")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }else if (all&&(A1 || A2 || speednoti)){ //若勾選為全部及其他選項
            AlertDialog.Builder builder = new AlertDialog.Builder(Noti_selectionActivity.this);
            builder.setTitle("注意！")
                    .setMessage("已重複選擇，請更改！")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }else {
            Intent intent=new Intent(Noti_selectionActivity.this,MapsActivity.class);
            intent.putExtra("all", all);
            intent.putExtra("A1", A1);
            intent.putExtra("A2", A2);
            intent.putExtra("speednoti", speednoti);
            startActivity(intent);
        }
    }
}
