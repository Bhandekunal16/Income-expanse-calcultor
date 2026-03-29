package com.example.generalExpanseTracker.api;

import com.example.generalExpanseTracker.Model.User;
import com.example.generalExpanseTracker.Model.PaymentRequest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;

public interface ApiService {

    @POST("create")
    Call<Object> registerUser(@Body User user);

    @POST("get/account")
    Call<Map<String, Object>> loginUser(@Body Map<String, String> body);

    @POST("create/payment")
    Call<Map<String, Object>> createPayment(@Body PaymentRequest request);

    @POST("get/txn")
    Call<Object> getTransactions(@Body Map<String, String> body);

    @POST("get/monthly/txn")
    Call<Object> getMonthlyTxn(@Body Map<String, String> body);

    @POST("/get/txn/statistics")
    Call<Map<String, Object>> getStatistics(@Body Map<String, String> body);

    // 🎯 BUDGET
    @POST("add/budget")
    Call<Object> addBudget(@Body Map<String, String> body);

    @POST("get/budget")
    Call<Object> getBudget(@Body Map<String, String> body);

    @POST("update/budget")
    Call<Object> updateBudget(@Body Map<String, String> body);

    // 📤 REPORT
    @GET("send/report/")
    Call<Object> sendReport();

    // 💳 QR
    @POST("get/qr")
    Call<Object> getQr(@Body Map<String, String> body);
}