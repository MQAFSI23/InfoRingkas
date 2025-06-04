package com.example.inforingkas.util;

public class Constants {
    // SharedPreferences Keys
    public static final String SHARED_PREFS_NAME = "InfoRingkasPrefs";
    public static final String KEY_THEME_MODE = "ThemeMode"; // Values: "light", "dark", "system"
    public static final String KEY_LAST_FETCH_DATE_TERKINI = "LastFetchDateTerkini"; // Store date as YYYY-MM-DD

    // API Related
    public static final String NEWS_API_BASE_URL = "https://newsdata.io/api/1/latest";
    // API Keys are accessed via BuildConfig.NEWS_API_KEY and BuildConfig.GEMINI_API_KEY

    // Gemini API
    public static final String GEMINI_API_URL_FORMAT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=%s";
    // Note: The prompt for Gemini needs to be carefully constructed.
    // Example prompt: "Buatkan rangkuman singkat dari artikel berita berikut: [link_berita]"
    // Or, if you fetch the article content first: "Buatkan rangkuman singkat dari konten berita berikut: [konten_artikel]"

    // Database
    public static final String DATABASE_NAME = "InfoRingkasDB";
    public static final int DATABASE_VERSION = 1;

    // Table Berita
    public static final String TABLE_BERITA = "berita";
    public static final String COLUMN_ARTICLE_ID = "article_id"; // Primary Key
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_LINK = "link";
    public static final String COLUMN_PUB_DATE = "pubDate";
    public static final String COLUMN_IMAGE_URL = "image_url";
    public static final String COLUMN_SOURCE_NAME = "source_name";
    public static final String COLUMN_SOURCE_ICON = "source_icon";
    public static final String COLUMN_LANGUAGE = "language";
    public static final String COLUMN_CATEGORY = "category"; // Store as JSON string or comma-separated
    public static final String COLUMN_IS_FAVORITE = "is_favorite"; // INTEGER 0 or 1
    public static final String COLUMN_RANGKUMAN = "rangkuman"; // Text summary from Gemini
    public static final String COLUMN_IS_TERKINI = "is_terkini"; // INTEGER 0 or 1, to differentiate latest news

    // Intent Extras
    public static final String EXTRA_ARTICLE_ID = "extra_article_id";
    public static final String EXTRA_ARTICLE_LINK = "extra_article_link"; // For RangkumanActivity if article_id not enough

    // Default values
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_SYSTEM = "system"; // Or follow system setting

    // Network
    public static final int NEWS_API_TIMEOUT_MS = 10000; // 10 seconds
    public static final int GEMINI_API_TIMEOUT_MS = 20000; // 20 seconds
}