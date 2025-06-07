package com.example.inforingkas.network.model;

import java.util.Collections;
import java.util.List;

// Model untuk mengirim request ke Gemini
public class GeminiRequest {
    private final List<Content> contents;

    public GeminiRequest(String text) {
        Part part = new Part(text);
        Content content = new Content(Collections.singletonList(part));
        this.contents = Collections.singletonList(content);
    }

    // Inner classes to match the JSON structure
    static class Content {
        List<Part> parts;
        Content(List<Part> parts) { this.parts = parts; }
    }

    static class Part {
        String text;
        Part(String text) { this.text = text; }
    }
}