package com.example.traffic_app;

public class Traffic {
//透過Gsonformat創建的資料庫類別
    /**
     * id : 1
     * longitude : 25.0419
     * latitude : 121.526
     */

    private String id;
    private String longitude;
    private String latitude;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }
}
