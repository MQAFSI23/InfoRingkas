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
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.example.inforingkas.databinding.ActivityRangkumanBeritaBinding;
import com.example.inforingkas.db.DatabaseHelper;
import com.example.inforingkas.model.Berita;
import com.example.inforingkas.network.ApiClient;
import com.example.inforingkas.network.ApiService;
import com.example.inforingkas.network.NetworkUtils;
import com.example.inforingkas.network.model.GeminiRequest;
import com.example.inforingkas.network.model.GeminiResponse;
import com.example.inforingkas.util.Constants;
import com.example.inforingkas.util.ThemePreferenceHelper;
import com.example.inforingkas.util.ThemeUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RangkumanBeritaActivity extends AppCompatActivity {
    private static final String TAG = "RangkumanBeritaActivity";
    private ActivityRangkumanBeritaBinding binding;
    private DatabaseHelper dbHelper;
    private ThemePreferenceHelper themePreferenceHelper;
    private ApiService apiService;
    private String articleId;
    private Berita currentBerita;
    private static final long MIN_ANIMATION_DURATION_MS = 2200;
    private long fetchStartTime;
    private final ExecutorService dbExecutorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        themePreferenceHelper = new ThemePreferenceHelper(this);
        ThemePreferenceHelper.applyTheme(themePreferenceHelper.getThemeMode());

        binding = ActivityRangkumanBeritaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        setupToolbar();

        articleId = getIntent().getStringExtra(Constants.EXTRA_ARTICLE_ID);
        if (articleId == null || articleId.isEmpty()) {
            Toast.makeText(this, "ID Artikel tidak valid.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupListeners();
        loadBeritaAndRangkuman();
    }

    private void setupToolbar() {
        Toolbar toolbar = binding.toolbarRangkuman;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.label_rangkuman_berita);
        }
    }

    private void setupListeners() {
        binding.fabFavoriteRangkuman.setOnClickListener(v -> toggleFavorite());
        binding.buttonMuatUlangRangkuman.setOnClickListener(v -> fetchRangkumanFromGemini());
    }

    private void loadBeritaAndRangkuman() {
        showLoadingRangkuman(true, getString(R.string.label_memuat_berita));
        dbExecutorService.execute(() -> {
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
            handleFailure(getString(R.string.label_gagal_memuat_rangkuman) + " (Link berita tidak tersedia)");
            showLoadingRangkuman(false, null);
            return;
        }

        showLoadingRangkuman(true, getString(R.string.label_memuat_rangkuman));

        fetchStartTime = System.currentTimeMillis();

        if (!NetworkUtils.isNetworkAvailable(this)) {
            processFetchResult(() -> handleFailure(getString(R.string.label_gagal_memuat_rangkuman) + " (Tidak ada koneksi internet)"));
            return;
        }

        String geminiApiKey = BuildConfig.GEMINI_API_KEY;
        String fullUrl = String.format(Constants.GEMINI_API_URL_FORMAT, geminiApiKey);
        String prompt = "Tolong buatkan rangkuman singkat dalam Bahasa Indonesia dari artikel berita yang ada di tautan berikut: " + currentBerita.getLink() +
                "\nCatatan: langsung kirim rangkumannya saja tanpa ada teks seperti: Berikut adalah rangkuman dari berita.... selain itu, jika berupa langkah-langkah atau poin-poin, tidak usah kasih bold atau miring tulisannya, karena jelek saat di-copy";
        GeminiRequest requestBody = new GeminiRequest(prompt);

        Call<GeminiResponse> call = apiService.getGeminiSummary(fullUrl, requestBody);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<GeminiResponse> call, @NonNull Response<GeminiResponse> response) {
                processFetchResult(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        String summaryText = response.body().getSummaryText();
                        if (summaryText != null) {
                            displayRangkuman(summaryText);
                            dbExecutorService.execute(() -> {
                                dbHelper.updateRangkuman(articleId, summaryText);
                                currentBerita.setRangkuman(summaryText);
                            });
                        } else {
                            handleFailure(getString(R.string.label_gagal_memuat_rangkuman) + " (Respons kosong)");
                        }
                    } else {
                        Log.e(TAG, "Gemini API Error Code: " + response.code());
                        handleFailure(getString(R.string.label_gagal_memuat_rangkuman));
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<GeminiResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Gemini API call failed: ", t);
                processFetchResult(() -> handleFailure(getString(R.string.label_gagal_memuat_rangkuman) + " (" + t.getMessage() + ")"));
            }
        });
    }

    private void processFetchResult(Runnable resultAction) {
        if (binding == null) return;

        long elapsedTime = System.currentTimeMillis() - fetchStartTime;

        if (elapsedTime < MIN_ANIMATION_DURATION_MS) {
            long delayNeeded = MIN_ANIMATION_DURATION_MS - elapsedTime;
            mainThreadHandler.postDelayed(() -> {
                showLoadingRangkuman(false, null);
                resultAction.run();
            }, delayNeeded);
        } else {
            showLoadingRangkuman(false, null);
            resultAction.run();
        }
    }

    private void displayRangkuman(String textRangkuman) {
        if (binding == null) return;
        binding.buttonMuatUlangRangkuman.setVisibility(View.GONE);
        binding.textViewIsiRangkuman.setVisibility(View.VISIBLE);
        binding.textViewIsiRangkuman.setText(textRangkuman);
    }

    private void handleFailure(String errorMessage) {
        if (binding == null) return;
        binding.textViewIsiRangkuman.setText(errorMessage);
        binding.textViewIsiRangkuman.setVisibility(View.VISIBLE);
        binding.buttonMuatUlangRangkuman.setVisibility(View.VISIBLE);
    }

    private void showLoadingRangkuman(boolean isLoading, String message) {
        if (isLoading) {
            binding.buttonMuatUlangRangkuman.setVisibility(View.GONE);
            binding.progressBarRangkuman.setVisibility(View.VISIBLE);
            binding.progressBarRangkuman.playAnimation();
            binding.textViewIsiRangkuman.setText(message != null ? message : getString(R.string.label_memuat_rangkuman));
        } else {
            binding.progressBarRangkuman.cancelAnimation();
            binding.progressBarRangkuman.setVisibility(View.GONE);
        }
    }

    private void toggleFavorite() {
        if (currentBerita == null) return;

        boolean newFavoriteStatus = !currentBerita.isFavorite();
        dbExecutorService.execute(() -> {
            dbHelper.updateFavoriteStatus(articleId, newFavoriteStatus);
            currentBerita.setFavorite(newFavoriteStatus);
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
                ImageViewCompat.setImageTintList(binding.fabFavoriteRangkuman, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.favorite_active)));
            } else {
                binding.fabFavoriteRangkuman.setImageResource(R.drawable.ic_baseline_favorite_border_24);
                int inactiveColor = ThemeUtils.getThemeColor(this, com.google.android.material.R.attr.colorOnSecondary);
                ImageViewCompat.setImageTintList(binding.fabFavoriteRangkuman, ColorStateList.valueOf(inactiveColor));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}