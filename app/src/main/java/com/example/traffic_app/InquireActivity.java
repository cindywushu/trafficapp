package com.example.traffic_app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class InquireActivity extends AppCompatActivity {

    EditText destination;
    String dest;
    String carselected;
    String time_slotselected;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquire);

        destination = (EditText) findViewById(R.id.destination);

        Spinner spinner = (Spinner)findViewById(R.id.spinner);
        final String[] car = {"全部", "小客車", "大客車", "小貨車", "大貨車", "半聯結車", "全聯結車", "曳引車"};
        ArrayAdapter<String> carList = new ArrayAdapter<>(InquireActivity.this,
                android.R.layout.simple_spinner_dropdown_item,
                car);
        spinner.setAdapter(carList);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                carselected = car[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Spinner spinnertime = (Spinner)findViewById(R.id.spinnertime);
        final String[] time_slot = {"全部", "凌晨", "早上", "下午", "晚上"};
        ArrayAdapter<String> time_slotList = new ArrayAdapter<>(InquireActivity.this,
                android.R.layout.simple_spinner_dropdown_item,
                time_slot);
        spinnertime.setAdapter(time_slotList);
        spinnertime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                time_slotselected = time_slot[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void goto_Map(View view){
        dest = destination.getText().toString();
        Intent intent=new Intent(InquireActivity.this, InquireMapsActivity.class);
        intent.putExtra( "destination", dest);
        intent.putExtra( "carselected", carselected);
        intent.putExtra( "time_slotselected", time_slotselected);
        startActivity(intent);
    }
}
