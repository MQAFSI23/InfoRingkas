package com.example.inforingkas;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        themePreferenceHelper = new ThemePreferenceHelper(this);
        ThemePreferenceHelper.applyTheme(themePreferenceHelper.getThemeMode());

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbarMain;
        setSupportActionBar(toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_container);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        Set<Integer> topLevelDestinations = new HashSet<>();
        topLevelDestinations.add(R.id.nav_berita_terkini);
        topLevelDestinations.add(R.id.nav_semua_berita);
        topLevelDestinations.add(R.id.nav_berita_favorit);

        appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations).build();

        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);
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
        int checkedItem = 0;
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
            recreate();
        });
        builder.create().show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}