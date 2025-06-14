package com.example.inforingkas.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SemuaBeritaFragment extends Fragment implements BeritaAdapter.OnBeritaClickListener {

    private FragmentSemuaBeritaBinding binding;
    private BeritaAdapter beritaAdapter;
    private DatabaseHelper dbHelper;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private final List<Berita> masterBeritaList = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSemuaBeritaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        setupSearchInput();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSemuaBeritaFromDb();
    }

    private void setupRecyclerView() {
        beritaAdapter = new BeritaAdapter(requireContext(), this);
        binding.recyclerViewSemuaBerita.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewSemuaBerita.setAdapter(beritaAdapter);
    }

    private void setupSearchInput() {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBerita(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterBerita(String query) {
        if (binding == null) return;

        List<Berita> filteredList = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(masterBeritaList);
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.ROOT);
            for (Berita berita : masterBeritaList) {
                if (berita.getTitle().toLowerCase(Locale.ROOT).contains(lowerCaseQuery)) {
                    filteredList.add(berita);
                }
            }
        }
        updateUiWithBerita(filteredList);
    }

    private void updateUiWithBerita(List<Berita> beritaList) {
        if (binding == null) return;

        beritaAdapter.submitList(beritaList);

        if (beritaList.isEmpty() && !masterBeritaList.isEmpty()) {
            binding.textViewInfoSemua.setText(R.string.label_tidak_ada_berita);
            binding.textViewInfoSemua.setVisibility(View.VISIBLE);
            binding.recyclerViewSemuaBerita.setVisibility(View.GONE);
        }
        else if (masterBeritaList.isEmpty()) {
            binding.textViewInfoSemua.setText(R.string.label_tidak_ada_berita);
            binding.textViewInfoSemua.setVisibility(View.VISIBLE);
            binding.recyclerViewSemuaBerita.setVisibility(View.GONE);
        }
        else {
            binding.textViewInfoSemua.setVisibility(View.GONE);
            binding.recyclerViewSemuaBerita.setVisibility(View.VISIBLE);
        }
    }

    private void loadSemuaBeritaFromDb() {
        executorService.execute(() -> {
            List<Berita> beritaFromDb = dbHelper.getAllBerita();
            mainThreadHandler.post(() -> {
                if (binding == null) return;
                masterBeritaList.clear();
                masterBeritaList.addAll(beritaFromDb);
                updateUiWithBerita(new ArrayList<>(masterBeritaList));
            });
        });
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
        String message = newFavoriteStatus ? "Ditambahkan ke favorit" : "Dihapus dari favorit";
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            dbHelper.updateFavoriteStatus(berita.getArticleId(), newFavoriteStatus);
            for (Berita masterBerita : masterBeritaList) {
                if (masterBerita.getArticleId().equals(berita.getArticleId())) {
                    masterBerita.setFavorite(newFavoriteStatus);
                    break;
                }
            }
            mainThreadHandler.post(() -> {
                if (binding != null) {
                    beritaAdapter.updateFavoriteStatus(position, newFavoriteStatus);
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}