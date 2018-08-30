package com.example.traffic_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Timer;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;//目前位置
    Marker mCurrLocationMarker;//資料庫點的marker
    LocationRequest mLocationRequest;

    LatLng yourposition;//定位點
    LatLng dbposition; //資料庫經緯度資料
    double distance;
    //資料庫透過PHP將資料轉換成JSON連結的網址(使用Amazon)
    String url = "http://traffic-env.eennja8tqr.ap-northeast-1.elasticbeanstalk.com/";

    private static final long INTERVAL = 1000 * 2;
    private static final long FASTEST_INTERVAL = 1000 * 1;
    static TextView speedtext;
    double speed;

    private static final int NOTIF_ID = 1;
    Boolean all=false;
    Boolean A1=false;
    Boolean A2=false;
    Boolean speednoti=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        speedtext = (TextView) findViewById(R.id.speedtext);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        all = getIntent().getExtras().getBoolean("all");
        A1 = getIntent().getExtras().getBoolean("A1");
        A2 = getIntent().getExtras().getBoolean("A2");
        speednoti = getIntent().getExtras().getBoolean("speednoti");

        getRetrofitArray();
    }

    @Override
    public void onBackPressed() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        super.onBackPressed();
    }

    /********定位********/
    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle("注意！")
                .setMessage("Map定位連結中斷！")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        yourposition = new LatLng(location.getLatitude(), location.getLongitude()); //定位點

        //經緯度及距離的資料
        String str_origin = "origin=" + yourposition.latitude + "," + yourposition.longitude;
        String str_dest = "destination=" + dbposition.latitude + "," + dbposition.longitude;
        String sensor = "sensor=false";
        String parameters = str_origin + "&" + str_dest + "&" + sensor;
        String output = "json";
        //定位點的URL
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        Log.d("onMapClick", url.toString());
        //取得定位URL
        FetchUrl FetchUrl = new FetchUrl();
        //取得定位URL轉換成JSON的資料結果
        FetchUrl.execute(url);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(yourposition));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
        mMap.clear();

        getRetrofitArray();//執行尋找資料庫的點及通知

        if (all||speednoti){
            //計算速度
            speed = mLastLocation.getSpeed() * 18 / 5;
            MapsActivity.speedtext.setText("車速: " + new DecimalFormat("#.##").format(speed) + " km/hr");
            speed();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle("注意！")
                .setMessage("Map定位連結失敗！")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    /********************/

    /*******Map準備*******/
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
    }

    public void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    //檢查Map的認證
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    //Map的認證結果
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    builder.setTitle("注意！")
                            .setMessage("Map認證失敗！")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
                return;
            }
        }
    }
    /********************/

    /****資料庫Retrofit****/
    //使用Retrofit取得資料後計算距離
    void getRetrofitArray() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RetrofitArrayAPI service = retrofit.create(RetrofitArrayAPI.class);
        //取得Traffic的資料類別及類別對應的資料
        Call<List<Traffic>> call = service.getTrafficDetails();
        //取得處理完的資料
        call.enqueue(new Callback<List<Traffic>>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(Response<List<Traffic>> response, Retrofit retrofit) {

                try {
                    List<Traffic> TrafficData = response.body();

                    for (int i = 0;i <= TrafficData.size();i++) {
                        //資料庫的經緯度資料的點
                        dbposition = new LatLng(Double.valueOf(TrafficData.get(i).getLatitude()), Double.valueOf(TrafficData.get(i).getLongitude()));

                        Location yourposition_location = new Location("Yourposition");
                        yourposition_location.setLatitude(yourposition.latitude);
                        yourposition_location.setLongitude(yourposition.longitude);

                        Location dbposition_location = new Location("Dbposition");
                        dbposition_location.setLatitude(dbposition.latitude);
                        dbposition_location.setLongitude(dbposition.longitude);

                        distance = (yourposition_location.distanceTo(dbposition_location));

                        if (all||(A1&&A2)){
                            if (TrafficData.get(i).getCategory().equals("A1")||TrafficData.get(i).getCategory().equals("A2")){
                                if (distance<500){
                                    if (TrafficData.get(i).getCategory().equals("A1")){
                                        addMarker_RED();
                                    }else {
                                        addMarker_ORANGE();
                                    }
                                    notification();
                                }
                            }
                        }else if(A1){
                            if (TrafficData.get(i).getCategory().equals("A1")){
                                if (distance<500){
                                    addMarker_RED();
                                    notification();
                                }
                            }
                        }else if (A2){
                            if (TrafficData.get(i).getCategory().equals("A2")){
                                if (distance<500){
                                    addMarker_ORANGE();
                                    notification();
                                }
                            }
                        }

                    }

                } catch (Exception e) {
                    Log.d("onResponse", "There is an error");
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.d("onFailure", t.toString());
            }
        });
    }
    public void speed(){
        if(speed>=110) { //國道規定最高速110~120
            //使用聲音
            Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.speeding);
            // 取得NotificationManager系統服務
            NotificationManager notiMgr = (NotificationManager)
                    getSystemService(NOTIFICATION_SERVICE);
            // 建立狀態列顯示的通知訊息
            NotificationCompat.Builder speed =
                    new NotificationCompat.Builder(MapsActivity.this)
                            .setSound(soundUri)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("注意")
                            .setContentText("超速, 您已進入危險路段, 請減速！");
            Intent intent = new Intent(MapsActivity.this, NotificationActivity.class);
            intent.putExtra("NOTIFICATION_ID", NOTIF_ID);
            // 建立PendingIntent物件
            PendingIntent pIntent = PendingIntent.getActivity(MapsActivity.this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            speed.setContentIntent(pIntent);  // 指定PendingIntent
            Notification note = speed.build();

            // 使用振動
            note.vibrate = new long[]{100, 250, 100, 500};
            // 使用LED
            note.ledARGB = Color.RED;
            note.flags |= Notification.FLAG_SHOW_LIGHTS;
            note.ledOnMS = 200;
            note.ledOffMS = 300;
            notiMgr.notify(NOTIF_ID, note);   // 送出通知訊息
        }
    }

    public void notification(){
        //Toast.makeText(getBaseContext(),"距離："+ distance, Toast.LENGTH_LONG).show();
        //使用聲音
        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.dan_ten);
        // 取得NotificationManager系統服務
        NotificationManager notiMgr = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        // 建立狀態列顯示的通知訊息
        NotificationCompat.Builder noti =
                new NotificationCompat.Builder(MapsActivity.this)
                        .setSound(soundUri)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("注意")
                        .setContentText("前方約五百公尺處為危險路段");
        Intent intent = new Intent(MapsActivity.this, NotificationActivity.class);
        intent.putExtra("NOTIFICATION_ID", NOTIF_ID);
        // 建立PendingIntent物件
        PendingIntent pIntent = PendingIntent.getActivity(MapsActivity.this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        noti.setContentIntent(pIntent);  // 指定PendingIntent
        Notification note = noti.build();

        // 使用振動
        note.vibrate= new long[] {100, 250, 100, 500};
        // 使用LED
        note.ledARGB = Color.RED;
        note.flags |= Notification.FLAG_SHOW_LIGHTS;
        note.ledOnMS = 200;
        note.ledOffMS = 300;
        notiMgr.notify(NOTIF_ID, note);// 送出通知訊息
    }

    public void addMarker_RED(){
        mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("Dbposition").icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
    }

    public void addMarker_ORANGE(){
        mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("Dbposition").icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
    }
    /********************/
    public void goto_Main(View view) {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        Intent intent=new Intent(MapsActivity.this,MainActivity.class);
        startActivity(intent);
    }

//    /********定時器*******/
//    @Override
//    protected void onResume() {
//        super.onResume();
//        startTimer();
//    }
//
//    Timer timer;
//    java.util.TimerTask timerTask;
//
//    final Handler handler = new Handler();
//
//    public void startTimer() {
//        timer = new Timer();
//        initializeTimerTask();
//        //第一次執行3秒, 之後每隔2秒執行一次
//        timer.schedule(timerTask, 2000, 1000);
//    }
//
//    public void initializeTimerTask() {
//
//        timerTask = new java.util.TimerTask() {
//            public void run() {
//                handler.post(new Runnable() {
//                    public void run() {
//
//                    }
//                });
//            }
//        };
//    }
//    stop timer:
//    timer.cancel();
//    timer = null;
//    /********************/
}
