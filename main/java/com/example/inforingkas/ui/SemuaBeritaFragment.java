package com.example.inforingkas.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.inforingkas.databinding.FragmentSemuaBeritaBinding;
import com.example.inforingkas.db.DatabaseHelper;
import com.example.inforingkas.model.Berita;
import com.example.inforingkas.ui.adapter.BeritaAdapter;
import com.example.inforingkas.util.Constants;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SemuaBeritaFragment extends Fragment implements BeritaAdapter.OnBeritaClickListener {

    private FragmentSemuaBeritaBinding binding;
    private BeritaAdapter beritaAdapter;
    private DatabaseHelper dbHelper;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSemuaBeritaBinding.inflate(inflater, container, false);
        dbHelper = new DatabaseHelper(requireContext());
        setupRecyclerView();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Load data when view is created
    }

    @Override
    public void onResume() {
        super.onResume();
        // Selalu muat ulang data dari DB saat fragment resumed untuk memastikan data terbaru
        loadSemuaBeritaFromDb();
    }

    private void setupRecyclerView() {
        beritaAdapter = new BeritaAdapter(requireContext(), this);
        binding.recyclerViewSemuaBerita.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewSemuaBerita.setAdapter(beritaAdapter);
    }

    private void loadSemuaBeritaFromDb() {
        showLoading(true);
        executorService.execute(() -> {
            List<Berita> beritaList = dbHelper.getAllBerita(); // Ambil semua berita
            mainThreadHandler.post(() -> {
                showLoading(false);
                if (beritaList == null || beritaList.isEmpty()) {
                    binding.textViewInfoSemua.setText(R.string.label_tidak_ada_berita);
                    binding.textViewInfoSemua.setVisibility(View.VISIBLE);
                    binding.recyclerViewSemuaBerita.setVisibility(View.GONE);
                } else {
                    binding.textViewInfoSemua.setVisibility(View.GONE);
                    binding.recyclerViewSemuaBerita.setVisibility(View.VISIBLE);
                }
                beritaAdapter.setBeritaList(beritaList);
            });
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBarSemua.setVisibility(View.VISIBLE);
            binding.textViewInfoSemua.setVisibility(View.GONE); // Sembunyikan teks info saat loading
            binding.recyclerViewSemuaBerita.setVisibility(View.GONE);
        } else {
            binding.progressBarSemua.setVisibility(View.GONE);
            // Visibilitas textViewInfoSemua dan recyclerViewSemuaBerita diatur setelah data dimuat
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
        executorService.execute(() -> {
            dbHelper.updateFavoriteStatus(berita.getArticleId(), newFavoriteStatus);
            mainThreadHandler.post(() -> {
                // Update item di adapter
                berita.setFavorite(newFavoriteStatus);
                beritaAdapter.notifyItemChanged(position, "payload_favorite_changed");
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
}