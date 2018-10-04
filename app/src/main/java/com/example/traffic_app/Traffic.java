package com.example.traffic_app;

//透過Gsonformat創建的資料庫類別
public class Traffic {

    /**
     * id : 0
     * time : 舒婷測試
     * place : 北商
     * direction : 北向
     * nums : null
     * sort : null
     * longitude : 121.524
     * latitude : 25.0423
     * category : A2
     */

    private String id;
    private String time;
    private String place;
    private String direction;
    private Object nums;
    private Object sort;
    private String longitude;
    private String latitude;
    private String category;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Object getNums() {
        return nums;
    }

    public void setNums(Object nums) {
        this.nums = nums;
    }

    public Object getSort() {
        return sort;
    }

    public void setSort(Object sort) {
        this.sort = sort;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
