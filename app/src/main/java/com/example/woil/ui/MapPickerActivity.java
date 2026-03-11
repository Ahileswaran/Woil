package com.example.woil.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.woil.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private String selectedAddress = "";

    private TextView tvAddress;
    private Button btnConfirm;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    if (mMap != null) tryEnableMyLocation();
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        tvAddress = findViewById(R.id.tv_selected_address);
        btnConfirm = findViewById(R.id.btn_confirm_location);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        btnConfirm.setOnClickListener(v -> {
            if (selectedLatLng == null) {
                Toast.makeText(this, "Tap map to select a location", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent result = new Intent();
            result.putExtra("lat", selectedLatLng.latitude);
            result.putExtra("lng", selectedLatLng.longitude);
            result.putExtra("address", selectedAddress != null ? selectedAddress : "");
            setResult(Activity.RESULT_OK, result);
            finish();
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Center on Colombo by default (change as needed)
        LatLng defaultCity = new LatLng(6.9271, 79.8612);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultCity, 12f));

        tryEnableMyLocation();

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng));
            selectedLatLng = latLng;
            selectedAddress = reverseGeocode(latLng.latitude, latLng.longitude);
            tvAddress.setText(selectedAddress != null && !selectedAddress.isEmpty() ? selectedAddress :
                    String.format(Locale.getDefault(), "%.6f, %.6f", latLng.latitude, latLng.longitude));
        });
    }

    private void tryEnableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) mMap.setMyLocationEnabled(true);
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private String reverseGeocode(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address a = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                if (a.getThoroughfare() != null) sb.append(a.getThoroughfare()).append(", ");
                if (a.getSubLocality() != null) sb.append(a.getSubLocality()).append(", ");
                if (a.getLocality() != null) sb.append(a.getLocality()).append(", ");
                if (a.getAdminArea() != null) sb.append(a.getAdminArea()).append(", ");
                if (a.getCountryName() != null) sb.append(a.getCountryName());
                return sb.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}