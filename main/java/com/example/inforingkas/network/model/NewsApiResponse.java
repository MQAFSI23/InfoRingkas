package com.example.inforingkas.network.model;

import com.example.inforingkas.model.Berita;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NewsApiResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("results")
    private List<Berita> results;

    @SerializedName("nextPage")
    private String nextPage;

    // Getters
    public String getStatus() {
        return status;
    }

    public List<Berita> getResults() {
        return results;
    }

    public String getNextPage() {
        return nextPage;
    }
}