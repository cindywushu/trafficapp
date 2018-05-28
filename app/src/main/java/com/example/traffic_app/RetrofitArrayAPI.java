package com.example.traffic_app;

import java.util.List;

import retrofit.Call;
import retrofit.http.GET;

public interface RetrofitArrayAPI {
    //取得資料庫透過PHP將資料轉換成JSON資料連結的網址(使用Amazon)
    @GET("http://traffic-env.eennja8tqr.ap-northeast-1.elasticbeanstalk.com/")
    //取得Traffic的資料類別及類別對應的資料
    Call<List<Traffic>> getTrafficDetails();
}
