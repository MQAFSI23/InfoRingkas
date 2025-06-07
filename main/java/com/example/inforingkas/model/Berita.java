package com.example.inforingkas.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class Berita {

    // PERBAIKAN: Menambahkan anotasi @SerializedName agar cocok dengan JSON API
    @SerializedName("article_id")
    private String articleId;

    @SerializedName("title")
    private String title;

    @SerializedName("link")
    private String link;

    @SerializedName("pubDate")
    private String pubDate;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("source_name")
    private String sourceName;

    @SerializedName("source_icon")
    private String sourceIcon;

    @SerializedName("language")
    private String language;

    @SerializedName("category")
    private List<String> category;

    // Variabel ini tidak dari JSON, jadi tidak perlu anotasi
    private boolean isFavorite;
    private String rangkuman;
    private boolean isTerkini;

    // Konstruktor, getter, dan setter (tidak perlu diubah)

    public Berita() {
        this.category = new ArrayList<>();
        this.isFavorite = false;
        this.isTerkini = false;
    }

    // Getters
    public String getArticleId() { return articleId; }
    public String getTitle() { return title; }
    public String getLink() { return link; }
    public String getPubDate() { return pubDate; }
    public String getImageUrl() { return imageUrl; }
    public String getSourceName() { return sourceName; }
    public String getSourceIcon() { return sourceIcon; }
    public String getLanguage() { return language; }
    public List<String> getCategory() { return category; }
    public boolean isFavorite() { return isFavorite; }
    public String getRangkuman() { return rangkuman; }
    public boolean isTerkini() { return isTerkini; }

    // Setters
    public void setArticleId(String articleId) { this.articleId = articleId; }
    public void setTitle(String title) { this.title = title; }
    public void setLink(String link) { this.link = link; }
    public void setPubDate(String pubDate) { this.pubDate = pubDate; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public void setSourceIcon(String sourceIcon) { this.sourceIcon = sourceIcon; }
    public void setLanguage(String language) { this.language = language; }
    public void setCategory(List<String> category) { this.category = category; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public void setRangkuman(String rangkuman) { this.rangkuman = rangkuman; }
    public void setTerkini(boolean terkini) { isTerkini = terkini; }

    public String getCategoryString() {
        if (category == null || category.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < category.size(); i++) {
            sb.append(category.get(i));
            if (i < category.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public void setCategoryFromString(String categoryString) {
        this.category = new ArrayList<>();
        if (categoryString != null && !categoryString.isEmpty()) {
            String[] cats = categoryString.split(",");
            for (String cat : cats) {
                this.category.add(cat.trim());
            }
        }
    }
}