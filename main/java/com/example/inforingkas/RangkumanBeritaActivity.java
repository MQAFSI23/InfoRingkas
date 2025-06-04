package com.example.inforingkas;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat; // Untuk FAB tint
import com.example.inforingkas.databinding.ActivityRangkumanBeritaBinding;
import com.example.inforingkas.db.DatabaseHelper;
import com.example.inforingkas.model.Berita;
import com.example.inforingkas.network.NetworkUtils;
import com.example.inforingkas.util.Constants;
import com.example.inforingkas.util.ThemePreferenceHelper;
import com.example.inforingkas.util.ThemeUtils; // Untuk warna tema
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RangkumanBeritaActivity extends AppCompatActivity {

    private static final String TAG = "RangkumanBeritaActivity";
    private ActivityRangkumanBeritaBinding binding;
    private DatabaseHelper dbHelper;
    private ThemePreferenceHelper themePreferenceHelper;
    private String articleId;
    private Berita currentBerita;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Terapkan tema sebelum setContentView
        themePreferenceHelper = new ThemePreferenceHelper(this);
        ThemePreferenceHelper.applyTheme(themePreferenceHelper.getThemeMode()); // Static call

        binding = ActivityRangkumanBeritaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        // Setup Toolbar
        Toolbar toolbar = binding.toolbarRangkuman;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.label_rangkuman_berita);
        }

        // Ambil article_id dari Intent
        articleId = getIntent().getStringExtra(Constants.EXTRA_ARTICLE_ID);

        if (articleId == null || articleId.isEmpty()) {
            Toast.makeText(this, "ID Artikel tidak valid.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadBeritaAndRangkuman();

        binding.fabFavoriteRangkuman.setOnClickListener(v -> toggleFavorite());
    }

    private void loadBeritaAndRangkuman() {
        showLoadingRangkuman(true, getString(R.string.label_memuat_berita));
        executorService.execute(() -> {
            currentBerita = dbHelper.getBerita(articleId);
            mainThreadHandler.post(() -> {
                if (currentBerita == null) {
                    showLoadingRangkuman(false, null);
                    Toast.makeText(this, "Berita tidak ditemukan.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                displayBeritaDetails();
                updateFavoriteFab();

                if (!TextUtils.isEmpty(currentBerita.getRangkuman())) {
                    displayRangkuman(currentBerita.getRangkuman());
                    showLoadingRangkuman(false, null);
                } else {
                    // Jika tidak ada rangkuman, fetch dari Gemini
                    fetchRangkumanFromGemini();
                }
            });
        });
    }

    private void displayBeritaDetails() {
        if (currentBerita != null) {
            binding.textViewJudulRangkuman.setText(currentBerita.getTitle());
            binding.textViewSumberRangkuman.setText(String.format(getString(R.string.label_sumber), currentBerita.getSourceName()));
            binding.textViewTanggalRangkuman.setText(String.format("Tanggal: %s", currentBerita.getPubDate()));
        }
    }

    private void fetchRangkumanFromGemini() {
        if (currentBerita == null || TextUtils.isEmpty(currentBerita.getLink())) {
            displayRangkuman(getString(R.string.label_gagal_memuat_rangkuman) + " (Link berita tidak tersedia)");
            showLoadingRangkuman(false, null);
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            displayRangkuman(getString(R.string.label_gagal_memuat_rangkuman) + " (Tidak ada koneksi internet)");
            showLoadingRangkuman(false, null);
            return;
        }

        showLoadingRangkuman(true, getString(R.string.label_memuat_rangkuman));
        executorService.execute(() -> {
            try {
                // Mengirim link berita untuk dirangkum
                String jsonResponse = NetworkUtils.fetchGeminiSummary(currentBerita.getLink(), true);
                Log.d(TAG, "Gemini API Response: " + jsonResponse.substring(0, Math.min(jsonResponse.length(), 500)));

                // Parsing respons Gemini (struktur bisa bervariasi)
                // Contoh asumsi: {"candidates":[{"content":{"parts":[{"text":"Ini adalah rangkuman..."}]}}]}
                JSONObject responseObject = new JSONObject(jsonResponse);
                JSONArray candidates = responseObject.optJSONArray("candidates");
                String summaryText = getString(R.string.label_gagal_memuat_rangkuman) + " (Format respons tidak dikenali)";

                if (candidates != null && candidates.length() > 0) {
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject content = firstCandidate.optJSONObject("content");
                    if (content != null) {
                        JSONArray parts = content.optJSONArray("parts");
                        if (parts != null && parts.length() > 0) {
                            JSONObject firstPart = parts.getJSONObject(0);
                            summaryText = firstPart.optString("text", summaryText);
                        }
                    }
                }

                // Simpan rangkuman ke database
                if (!summaryText.startsWith(getString(R.string.label_gagal_memuat_rangkuman))) { // Hanya simpan jika berhasil
                    dbHelper.updateRangkuman(articleId, summaryText);
                    currentBerita.setRangkuman(summaryText); // Update objek lokal
                }
                final String finalSummaryText = summaryText;
                mainThreadHandler.post(() -> {
                    displayRangkuman(finalSummaryText);
                    showLoadingRangkuman(false, null);
                });

            } catch (Exception e) {
                Log.e(TAG, "Exception fetching Gemini summary: ", e);
                mainThreadHandler.post(() -> {
                    displayRangkuman(getString(R.string.label_gagal_memuat_rangkuman) + " (" + e.getMessage() + ")");
                    showLoadingRangkuman(false, null);
                });
            }
        });
    }

    private void displayRangkuman(String textRangkuman) {
        binding.textViewIsiRangkuman.setText(textRangkuman);
    }

    private void showLoadingRangkuman(boolean isLoading, String message) {
        if (isLoading) {
            binding.progressBarRangkuman.setVisibility(View.VISIBLE);
            binding.textViewIsiRangkuman.setText(message != null ? message : getString(R.string.label_memuat_rangkuman));
        } else {
            binding.progressBarRangkuman.setVisibility(View.GONE);
            // Teks rangkuman akan di-set oleh displayRangkuman() atau fetchRangkumanFromGemini()
        }
    }

    private void toggleFavorite() {
        if (currentBerita == null) return;

        boolean newFavoriteStatus = !currentBerita.isFavorite();
        executorService.execute(() -> {
            dbHelper.updateFavoriteStatus(articleId, newFavoriteStatus);
            currentBerita.setFavorite(newFavoriteStatus); // Update objek lokal
            mainThreadHandler.post(() -> {
                updateFavoriteFab();
                String message = newFavoriteStatus ? "Ditambahkan ke favorit" : "Dihapus dari favorit";
                Toast.makeText(RangkumanBeritaActivity.this, message, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void updateFavoriteFab() {
        if (currentBerita != null) {
            if (currentBerita.isFavorite()) {
                binding.fabFavoriteRangkuman.setImageResource(R.drawable.ic_baseline_favorite_24);
                // Atur tint jika perlu, sesuai dengan warna tema untuk ikon aktif
                ImageViewCompat.setImageTintList(binding.fabFavoriteRangkuman, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.favorite_active)));

            } else {
                binding.fabFavoriteRangkuman.setImageResource(R.drawable.ic_baseline_favorite_border_24);
                // Atur tint sesuai dengan warna tema untuk ikon non-aktif (colorOnSecondary dari FAB atau warna ikon standar)
                int inactiveColor = ThemeUtils.getThemeColor(this, com.google.android.material.R.attr.colorOnSecondary); // Warna teks/ikon di atas FAB
                ImageViewCompat.setImageTintList(binding.fabFavoriteRangkuman, ColorStateList.valueOf(inactiveColor));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // onBackPressed();
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            binding = null;
        }
        // Shutdown executor service jika tidak lagi dibutuhkan oleh activity lain
        // executorService.shutdown();
    }
}