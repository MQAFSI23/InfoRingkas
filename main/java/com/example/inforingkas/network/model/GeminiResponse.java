package com.example.inforingkas.network.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// Model untuk menangkap respons dari Gemini
public class GeminiResponse {

    @SerializedName("candidates")
    private List<Candidate> candidates;

    // Helper method untuk mendapatkan teks rangkuman dengan aman
    public String getSummaryText() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate firstCandidate = candidates.get(0);
            if (firstCandidate != null && firstCandidate.content != null &&
                    firstCandidate.content.parts != null && !firstCandidate.content.parts.isEmpty()) {
                Part firstPart = firstCandidate.content.parts.get(0);
                if (firstPart != null && firstPart.text != null) {
                    return firstPart.text;
                }
            }
        }
        return null; // Return null jika rangkuman tidak ditemukan
    }

    // Inner classes to match the JSON structure
    public static class Candidate {
        @SerializedName("content")
        public Content content;
    }

    public static class Content {
        @SerializedName("parts")
        public List<Part> parts;
    }

    public static class Part {
        @SerializedName("text")
        public String text;
    }
}