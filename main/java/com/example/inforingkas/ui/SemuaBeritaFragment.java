package com.example.inforingkas.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
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

    private List<Berita> masterBeritaList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSemuaBeritaBinding.inflate(inflater, container, false);
        dbHelper = new DatabaseHelper(requireContext());
        setupRecyclerView();
        setupMenu();
        return binding.getRoot();
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

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.search_menu, menu);

                MenuItem searchItem = menu.findItem(R.id.action_search);
                SearchView searchView = (SearchView) searchItem.getActionView();

                assert searchView != null;
                searchView.setQueryHint(getString(R.string.menu_search));

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        searchView.clearFocus();
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        filterBerita(newText);
                        return true;
                    }
                });
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void setupSearchView(SearchView searchView, MenuItem searchItem) {
        searchView.setQueryHint(getString(R.string.menu_search));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterBerita(newText);
                return true;
            }
        });

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                filterBerita("");
                return true;
            }
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
        showLoading(false);
        beritaAdapter.submitList(beritaList);

        if (beritaList.isEmpty() && masterBeritaList.isEmpty()) {
            binding.textViewInfoSemua.setText(R.string.label_tidak_ada_berita);
            binding.textViewInfoSemua.setVisibility(View.VISIBLE);
            binding.recyclerViewSemuaBerita.setVisibility(View.GONE);
        } else {
            binding.textViewInfoSemua.setVisibility(View.GONE);
            binding.recyclerViewSemuaBerita.setVisibility(View.VISIBLE);
        }
    }

    private void loadSemuaBeritaFromDb() {
        showLoading(true);
        executorService.execute(() -> {
            List<Berita> beritaFromDb = dbHelper.getAllBerita();
            mainThreadHandler.post(() -> {
                masterBeritaList.clear();
                masterBeritaList.addAll(beritaFromDb);
                updateUiWithBerita(new ArrayList<>(masterBeritaList));
            });
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBarSemua.setVisibility(View.VISIBLE);
            binding.textViewInfoSemua.setVisibility(View.GONE);
            binding.recyclerViewSemuaBerita.setVisibility(View.GONE);
        } else {
            binding.progressBarSemua.setVisibility(View.GONE);
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
        String message = newFavoriteStatus ? "Ditambahkan ke favorit" : "Dihapus dari favorit";
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            dbHelper.updateFavoriteStatus(berita.getArticleId(), newFavoriteStatus);
            mainThreadHandler.post(() -> {
                for (Berita masterBerita : masterBeritaList) {
                    if (masterBerita.getArticleId().equals(berita.getArticleId())) {
                        masterBerita.setFavorite(newFavoriteStatus);
                        break;
                    }
                }
                beritaAdapter.updateFavoriteStatus(position, newFavoriteStatus);
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}