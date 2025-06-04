package com.example.inforingkas.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.example.inforingkas.BuildConfig;
import com.example.inforingkas.util.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64; // Java 8+

public class NetworkUtils {

    private static final String TAG = "NetworkUtils";
    // Kunci sederhana untuk "enkripsi" parameter (demonstrasi, BUKAN keamanan produksi)
    // Dalam produksi, API key sebaiknya tidak dikirim sebagai parameter terenkripsi kecuali API mendukungnya.
    // HTTPS sudah mengenkripsi seluruh URL saat transit.
    // Untuk Gemini, request body akan dienkripsi jika menggunakan HTTPS.
    private static final String AES_KEY = "YourSecretKeyForParams12345678"; // HARUS 16, 24, atau 32 byte

    // Metode untuk "mengenkripsi" string (Contoh sederhana, JANGAN DIGUNAKAN UNTUK KEAMANAN NYATA)
    // Ini lebih ke obfuscation daripada enkripsi yang aman untuk parameter URL.
    // API server harus bisa mendekripsi ini, yang biasanya tidak terjadi untuk API publik.
    public static String encryptParameter(String value) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                Key aesKey = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, aesKey);
                byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(encrypted);
            } catch (Exception e) {
                Log.e(TAG, "Error encrypting parameter: " + e.getMessage());
                return value; // Fallback jika gagal
            }
        }
        return value; // Untuk versi Android di bawah O, atau jika ada error
    }


    public static String fetchNewsData(String pageId) throws IOException {
        // Menggunakan API Key dari BuildConfig
        String apiKey = BuildConfig.NEWS_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "NEWS_API_KEY is not set in local.properties or BuildConfig.");
            throw new IOException("API Key for NewsData.io is missing.");
        }

        // HTTPS sudah mengenkripsi URL saat transit.
        // "Enkripsi data request" untuk API key di URL biasanya berarti menggunakan HTTPS
        // dan menyimpan API key dengan aman di sisi klien (misal, tidak hardcode).
        // Jika maksudnya mengenkripsi nilai API key itu sendiri, API server harus mendukung dekripsinya.
        // String encryptedApiKey = encryptParameter(apiKey); // Contoh jika API mendukung ini (jarang)

        String urlString = Constants.NEWS_API_BASE_URL + "?apikey=" + apiKey + "&country=id&language=indonesian";
        if (pageId != null && !pageId.isEmpty()) {
            urlString += "&page=" + pageId;
        }
        Log.d(TAG, "Fetching URL: " + urlString); // Jangan log API key di production

        return makeHttpRequest(new URL(urlString), "GET", null, Constants.NEWS_API_TIMEOUT_MS);
    }

    public static String fetchGeminiSummary(String articleLinkOrContent, boolean isLink) throws IOException {
        String geminiApiKey = BuildConfig.GEMINI_API_KEY;
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            Log.e(TAG, "GEMINI_API_KEY is not set in local.properties or BuildConfig.");
            throw new IOException("API Key for Gemini is missing.");
        }

        String apiUrl = String.format(Constants.GEMINI_API_URL_FORMAT, geminiApiKey);
        Log.d(TAG, "Gemini API URL: " + apiUrl);

        // Membuat JSON payload untuk Gemini
        // Struktur payload bisa berbeda tergantung model dan versi API Gemini.
        // Ini adalah contoh payload dasar untuk gemini-2.0-flash (sebelumnya gemini-1.5-flash)
        // {"contents":[{"parts":[{"text":"Rangkum artikel ini: [link_artikel_atau_konten_artikel]"}]}]}

        String promptText;
        if (isLink) {
            promptText = "Tolong buatkan rangkuman singkat dalam Bahasa Indonesia dari artikel berita yang ada di tautan berikut: " + articleLinkOrContent;
        } else {
            promptText = "Tolong buatkan rangkuman singkat dalam Bahasa Indonesia dari konten berita berikut: " + articleLinkOrContent;
        }

        // Escape karakter khusus dalam promptText untuk JSON
        String escapedPromptText = promptText.replace("\"", "\\\"");

        String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapedPromptText + "\"}]}]}";
        Log.d(TAG, "Gemini JSON Payload: " + jsonPayload);

        // Untuk Gemini, request body (JSON payload) akan terenkripsi jika menggunakan HTTPS.
        // Tidak perlu "enkripsi data request" secara manual pada payload jika HTTPS digunakan.
        return makeHttpRequest(new URL(apiUrl), "POST", jsonPayload, Constants.GEMINI_API_TIMEOUT_MS);
    }


    private static String makeHttpRequest(URL url, String method, String jsonPayload, int timeout) throws IOException {
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        String jsonResponse = "";

        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod(method);
            urlConnection.setReadTimeout(timeout); // milliseconds
            urlConnection.setConnectTimeout(timeout); // milliseconds

            if ("POST".equals(method) && jsonPayload != null) {
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                urlConnection.setDoOutput(true); // Penting untuk POST
                urlConnection.getOutputStream().write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(TAG, "HTTP error code: " + responseCode + " for URL: " + url);
                // Baca error stream jika ada
                inputStream = urlConnection.getErrorStream();
                if (inputStream != null) {
                    String errorResponse = readFromStream(inputStream);
                    Log.e(TAG, "Error response: " + errorResponse);
                    // Anda bisa melempar exception dengan detail error di sini
                    throw new IOException("HTTP error " + responseCode + ": " + errorResponse);
                } else {
                    throw new IOException("HTTP error code: " + responseCode);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Problem making HTTP request: " + e.toString());
            throw e; // Re-throw exception untuk ditangani oleh pemanggil
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
}