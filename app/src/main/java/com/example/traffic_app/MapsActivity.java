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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DecimalFormat;
import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;//目前位置
    Marker mCurrLocationMarker;//資料庫點的marker
    LocationRequest mLocationRequest;

    LatLng yourposition;//定位點
    LatLng dbposition; //資料庫經緯度資料
    double distance;//定位與資料庫點的距離
    //資料庫透過PHP將資料轉換成JSON連結的網址(使用Amazon)
    String url = "http://traffic-env.eennja8tqr.ap-northeast-1.elasticbeanstalk.com/";

    private static final long INTERVAL = 1000 * 2;//第一次執行時間2秒
    private static final long FASTEST_INTERVAL = 1000 * 1;//第一次後每隔1秒執行一次
    static TextView speedtext;//車速顯示的text
    static TextView distancetext;//距離顯示的text
    double speed;//車速

    private static final int NOTIF_ID = 1;
    Boolean all=false;//全部勾選預設為false
    Boolean A1=false;//A1勾選預設為false
    Boolean A2=false;//A2勾選預設為false
    Boolean speednoti=false;//超速提醒勾選預設為false

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        speedtext = (TextView) findViewById(R.id.speedtext);
        distancetext = (TextView) findViewById(R.id.distancetext);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission(); //檢查Map的定位認證
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //取得MainActivity的勾選值(是否有勾選)
        all = getIntent().getExtras().getBoolean("all");
        A1 = getIntent().getExtras().getBoolean("A1");
        A2 = getIntent().getExtras().getBoolean("A2");
        speednoti = getIntent().getExtras().getBoolean("speednoti");

        //執行尋找資料庫的點及通知
        getRetrofitArray();
    }

    @Override
    public void onBackPressed() {
        //若按手機內建的返回鍵, 則關閉定位服務
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        super.onBackPressed();
    }

    /********定位********/
    @Override
    public void onConnected(Bundle bundle) { //連結定位

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
    public void onConnectionSuspended(int i) { //定位中斷時, Alert視窗通知
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
    public void onLocationChanged(Location location) { //定位改變時
        mLastLocation = location; //目前的定位位置
        yourposition = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()); //定位點

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
        //顯示定位點在地圖上
        mMap.moveCamera(CameraUpdateFactory.newLatLng(yourposition));
        mMap.clear();//將Map原先有的全清除(eg.Marker)

        updateCameraBearing(mMap, mLastLocation.getBearing());//更新攝影機方向

        getRetrofitArray();//執行尋找資料庫的點及通知

        if (all||speednoti){ //若勾選全部或超速提醒時
            speed = mLastLocation.getSpeed() * 18 / 5;//計算速度
            MapsActivity.speedtext.setText("車速: " + new DecimalFormat("#.##").format(speed) + " 公里/小時");
            speed();//Notification提醒
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { //定位失敗時, Alert視窗通知
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

    private void updateCameraBearing(GoogleMap googleMap, float bearing) { //更新攝影機方向
        if ( googleMap == null) return;
        CameraPosition camPos = CameraPosition
                .builder(
                        googleMap.getCameraPosition()
                )
                .bearing(bearing)
                .target(yourposition)
                .zoom(18)
                .tilt(90)
                .build();
        mMap.setMinZoomPreference(10);
        mMap.setMaxZoomPreference(20);
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPos));
    }
    /********************/

    /*******Map準備*******/
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMinZoomPreference(12);
        mMap.setIndoorEnabled(true);
        mMap.setBuildingsEnabled(true);
        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setIndoorLevelPickerEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setRotateGesturesEnabled(true);
        uiSettings.setScrollGesturesEnabled(true);
        uiSettings.setTiltGesturesEnabled(true);
        uiSettings.setZoomGesturesEnabled(true);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);//顯示用戶的當前位置
            }
        }
        else {
            buildGoogleApiClient();//建立Api連線
            mMap.setMyLocationEnabled(true);
        }
    }

    public void buildGoogleApiClient() { //Map的Api連線(金鑰認證)
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission(){ //檢查Map的定位認證
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
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        }else {
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
                        //目前位置的經緯度
                        Location yourposition_location = new Location("Yourposition");
                        yourposition_location.setLatitude(yourposition.latitude);
                        yourposition_location.setLongitude(yourposition.longitude);
                        //資料庫點的經緯度
                        Location dbposition_location = new Location("Dbposition");
                        dbposition_location.setLatitude(dbposition.latitude);
                        dbposition_location.setLongitude(dbposition.longitude);
                        //計算距離
                        distance = (yourposition_location.distanceTo(dbposition_location));

                        if (all||(A1&&A2)){ //若勾選全部或A1及A2
                            if (TrafficData.get(i).getCategory().equals("A1")||TrafficData.get(i).getCategory().equals("A2")){
                                if (distance<500){ //顯示所有距離500m內的點
                                    MapsActivity.distancetext.setText("離危險路段距離約: " + new DecimalFormat("#.##").format(distance) + "公尺");
                                    if (TrafficData.get(i).getCategory().equals("A1")){
                                        addMarker_RED(); //A1類顯示紅點
                                    }else {
                                        addMarker_ORANGE(); //A2類顯示橘點
                                    }
                                    notification(); //提醒通知
                                }
                            }
                        }else if(A1){ //若勾選A1
                            if (TrafficData.get(i).getCategory().equals("A1")){
                                if (distance<500){ //顯示所有距離500m內的點
                                    MapsActivity.distancetext.setText("離危險路段距離約: " + new DecimalFormat("#.##").format(distance) + "公尺");
                                    addMarker_RED(); //A1類顯示紅點
                                    notification(); //提醒通知
                                }
                            }
                        }else if (A2){ //若勾選A2
                            if (TrafficData.get(i).getCategory().equals("A2")){
                                if (distance<500){ //顯示所有距離500m內的點
                                    MapsActivity.distancetext.setText("離危險路段距離約: " + new DecimalFormat("#.##").format(distance) + "公尺");
                                    addMarker_ORANGE(); //A2類顯示橘點
                                    notification(); //提醒通知
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
    public void speed(){ //超速的提醒
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
                            .setContentTitle("注意！")
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

    public void notification(){ //在設定距離內的危險路段提醒
        //使用聲音
        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.noti_500);
        // 取得NotificationManager系統服務
        NotificationManager notiMgr = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        // 建立狀態列顯示的通知訊息
        NotificationCompat.Builder noti =
                new NotificationCompat.Builder(MapsActivity.this)
                        .setSound(soundUri)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("注意！")
                        .setContentText("前方五百公尺內為危險路段, 請小心駕駛！");
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

    public void addMarker_RED(){ //A1類顯示紅點
        mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
    }

    public void addMarker_ORANGE(){ //A2類顯示橘點
        mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
    }
    /********************/
    public void goto_Main(View view) { //回首頁的button, 按下會關閉服務並回首頁(MainActivity)
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
//    Toast.makeText(MapsActivity.this, "bearing:"+bearing, Toast.LENGTH_LONG).show();
}
