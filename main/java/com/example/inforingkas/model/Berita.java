package com.example.inforingkas.model;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

public class Berita {
    private String articleId;
    private String title;
    private String link;
    private String pubDate; // Simpan sebagai string, bisa di-parse nanti jika perlu manipulasi tanggal
    private String imageUrl;
    private String sourceName;
    private String sourceIcon;
    private String language;
    private List<String> category; // Bisa juga String jika hanya satu atau dipisah koma
    private boolean isFavorite;
    private String rangkuman;
    private boolean isTerkini; // Untuk menandai berita dari fetch terkini

    // Konstruktor, getter, dan setter

    public Berita() {
        this.category = new ArrayList<>();
        this.isFavorite = false;
        this.isTerkini = false; // Default
    }

    public Berita(String articleId, String title, String link, String pubDate, String imageUrl,
                  String sourceName, String sourceIcon, String language, List<String> category,
                  boolean isFavorite, String rangkuman, boolean isTerkini) {
        this.articleId = articleId;
        this.title = title;
        this.link = link;
        this.pubDate = pubDate;
        this.imageUrl = imageUrl;
        this.sourceName = sourceName;
        this.sourceIcon = sourceIcon;
        this.language = language;
        this.category = category;
        this.isFavorite = isFavorite;
        this.rangkuman = rangkuman;
        this.isTerkini = isTerkini;
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

    // Helper untuk kategori (jika disimpan sebagai JSON string di DB)
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

    // Helper untuk parsing dari JSONObject (API response)
    public static Berita fromJson(JSONObject jsonObject) throws JSONException {
        Berita berita = new Berita();
        berita.setArticleId(jsonObject.optString("article_id", null));
        berita.setTitle(jsonObject.optString("title", "Judul Tidak Tersedia"));
        berita.setLink(jsonObject.optString("link", null));
        berita.setPubDate(jsonObject.optString("pubDate", "Tanggal Tidak Tersedia"));
        berita.setImageUrl(jsonObject.optString("image_url", null)); // Handle null or empty string
        berita.setSourceName(jsonObject.optString("source_name", "Sumber Tidak Diketahui"));
        berita.setSourceIcon(jsonObject.optString("source_icon", null)); // Handle null or empty string
        berita.setLanguage(jsonObject.optString("language", ""));

        JSONArray categoryArray = jsonObject.optJSONArray("category");
        List<String> categories = new ArrayList<>();
        if (categoryArray != null) {
            for (int i = 0; i < categoryArray.length(); i++) {
                categories.add(categoryArray.optString(i));
            }
        }
        berita.setCategory(categories);
        // isFavorite, rangkuman, isTerkini akan di-set dari database atau logic lain
        return berita;
    }
}