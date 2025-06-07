package com.example.inforingkas.network;

import com.example.inforingkas.network.model.GeminiRequest;
import com.example.inforingkas.network.model.GeminiResponse;
import com.example.inforingkas.network.model.NewsApiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface ApiService {

    // Endpoint untuk NewsData API
    @GET("latest")
    Call<NewsApiResponse> getLatestNews(
            @Query("apikey") String apiKey,
            @Query("country") String country,
            @Query("page") String page // bisa null
    );

    // Endpoint untuk Gemini API. Menggunakan @Url karena base URL-nya berbeda
    @POST
    Call<GeminiResponse> getGeminiSummary(
            @Url String url,
            @Body GeminiRequest body
    );
}
