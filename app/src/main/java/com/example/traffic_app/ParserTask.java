package com.example.traffic_app;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

//分析定位URL及距離計算的Task
class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
    @Override
    protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

        JSONObject jObject;
        List<List<HashMap<String, String>>> routes = null;

        try {
            jObject = new JSONObject(jsonData[0]);
            Log.d("ParserTask",jsonData[0].toString());
            JSONParserTask parser = new JSONParserTask();//使用JSONParserTask.java來計算距離
            Log.d("ParserTask", parser.toString());
            routes = parser.parse(jObject);
            Log.d("ParserTask","Executing routes");
            Log.d("ParserTask",routes.toString());

        } catch (Exception e) {
            Log.d("ParserTask",e.toString());
            e.printStackTrace();
        }
        return routes;
    }
//繪製路線
//        @Override
//        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
//            ArrayList<LatLng> points;
//            PolylineOptions lineOptions = null;
//            for (int i = 0; i < result.size(); i++) {
//                points = new ArrayList<>();
//                lineOptions = new PolylineOptions();
//                List<HashMap<String, String>> path = result.get(i);
//                for (int j = 0; j < path.size(); j++) {
//                    HashMap<String, String> point = path.get(j);
//                    double lat = Double.parseDouble(point.get("lat"));
//                    double lng = Double.parseDouble(point.get("lng"));
//                    LatLng position = new LatLng(lat, lng);
//                    points.add(position);
//                }
//                lineOptions.addAll(points);
//                lineOptions.width(10);
//                lineOptions.color(Color.RED);
//
//                Log.d("onPostExecute","onPostExecute lineoptions decoded");
//
//            }
//            if(lineOptions != null) {
//                mMap.addPolyline(lineOptions);
//            }
//            else {
//                Log.d("onPostExecute","without Polylines drawn");
//            }
//        }
}
