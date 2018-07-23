package com.example.traffic_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
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
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;
    ScheduledExecutorService scheduledExecutorService;

    LatLng yourposition;//定位點
    LatLng dbposition; //資料庫經緯度資料
    //資料庫透過PHP將資料轉換成JSON連結的網址(使用Amazon)
    String url ="http://traffic-env.eennja8tqr.ap-northeast-1.elasticbeanstalk.com/";
    //一開始APP建置時(此階段不會顯示出來)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        getRetrofitArray();//取得資料庫的資料
    }

    @Override
    protected void onResume() {
        super.onResume();

        //onResume we start our timer so it can start when the app comes from the background
        startTimer();
    }

    Timer timer;
    TimerTask timerTask;

    //we are going to use a handler to be able to run in our TimerTask
    final Handler handler = new Handler();

    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 5000, 10000); //
    }

    public void stoptimertask(View v) {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {

                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        getRetrofitArray();
                        //取得定位URL
                        FetchUrl FetchUrl = new FetchUrl();
                        //取得定位URL轉換成JSON的資料結果
                        FetchUrl.execute(url);
                    }
                });
            }
        };
    }

    //Map定位連結
    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }
    //Map定位連結中斷時
    @Override
    public void onConnectionSuspended(int i) {

    }
    //當定位移動時
    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        yourposition = new LatLng(location.getLatitude(), location.getLongitude()); //定位點
//        mMap.addMarker(new MarkerOptions().position(delhi).title("Delhi"));//Mark定位點
        mMap.addMarker(new MarkerOptions().position(dbposition).title("Dbposition"));//Mark資料庫的點
        //當定位移動時的點
        mMap.moveCamera(CameraUpdateFactory.newLatLng(yourposition));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }

        //經緯度及距離的資料
        String str_origin = "origin=" + yourposition.latitude + "," + yourposition.longitude;
        String str_dest = "destination=" + dbposition.latitude + "," + dbposition.longitude;
        String sensor = "sensor=false";
        String parameters = str_origin + "&" + str_dest + "&" + sensor;
        String output = "json";
        //定位點的URL
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        Log.d("onMapClick", url.toString());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(yourposition));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(7));
    }

    //Map定位連結失敗時
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    //App一開始進入的地方(顯示地圖及定位點)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //Initialize Google Play Services
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
    //GoogleMap建立api及連結
    protected synchronized void buildGoogleApiClient() {
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

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
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
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }

    //從資料庫連PHP的網站取得JSON的資料(使用Retrofit)
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

                    for ( int i = 0;i < TrafficData.size();i++) {
                        //資料庫的經緯度資料的點
                        dbposition = new LatLng(Double.valueOf(TrafficData.get(i).getLatitude()), Double.valueOf(TrafficData.get(i).getLongitude()));
                        //定位點的經緯度
                        Location yourposition_location = new Location("Yourposition");
                        yourposition_location.setLatitude(yourposition.latitude);
                        yourposition_location.setLongitude(yourposition.longitude);
                        //資料庫的點的經緯度
                        Location dbposition_location = new Location("Dbposition");
                        dbposition_location.setLatitude(dbposition.latitude);
                        dbposition_location.setLongitude(dbposition.longitude);

                        double distance = (yourposition_location.distanceTo(dbposition_location));
                        //距離在?公尺內就跳出視窗顯示
                        if (distance<=10000) {
                            Toast.makeText(getApplicationContext(),"距離:"+distance+"公尺",Toast.LENGTH_SHORT).show();
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
}
