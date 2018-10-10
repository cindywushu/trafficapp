package com.example.traffic_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.location.Address;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class InquireMapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LatLng yourposition;//定位點
    LatLng dbposition; //資料庫經緯度資料
    LatLng destposition; //查詢的經緯度資料
    LocationRequest mLocationRequest;
    String  destination;//查詢目的地
    String carselected;//查詢車種
    String time_slotselected;//查詢時段
    Marker mCurrLocationMarker;//資料庫點的marker
    Marker mMarkers; //查詢的marker
    private static final long INTERVAL = 1000 * 2;//第一次執行時間2秒
    private static final long FASTEST_INTERVAL = 1000 * 1;//第一次後每隔1秒執行一次

    //資料庫透過PHP將資料轉換成JSON連結的網址(使用Amazon)
    String url = "http://traffic-env.eennja8tqr.ap-northeast-1.elasticbeanstalk.com/";

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
        getRetrofitArray();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMinZoomPreference(10);
        mMap.setMaxZoomPreference(20);
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
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
        carselected = getIntent().getExtras().getString("carselected");
        time_slotselected = getIntent().getExtras().getString("time_slotselected");
    }

    public void buildGoogleApiClient() { //Map的Api連線(金鑰認證)
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onBackPressed() {
        //若按手機內建的返回鍵, 則關閉定位服務
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        super.onBackPressed();
    }

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
        AlertDialog.Builder builder = new AlertDialog.Builder(InquireMapsActivity.this);
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
        mLastLocation = location; //目前的定位位置
        yourposition = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(yourposition));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(18));

        destination = getIntent().getExtras().getString("destination");
        Geocoder geoCoder = new Geocoder(InquireMapsActivity.this, Locale.getDefault());
        List<Address> addressLocation = null;
        try {
            addressLocation = geoCoder.getFromLocationName(destination, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        double latitude = addressLocation.get(0).getLatitude();
        double longitude = addressLocation.get(0).getLongitude();
        MarkerOptions mo = new MarkerOptions();
        mo.position(new LatLng(latitude, longitude))
                .title("查詢點")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mMarkers= mMap.addMarker(mo);
        destposition = new LatLng(latitude, longitude);

        String url = getUrl(yourposition, destposition);
        Log.d("onMapClick", url.toString());
        InquireFetchUrl FetchUrl = new InquireFetchUrl();

        FetchUrl.execute(url);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { //定位失敗時, Alert視窗通知
        AlertDialog.Builder builder = new AlertDialog.Builder(InquireMapsActivity.this);
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(InquireMapsActivity.this);
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

    private String getUrl(LatLng yourposition, LatLng dest) {
        String str_origin = "origin=" + this.yourposition.latitude + "," + this.yourposition.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        String sensor = "sensor=false";
        String parameters = str_origin + "&" + str_dest + "&" + sensor;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private class InquireFetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {
            String data = "";

            try {
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            InquireParserTask parserTask = new InquireParserTask();
            parserTask.execute(result);
        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class InquireParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask",jsonData[0].toString());
                JSONParserTask parser = new JSONParserTask();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask","Executing routes");
                Log.d("ParserTask",routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask",e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.BLUE);

                Log.d("onPostExecute","onPostExecute lineoptions decoded");
            }

            if(lineOptions != null) {
                mMap.addPolyline(lineOptions);
            }
            else {
                Log.d("onPostExecute","without Polylines drawn");
            }
        }
    }

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
                        if (carselected.equals("全部")){
                            if (time_slotselected.equals("全部")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("凌晨")&& TrafficData.get(i).getTimeslot().equals("凌")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("早上")&& TrafficData.get(i).getTimeslot().equals("早")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("下午")&& TrafficData.get(i).getTimeslot().equals("午")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("晚上")&& TrafficData.get(i).getTimeslot().equals("晚")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }
                        }else if (carselected.equals("小客車") && TrafficData.get(i).getMinibus().equals("1")){
                            if (time_slotselected.equals("全部")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("凌晨")&& TrafficData.get(i).getTimeslot().equals("凌")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("早上")&& TrafficData.get(i).getTimeslot().equals("早")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("下午")&& TrafficData.get(i).getTimeslot().equals("午")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("晚上")&& TrafficData.get(i).getTimeslot().equals("晚")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }
                        }else if (carselected.equals("大客車") && TrafficData.get(i).getBus().equals("1")){
                            if (time_slotselected.equals("全部")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("凌晨")&& TrafficData.get(i).getTimeslot().equals("凌")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("早上")&& TrafficData.get(i).getTimeslot().equals("早")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("下午")&& TrafficData.get(i).getTimeslot().equals("午")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("晚上")&& TrafficData.get(i).getTimeslot().equals("晚")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }
                        }else if (carselected.equals("小貨車") && TrafficData.get(i).getSmalltruck().equals("1")){
                            if (time_slotselected.equals("全部")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("凌晨")&& TrafficData.get(i).getTimeslot().equals("凌")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("早上")&& TrafficData.get(i).getTimeslot().equals("早")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("下午")&& TrafficData.get(i).getTimeslot().equals("午")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("晚上")&& TrafficData.get(i).getTimeslot().equals("晚")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }
                        }else if (carselected.equals("大貨車") && TrafficData.get(i).getBigtruck().equals("1")){
                            if (time_slotselected.equals("全部")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("凌晨")&& TrafficData.get(i).getTimeslot().equals("凌")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("早上")&& TrafficData.get(i).getTimeslot().equals("早")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("下午")&& TrafficData.get(i).getTimeslot().equals("午")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("晚上")&& TrafficData.get(i).getTimeslot().equals("晚")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }
                        }else if (carselected.equals("半聯結車") && TrafficData.get(i).getSemijoinedcar().equals("1")){
                            if (time_slotselected.equals("全部")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("凌晨")&& TrafficData.get(i).getTimeslot().equals("凌")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("早上")&& TrafficData.get(i).getTimeslot().equals("早")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("下午")&& TrafficData.get(i).getTimeslot().equals("午")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("晚上")&& TrafficData.get(i).getTimeslot().equals("晚")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }
                        }else if (carselected.equals("全聯結車") && TrafficData.get(i).getFullyconnectedcar().equals("1")){
                            if (time_slotselected.equals("全部")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("凌晨")&& TrafficData.get(i).getTimeslot().equals("凌")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("早上")&& TrafficData.get(i).getTimeslot().equals("早")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("下午")&& TrafficData.get(i).getTimeslot().equals("午")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("晚上")&& TrafficData.get(i).getTimeslot().equals("晚")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }
                        }else if (carselected.equals("曳引車") && TrafficData.get(i).getTractioncar().equals("1")){
                            if (time_slotselected.equals("全部")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("凌晨")&& TrafficData.get(i).getTimeslot().equals("凌")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("早上")&& TrafficData.get(i).getTimeslot().equals("早")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("下午")&& TrafficData.get(i).getTimeslot().equals("午")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }
                            }else if (time_slotselected.equals("晚上")&& TrafficData.get(i).getTimeslot().equals("晚")){
                                if (TrafficData.get(i).getCategory().equals("A1")){
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A1類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
                                }else {
                                    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(dbposition).title("A2類").icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//Mark資料庫的點 HUE_RED/HUE_ORANGE
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

    public void goto_Inquire(View view) {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        Intent intent=new Intent(InquireMapsActivity.this,InquireActivity.class);
        startActivity(intent);
    }
}
