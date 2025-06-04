package com.example.inforingkas;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.inforingkas.databinding.ActivityMainBinding;
import com.example.inforingkas.util.Constants;
import com.example.inforingkas.util.ThemePreferenceHelper;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private ThemePreferenceHelper themePreferenceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inisialisasi ThemePreferenceHelper dan terapkan tema sebelum setContentView
        themePreferenceHelper = new ThemePreferenceHelper(this);
        // Jika Anda tidak menggunakan Application class, panggil applyThemeOnAppStart di sini
        // themePreferenceHelper.applyThemeOnAppStart(); // Atau panggil ThemePreferenceHelper.applyTheme(themePreferenceHelper.getThemeMode());

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Toolbar
        Toolbar toolbar = binding.toolbarMain; // Menggunakan ID dari activity_main.xml
        setSupportActionBar(toolbar);

        // Setup NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_container);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        // Setup AppBarConfiguration: Menentukan top-level destinations
        // agar tombol up/back tidak muncul di fragment ini.
        Set<Integer> topLevelDestinations = new HashSet<>();
        topLevelDestinations.add(R.id.nav_berita_terkini);
        topLevelDestinations.add(R.id.nav_semua_berita);
        topLevelDestinations.add(R.id.nav_berita_favorit);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations).build();

        // Link NavController dengan Toolbar
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);

        // Link NavController dengan BottomNavigationView
        NavigationUI.setupWithNavController(binding.bottomNavigationView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_change_theme) {
            showThemeChooserDialog();
            return true;
        }
        // Biarkan NavigationUI menangani item lain (seperti tombol Up)
        return NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item);
    }

    private void showThemeChooserDialog() {
        final String[] themes = {
                getString(R.string.theme_light),
                getString(R.string.theme_dark),
                getString(R.string.theme_system)
        };
        final String[] themeValues = {
                Constants.THEME_LIGHT,
                Constants.THEME_DARK,
                Constants.THEME_SYSTEM
        };

        String currentThemeValue = themePreferenceHelper.getThemeMode();
        int checkedItem = 0; // Default to Light
        for (int i = 0; i < themeValues.length; i++) {
            if (themeValues[i].equals(currentThemeValue)) {
                checkedItem = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_title_pilih_tema);
        builder.setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
            themePreferenceHelper.setThemeMode(themeValues[which]);
            dialog.dismiss();
            // Recreate activity untuk menerapkan tema baru
            // Ini cara sederhana, cara yang lebih smooth mungkin melibatkan listener
            // atau event bus untuk memberitahu komponen UI agar menggambar ulang.
            recreate();
        });
        builder.create().show();
    }

    // Override ini jika Anda ingin NavController menangani tombol back sistem
    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, (AppBarConfiguration) null) || super.onSupportNavigateUp();
    }
}