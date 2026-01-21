package com.example.woil.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.example.woil.R;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.Calendar;
import java.util.Locale;

public class PostJobActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etJobTitle, etWage, etDescription;
    private TextView tvCategoryValue, tvMapAddress, tvSetLocation, tvDate, tvTimeRange;
    private ImageView imgMapPreview;
    private Switch switchAllowOffers;
    private MaterialButton btnPostJob;

    private String selectedCategory = "Housekeeping";
    private String selectedDate = "";
    private String timeRange = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.post_job);

        btnBack = findViewById(R.id.btnBack);
        etJobTitle = findViewById(R.id.etJobTitle);
        etWage = findViewById(R.id.etWage);
        etDescription = findViewById(R.id.etDescription);
        tvCategoryValue = findViewById(R.id.tvCategoryValue);
        tvMapAddress = findViewById(R.id.tvMapAddress);
        tvSetLocation = findViewById(R.id.tvSetLocation);
        tvDate = findViewById(R.id.tvDate);
        tvTimeRange = findViewById(R.id.tvTimeRange);
        imgMapPreview = findViewById(R.id.imgMapPreview);
        switchAllowOffers = findViewById(R.id.switchAllowOffers);
        btnPostJob = findViewById(R.id.btnPostJob);

        tvCategoryValue.setText(selectedCategory);

        btnBack.setOnClickListener(v -> finish());

        // pick category -> simple dialog
        findViewById(R.id.llCategory).setOnClickListener(v -> showCategoryPicker());

        // set location -> open maps
        findViewById(R.id.cardLocationPreview).setOnClickListener(v -> openMapsForAddress(tvMapAddress.getText().toString()));

        tvSetLocation.setOnClickListener(v -> openMapsForAddress(tvMapAddress.getText().toString()));

        // date picker
        findViewById(R.id.tvDate).setOnClickListener(v -> showDatePicker());

        // time range: pick start then end
        findViewById(R.id.tvTimeRange).setOnClickListener(v -> pickTimeRange());

        btnPostJob.setOnClickListener(v -> {
            if (validateForm()) {
                postJob();
            }
        });

        // map preview click also opens Maps
        imgMapPreview.setOnClickListener(v -> openMapsForAddress(tvMapAddress.getText().toString()));
    }

    private void showCategoryPicker() {
        final String[] categories = new String[]{"Housekeeping", "Cleaning", "Cooking", "Gardening", "Delivery", "Handyman"};
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Select Category");
        b.setItems(categories, (dialog, which) -> {
            selectedCategory = categories[which];
            tvCategoryValue.setText(selectedCategory);
        });
        b.show();
    }

    private void openMapsForAddress(String address) {
        String query = TextUtils.isEmpty(address) ? "Colombo" : address;
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(mapIntent);
        } catch (ActivityNotFoundException e) {
            // fallback to browser
            Intent alt = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query)));
            startActivity(alt);
        }
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dpd = new DatePickerDialog(this, (DatePicker view, int year, int monthOfYear, int dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%02d %s, %04d", dayOfMonth, getMonthShort(monthOfYear), year);
            tvDate.setText(selectedDate);
        }, y, m, d);
        dpd.show();
    }

    private String getMonthShort(int m) {
        final String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        if (m >= 0 && m < months.length) return months[m];
        return "";
    }

    private void pickTimeRange() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // start time
        TimePickerDialog startPicker = new TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            String start = formatTime(hourOfDay, minute1);
            // end time picker
            TimePickerDialog endPicker = new TimePickerDialog(this, (v2, hourOfDay2, minute2) -> {
                String end = formatTime(hourOfDay2, minute2);
                timeRange = start + " - " + end;
                tvTimeRange.setText(timeRange);
            }, hour, minute, false);
            endPicker.setTitle("Select end time");
            endPicker.show();
        }, hour, minute, false);
        startPicker.setTitle("Select start time");
        startPicker.show();
    }

    private String formatTime(int h, int m) {
        boolean pm = h >= 12;
        int hh = h % 12;
        if (hh == 0) hh = 12;
        return String.format(Locale.getDefault(), "%02d:%02d %s", hh, m, pm ? "PM" : "AM");
    }

    private boolean validateForm() {
        String title = etJobTitle.getText().toString().trim();
        String wage = etWage.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etJobTitle.setError("Enter job title");
            etJobTitle.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(tvDate.getText())) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(tvTimeRange.getText())) {
            Toast.makeText(this, "Please select time range", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(wage)) {
            etWage.setError("Enter wage");
            etWage.requestFocus();
            return false;
        }
        return true;
    }

    private void postJob() {
        // collect form values
        String title = etJobTitle.getText().toString().trim();
        String category = selectedCategory;
        String address = tvMapAddress.getText().toString().trim();
        String date = tvDate.getText().toString().trim();
        String timer = tvTimeRange.getText().toString().trim();
        String wage = etWage.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        boolean allowOffers = switchAllowOffers.isChecked();

        // TODO: call your backend API here (Retrofit or Firebase)
        // For now just show a Toast
        Toast.makeText(this, "Job posted:\n" + title + " | " + category + "\n" + date + " " + timer, Toast.LENGTH_LONG).show();

        // finish or navigate to job list
        finish();
    }
}
