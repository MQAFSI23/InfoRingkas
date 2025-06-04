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
import com.example.inforingkas.R;
import com.example.inforingkas.RangkumanBeritaActivity;
import com.example.inforingkas.databinding.FragmentBeritaTerkiniBinding;
import com.example.inforingkas.db.DatabaseHelper;
import com.example.inforingkas.model.Berita;
import com.example.inforingkas.network.NetworkUtils;
import com.example.inforingkas.ui.adapter.BeritaAdapter;
import com.example.inforingkas.util.Constants;
import com.example.inforingkas.util.ThemePreferenceHelper; // Menggunakan ini untuk SharedPreferences
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BeritaTerkiniFragment extends Fragment implements BeritaAdapter.OnBeritaClickListener {

    private static final String TAG = "BeritaTerkiniFragment";
    private FragmentBeritaTerkiniBinding binding;
    private BeritaAdapter beritaAdapter;
    private DatabaseHelper dbHelper;
    private ThemePreferenceHelper themePreferenceHelper; // Untuk akses SharedPreferences
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private static final int JUMLAH_BERITA_PER_FETCH = 10;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBeritaTerkiniBinding.inflate(inflater, container, false);
        dbHelper = new DatabaseHelper(requireContext());
        themePreferenceHelper = new ThemePreferenceHelper(requireContext()); // Inisialisasi

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
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.label_gagal_memuat_berita) + " (Tidak ada koneksi)", Toast.LENGTH_LONG).show();
            loadBeritaTerkiniFromDb(); // Coba load dari DB jika offline
            return;
        }

        String lastFetchDateStr = themePreferenceHelper.getLastFetchDateTerkini();
        String todayDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Log.d(TAG, "Last fetch date: " + lastFetchDateStr + ", Today's date: " + todayDateStr);

        if (!todayDateStr.equals(lastFetchDateStr)) {
            Log.d(TAG, "Tanggal berbeda atau belum pernah fetch. Mengambil berita baru dari API.");
            fetchBeritaTerkiniFromApi(null); // Mulai tanpa nextPageId
        } else {
            Log.d(TAG, "Tanggal sama. Memuat berita dari DB dan menampilkan toast.");
            Toast.makeText(requireContext(), R.string.label_berita_sudah_terbaru, Toast.LENGTH_SHORT).show();
            loadBeritaTerkiniFromDb();
        }
    }

    private void fetchBeritaTerkiniFromApi(String nextPageId) {
        showLoading(true, getString(R.string.label_memperbarui_berita));
        final boolean isInitialFetch = (nextPageId == null);

        executorService.execute(() -> {
            try {
                String jsonResponse = NetworkUtils.fetchNewsData(nextPageId);
                Log.d(TAG, "API Response: " + jsonResponse.substring(0, Math.min(jsonResponse.length(), 500))); // Log sebagian response

                JSONObject responseObject = new JSONObject(jsonResponse);
                String status = responseObject.optString("status");

                if ("success".equals(status)) {
                    JSONArray resultsArray = responseObject.optJSONArray("results");
                    List<Berita> fetchedBeritaList = new ArrayList<>();
                    if (resultsArray != null) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            JSONObject beritaJson = resultsArray.getJSONObject(i);
                            // Filter hanya berita berbahasa Indonesia
                            if ("indonesian".equalsIgnoreCase(beritaJson.optString("language"))) {
                                Berita berita = Berita.fromJson(beritaJson);
                                fetchedBeritaList.add(berita);
                            }
                        }
                    }

                    // Jika ini adalah fetch awal (bukan pagination), bersihkan flag terkini sebelumnya
                    if (isInitialFetch) {
                        dbHelper.clearIsTerkiniFlags();
                    }

                    // Tambahkan berita baru ke DB dengan flag is_terkini = true
                    dbHelper.addAllBerita(fetchedBeritaList, true);

                    // Cek apakah sudah cukup 10 berita atau perlu fetch nextPage
                    List<Berita> currentTerkiniInDb = dbHelper.getBeritaTerkiniFromDb();
                    String newNextPageId = responseObject.optString("nextPage", null);

                    if (currentTerkiniInDb.size() < JUMLAH_BERITA_PER_FETCH && !newNextPageId.isEmpty()) {
                        Log.d(TAG, "Belum cukup berita ("+ currentTerkiniInDb.size() +"), fetch nextPage: " + newNextPageId);
                        // Rekursif panggil untuk halaman berikutnya
                        // Penting: pastikan ada mekanisme untuk menghentikan rekursi jika tidak ada berita baru
                        // atau jika API terus memberikan nextPage tanpa hasil yang relevan.
                        mainThreadHandler.post(() -> fetchBeritaTerkiniFromApi(newNextPageId));
                    } else {
                        // Sudah cukup berita atau tidak ada nextPage lagi
                        if (isInitialFetch || currentTerkiniInDb.size() >= JUMLAH_BERITA_PER_FETCH) {
                            themePreferenceHelper.setLastFetchDateTerkini(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
                        }
                        mainThreadHandler.post(() -> {
                            loadBeritaTerkiniFromDb();
                            showLoading(false, null);
                            if (fetchedBeritaList.isEmpty() && isInitialFetch) {
                                binding.textViewInfoTerkini.setText(R.string.label_tidak_ada_berita);
                                binding.textViewInfoTerkini.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                } else {
                    String message = responseObject.optString("message", getString(R.string.label_gagal_memuat_berita));
                    Log.e(TAG, "API Error: " + message);
                    mainThreadHandler.post(() -> {
                        showLoading(false, null);
                        binding.textViewInfoTerkini.setText(getString(R.string.label_gagal_memuat_berita) + ": " + message);
                        binding.textViewInfoTerkini.setVisibility(View.VISIBLE);
                        Toast.makeText(requireContext(), getString(R.string.label_gagal_memuat_berita) + ": " + message, Toast.LENGTH_LONG).show();
                        loadBeritaTerkiniFromDb(); // Coba load dari DB jika API gagal
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception during API fetch: ", e);
                mainThreadHandler.post(() -> {
                    showLoading(false, null);
                    binding.textViewInfoTerkini.setText(R.string.label_gagal_memuat_berita);
                    binding.textViewInfoTerkini.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), R.string.label_gagal_memuat_berita, Toast.LENGTH_LONG).show();
                    loadBeritaTerkiniFromDb(); // Coba load dari DB jika ada exception
                });
            }
        });
    }


    private void loadBeritaTerkiniFromDb() {
        showLoading(true, getString(R.string.label_memuat_berita));
        executorService.execute(() -> {
            List<Berita> beritaList = dbHelper.getBeritaTerkiniFromDb();
            // Ambil hanya 10 berita teratas jika lebih dari itu
            List<Berita> displayList = new ArrayList<>();
            if (beritaList != null) {
                for (int i = 0; i < Math.min(beritaList.size(), JUMLAH_BERITA_PER_FETCH); i++) {
                    displayList.add(beritaList.get(i));
                }
            }

            mainThreadHandler.post(() -> {
                showLoading(false, null);
                if (displayList.isEmpty()) {
                    binding.textViewInfoTerkini.setText(R.string.label_tidak_ada_berita);
                    binding.textViewInfoTerkini.setVisibility(View.VISIBLE);
                    binding.recyclerViewBeritaTerkini.setVisibility(View.GONE);
                } else {
                    binding.textViewInfoTerkini.setVisibility(View.GONE);
                    binding.recyclerViewBeritaTerkini.setVisibility(View.VISIBLE);
                }
                beritaAdapter.setBeritaList(displayList);
            });
        });
    }

    private void showLoading(boolean isLoading, @Nullable String message) {
        if (isLoading) {
            binding.progressBarTerkini.setVisibility(View.VISIBLE);
            if (message != null) {
                binding.textViewInfoTerkini.setText(message);
                binding.textViewInfoTerkini.setVisibility(View.VISIBLE);
            } else {
                binding.textViewInfoTerkini.setVisibility(View.GONE);
            }
            binding.recyclerViewBeritaTerkini.setVisibility(View.GONE);
        } else {
            binding.progressBarTerkini.setVisibility(View.GONE);
            // Info text visibility diatur oleh loadBeritaTerkiniFromDb atau fetchBeritaTerkiniFromApi
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
        // intent.putExtra(Constants.EXTRA_ARTICLE_LINK, berita.getLink()); // Kirim link jika diperlukan
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Berita berita, int position) {
        boolean newFavoriteStatus = !berita.isFavorite();
        executorService.execute(() -> {
            dbHelper.updateFavoriteStatus(berita.getArticleId(), newFavoriteStatus);
            mainThreadHandler.post(() -> {
                berita.setFavorite(newFavoriteStatus); // Update model di adapter
                beritaAdapter.notifyItemChanged(position, "payload_favorite_changed"); // Update item spesifik
                String message = newFavoriteStatus ? "Ditambahkan ke favorit" : "Dihapus dari favorit";
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Hindari memory leak
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data jika kembali ke fragment ini, misalnya setelah merangkum berita
        // atau mengubah status favorit dari activity lain (jika ada).
        // Untuk Berita Terkini, mungkin tidak perlu reload otomatis kecuali jika ada perubahan signifikan.
        // Jika ada perubahan status favorit dari RangkumanBeritaActivity, kita perlu cara untuk merefresh itemnya.
        // Salah satu cara adalah dengan memanggil loadBeritaTerkiniFromDb() atau cara yang lebih spesifik.
        // Atau, RangkumanBeritaActivity bisa mengembalikan result yang menandakan perubahan.
        loadBeritaTerkiniFromDb(); // Muat ulang data dari DB untuk merefleksikan perubahan (misal favorit)
    }
}