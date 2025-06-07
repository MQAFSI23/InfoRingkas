package com.example.inforingkas.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.inforingkas.BuildConfig;
import com.example.inforingkas.R;
import com.example.inforingkas.RangkumanBeritaActivity;
import com.example.inforingkas.databinding.FragmentBeritaTerkiniBinding;
import com.example.inforingkas.db.DatabaseHelper;
import com.example.inforingkas.model.Berita;
import com.example.inforingkas.network.ApiClient;
import com.example.inforingkas.network.ApiService;
import com.example.inforingkas.network.NetworkUtils;
import com.example.inforingkas.network.model.NewsApiResponse;
import com.example.inforingkas.ui.adapter.BeritaAdapter;
import com.example.inforingkas.util.Constants;
import com.example.inforingkas.util.ThemePreferenceHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BeritaTerkiniFragment extends Fragment implements BeritaAdapter.OnBeritaClickListener {

    private static final String TAG = "BeritaTerkiniFragment";
    private FragmentBeritaTerkiniBinding binding;
    private BeritaAdapter beritaAdapter;
    private DatabaseHelper dbHelper;
    private ThemePreferenceHelper themePreferenceHelper;
    private ApiService apiService;

    private final ExecutorService dbExecutorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private static final int MINIMUM_ARTICLES_TARGET = 5;
    private static final int MAX_PAGES_TO_FETCH = 3;

    private boolean isFetching = false;
    private final List<Berita> sessionBeritaList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBeritaTerkiniBinding.inflate(inflater, container, false);
        dbHelper = new DatabaseHelper(requireContext());
        themePreferenceHelper = new ThemePreferenceHelper(requireContext());
        apiService = ApiClient.getClient().create(ApiService.class);
        setupRecyclerView();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkAndFetchBerita();
    }

    private void setupRecyclerView() {
        beritaAdapter = new BeritaAdapter(requireContext(), this);
        binding.recyclerViewBeritaTerkini.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewBeritaTerkini.setAdapter(beritaAdapter);
    }

    private void checkAndFetchBerita() {
        if (isFetching) return;

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.label_gagal_memuat_berita) + " (Tidak ada koneksi)", Toast.LENGTH_LONG).show();
            loadBeritaTerkiniFromDb();
            return;
        }

        String lastFetchDateStr = themePreferenceHelper.getLastFetchDateTerkini();
        String todayDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (!todayDateStr.equals(lastFetchDateStr)) {
            sessionBeritaList.clear();
            beritaAdapter.submitList(new ArrayList<>()); // Kosongkan UI sebelum fetch
            fetchBeritaTerkiniFromApi(null, 1);
        } else {
            Toast.makeText(requireContext(), R.string.label_berita_sudah_terbaru, Toast.LENGTH_SHORT).show();
            loadBeritaTerkiniFromDb();
        }
    }

    private void fetchBeritaTerkiniFromApi(@Nullable String pageId, int pageFetchCount) {
        isFetching = true;
        showLoading(true, getString(R.string.label_memperbarui_berita));

        if (pageId == null) {
            dbExecutorService.execute(() -> dbHelper.clearIsTerkiniFlags());
        }

        Call<NewsApiResponse> call = apiService.getLatestNews(BuildConfig.NEWS_API_KEY, "id", pageId);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<NewsApiResponse> call, @NonNull Response<NewsApiResponse> response) {
                isFetching = false;
                if (response.isSuccessful() && response.body() != null) {
                    handleSuccessfulResponse(response.body(), pageFetchCount);
                } else {
                    handleApiError("Response not successful. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<NewsApiResponse> call, @NonNull Throwable t) {
                isFetching = false;
                Log.e(TAG, "API call failed: ", t);
                handleApiError(t.getLocalizedMessage());
            }
        });
    }

    private void handleSuccessfulResponse(NewsApiResponse apiResponse, int pageFetchCount) {
        List<Berita> indonesianBerita = new ArrayList<>();
        if (apiResponse.getResults() != null) {
            for (Berita berita : apiResponse.getResults()) {
                if (berita != null && "indonesian".equalsIgnoreCase(berita.getLanguage())) {
                    indonesianBerita.add(berita);
                }
            }
        }

        if (!indonesianBerita.isEmpty()) {
            dbExecutorService.execute(() -> dbHelper.addAllBerita(indonesianBerita, true));
            sessionBeritaList.addAll(indonesianBerita);
        }

        // PERBAIKAN: Selalu update UI dengan list sesi terbaru
        updateUiWithBerita(sessionBeritaList);

        boolean shouldFetchMore = sessionBeritaList.size() < MINIMUM_ARTICLES_TARGET &&
                apiResponse.getNextPage() != null &&
                pageFetchCount < MAX_PAGES_TO_FETCH;

        if (shouldFetchMore) {
            Log.d(TAG, "Belum cukup berita. Fetching halaman ke-" + (pageFetchCount + 1));
            fetchBeritaTerkiniFromApi(apiResponse.getNextPage(), pageFetchCount + 1);
        } else {
            Log.d(TAG, "Fetching dihentikan. Total berita didapat: " + sessionBeritaList.size());
            if (!sessionBeritaList.isEmpty()) {
                themePreferenceHelper.setLastFetchDateTerkini(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
            }
        }
    }

    private void handleApiError(String errorMessage) {
        mainThreadHandler.post(() -> {
            if (binding == null) return;
            updateUiWithBerita(new ArrayList<>());
            binding.textViewInfoTerkini.setText(getString(R.string.label_gagal_memuat_berita) + ": " + errorMessage);
            Toast.makeText(requireContext(), getString(R.string.label_gagal_memuat_berita), Toast.LENGTH_LONG).show();
        });
    }

    private void loadBeritaTerkiniFromDb() {
        showLoading(true, getString(R.string.label_memuat_berita));
        dbExecutorService.execute(() -> {
            List<Berita> beritaList = dbHelper.getBeritaTerkiniFromDb();
            mainThreadHandler.post(() -> updateUiWithBerita(beritaList));
        });
    }

    private void updateUiWithBerita(List<Berita> beritaList) {
        if (binding == null) {
            return;
        }

        showLoading(false, null);
        beritaAdapter.submitList(beritaList);

        if (beritaList.isEmpty()) {
            binding.textViewInfoTerkini.setText(R.string.label_tidak_ada_berita);
            binding.textViewInfoTerkini.setVisibility(View.VISIBLE);
            binding.recyclerViewBeritaTerkini.setVisibility(View.GONE);
        } else {
            binding.textViewInfoTerkini.setVisibility(View.GONE);
            binding.recyclerViewBeritaTerkini.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean isLoading, @Nullable String message) {
        if (binding == null) {
            return;
        }
        if (isLoading && beritaAdapter.getItemCount() == 0) {
            binding.progressBarTerkini.setVisibility(View.VISIBLE);
            if (message != null) {
                binding.textViewInfoTerkini.setText(message);
                binding.textViewInfoTerkini.setVisibility(View.VISIBLE);
            }
            binding.recyclerViewBeritaTerkini.setVisibility(View.GONE);
        } else {
            binding.progressBarTerkini.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLihatBeritaClick(Berita berita) {
        if (berita.getLink() != null && !berita.getLink().isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(berita.getLink()));
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(requireContext(), "Tidak ada browser ditemukan", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "Link berita tidak tersedia", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRangkumBeritaClick(Berita berita) {
        Intent intent = new Intent(requireActivity(), RangkumanBeritaActivity.class);
        intent.putExtra(Constants.EXTRA_ARTICLE_ID, berita.getArticleId());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Berita berita, int position) {
        boolean newFavoriteStatus = !berita.isFavorite();
        dbExecutorService.execute(() -> {
            dbHelper.updateFavoriteStatus(berita.getArticleId(), newFavoriteStatus);
            mainThreadHandler.post(() -> {
                berita.setFavorite(newFavoriteStatus);
                beritaAdapter.notifyItemChanged(position, "payload_favorite_changed");
                String message = newFavoriteStatus ? "Ditambahkan ke favorit" : "Dihapus dari favorit";
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if(beritaAdapter.getItemCount() == 0 && !isFetching) {
            loadBeritaTerkiniFromDb();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}