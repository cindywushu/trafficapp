package com.example.traffic_app;

//透過Gsonformat創建的資料庫類別
public class Traffic {

    /**
     * id : 3
     * time : 105年01月04日 12時05分
     * place : 嘉義縣民雄鄉國道一號259公里250公尺處北向外側車道
     * city : null
     * nums : 死亡1;受傷0
     * sort : 營業用-半聯結車;營業用-半聯結車
     * longitude : 120.41
     * latitude : 23.5382
     * category : A1
     */

    private String id;
    private String time;
    private String place;
    private Object city;
    private String nums;
    private String sort;
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

    public Object getCity() {
        return city;
    }

    public void setCity(Object city) {
        this.city = city;
    }

    public String getNums() {
        return nums;
    }

    public void setNums(String nums) {
        this.nums = nums;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
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
