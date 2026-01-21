package com.example.woil.ui;

import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.example.woil.R;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.Calendar;

public class ProfileSetupActivity extends AppCompatActivity {

    private static final int REQ_IMAGE = 100;

    private EditText etFirstName, etLastName, etAddress, etNic, etDob;
    private TextView tvSelectLocation, tvSkipNic, tvSkillsLabel;
    private ImageButton btnUploadNic;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale;
    private Spinner spinnerSkills;
    private MaterialButton btnSubmit;

    private String nicImageUriString = null;
    private String role = "worker"; // default assume worker; will be overwritten by intent extra

    // modern activity result launcher for picking images
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                Uri selected = result.getData().getData();
                                if (selected != null) {
                                    nicImageUriString = selected.toString();
                                    Toast.makeText(ProfileSetupActivity.this, "NIC photo selected", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etAddress = findViewById(R.id.etAddress);
        tvSelectLocation = findViewById(R.id.tvSelectLocation);
        etNic = findViewById(R.id.etNic);
        btnUploadNic = findViewById(R.id.btnUploadNic);
        tvSkipNic = findViewById(R.id.tvSkipNic);
        rgGender = findViewById(R.id.rgGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        etDob = findViewById(R.id.etDob);
        spinnerSkills = findViewById(R.id.spinnerSkills);
        tvSkillsLabel = findViewById(R.id.tvSkillsLabel);
        btnSubmit = findViewById(R.id.btnSubmitProfile);

        // read role from intent (if passed from SignUp)
        if (getIntent() != null && getIntent().hasExtra("role")) {
            role = getIntent().getStringExtra("role");
        }

        // Show/hide skills for workers only
        if (!"worker".equalsIgnoreCase(role)) {
            spinnerSkills.setVisibility(View.GONE);
            tvSkillsLabel.setVisibility(View.GONE);
        } else {
            spinnerSkills.setVisibility(View.VISIBLE);
            tvSkillsLabel.setVisibility(View.VISIBLE);
        }

        // populate skills spinner
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"Cleaning", "Cooking", "Driving", "Construction", "Electrical", "Other"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSkills.setAdapter(adapter);

        // launch gallery to pick NIC image
        btnUploadNic.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                pickImageLauncher.launch(Intent.createChooser(i, "Select NIC photo"));
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(this, "No app found to pick image", Toast.LENGTH_SHORT).show();
            }
        });

        tvSkipNic.setOnClickListener(v -> {
            nicImageUriString = null;
            Toast.makeText(this, "NIC skipped", Toast.LENGTH_SHORT).show();
        });

        // select location (open maps search). You can replace with a PlacePicker integration later.
        tvSelectLocation.setOnClickListener(v -> {
            String q = etAddress.getText().toString().trim();
            if (TextUtils.isEmpty(q)) {
                q = "my location";
            }
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(q));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            try {
                startActivity(mapIntent);
            } catch (ActivityNotFoundException e) {
                // fallback: open any maps app / browser
                Intent alt = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(q)));
                startActivity(alt);
            }
        });

        // date picker for DOB
        etDob.setOnClickListener(v -> showDatePicker());

        btnSubmit.setOnClickListener(v -> submitProfile());
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String chosen = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
            etDob.setText(chosen);
        }, y, m, d);

        dpd.show();
    }

    private void submitProfile() {
        String first = etFirstName.getText().toString().trim();
        String last = etLastName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String nic = etNic.getText().toString().trim();
        String dob = etDob.getText().toString().trim();
        String gender = (rgGender.getCheckedRadioButtonId() == R.id.rbMale) ? "male" : "female";
        String skill = spinnerSkills.getSelectedItem() != null ? spinnerSkills.getSelectedItem().toString() : "";

        // Basic validation
        if (TextUtils.isEmpty(first)) {
            etFirstName.setError("Enter first name");
            etFirstName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(last)) {
            etLastName.setError("Enter last name");
            etLastName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(address)) {
            etAddress.setError("Enter address");
            etAddress.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(dob)) {
            etDob.setError("Enter date of birth");
            etDob.requestFocus();
            return;
        }

        // If worker ensure skill chosen (optional)
        if ("worker".equalsIgnoreCase(role) && (skill == null || skill.isEmpty())) {
            Toast.makeText(this, "Please choose at least one skill", Toast.LENGTH_SHORT).show();
            return;
        }

        // At this point you have everything. Replace this with your API call / upload logic.
        // Example bundle to send to server:
        // first, last, address, nic (optional), nicImageUriString (optional), gender, dob, skill, role

        Toast.makeText(this, "Profile submitted successfully", Toast.LENGTH_LONG).show();

        // TODO: send to your server, then navigate to next screen. For now finish:
        finish();
    }
}
