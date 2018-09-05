package com.example.traffic_app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class InquireActivity extends AppCompatActivity {

    EditText destination;
    String dest;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquire);

        destination = (EditText) findViewById(R.id.destination);
    }

    public void goto_Map(View view){
        dest = destination.getText().toString();
        Intent intent=new Intent(InquireActivity.this, InquireMapsActivity.class);
        intent.putExtra( "destination", dest);
        startActivity(intent);
    }
}
