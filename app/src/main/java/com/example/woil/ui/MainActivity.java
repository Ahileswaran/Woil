package com.example.woil.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import com.example.woil.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       // BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
       // NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        // Set up navigation with the bottom nav
     //   NavigationUI.setupWithNavController(bottomNavigationView, navController);

        // Toolbar (optional)
        //Toolbar toolbar = findViewById(R.id.topToolbar);
       // if (toolbar != null) {
         //   setSupportActionBar(toolbar);
      //  }

        // Remove BottomAppBar/FAB code if not using it; or initialize if layout has it
        // bottomAppBar = findViewById(R.id.bottom_app_bar); // If present in layout
        // fab = findViewById(R.id.fab);
        // menuCount = bottomAppBar.getMenu().size(); // If using BottomAppBar instead
    }
}