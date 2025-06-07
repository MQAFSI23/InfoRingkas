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
import com.example.inforingkas.databinding.FragmentBeritaFavoritBinding;
import com.example.inforingkas.db.DatabaseHelper;
import com.example.inforingkas.model.Berita;
import com.example.inforingkas.ui.adapter.BeritaAdapter;
import com.example.inforingkas.util.Constants;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BeritaFavoritFragment extends Fragment implements BeritaAdapter.OnBeritaClickListener {

    private FragmentBeritaFavoritBinding binding;
    private BeritaAdapter beritaAdapter;
    private DatabaseHelper dbHelper;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBeritaFavoritBinding.inflate(inflater, container, false);
        dbHelper = new DatabaseHelper(requireContext());
        setupRecyclerView();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBeritaFavoritFromDb();
    }

    private void setupRecyclerView() {
        beritaAdapter = new BeritaAdapter(requireContext(), this);
        binding.recyclerViewBeritaFavorit.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewBeritaFavorit.setAdapter(beritaAdapter);
    }

    private void loadBeritaFavoritFromDb() {
        showLoading(true);
        executorService.execute(() -> {
            List<Berita> beritaList = dbHelper.getFavoriteBerita();
            mainThreadHandler.post(() -> {
                showLoading(false);
                if (binding == null) return;
                beritaAdapter.submitList(beritaList);

                if (beritaList.isEmpty()) {
                    binding.textViewInfoFavorit.setText(R.string.label_tidak_ada_berita);
                    binding.textViewInfoFavorit.setVisibility(View.VISIBLE);
                    binding.recyclerViewBeritaFavorit.setVisibility(View.GONE);
                } else {
                    binding.textViewInfoFavorit.setVisibility(View.GONE);
                    binding.recyclerViewBeritaFavorit.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBarFavorit.setVisibility(View.VISIBLE);
            binding.textViewInfoFavorit.setVisibility(View.GONE);
            binding.recyclerViewBeritaFavorit.setVisibility(View.GONE);
        } else {
            binding.progressBarFavorit.setVisibility(View.GONE);
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
        boolean newFavoriteStatus = false;
        executorService.execute(() -> {
            dbHelper.updateFavoriteStatus(berita.getArticleId(), newFavoriteStatus);
            mainThreadHandler.post(() -> {
                loadBeritaFavoritFromDb();
                Toast.makeText(requireContext(), "Dihapus dari favorit", Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
