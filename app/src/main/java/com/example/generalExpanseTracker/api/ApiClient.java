package com.example.generalExpanseTracker.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.Map;


public class ApiClient {

    private static final String BASE_URL = "https://genral-expanse-tracker.vercel.app/";

    public static Retrofit getClient() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    
}