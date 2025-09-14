package com.example.travellog;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.travellog.utils.RegistrationManager;
import com.example.travellog.utils.UserImageSelector;

public class RegistrationActivity extends AppCompatActivity {

    private static final String TAG = "RegistrationActivity";
    protected EditText emailEditText;
    protected EditText passwordEditText;
    UserImageSelector userImageSelector;
    protected EditText et_nickname;
    protected EditText et_age;
    protected Spinner travelLikingSpinner;





    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        et_nickname = findViewById(R.id.et_nickname);
        et_age = findViewById(R.id.et_age);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        travelLikingSpinner = findViewById(R.id.spinner_travel_liking);
        Button registerButton = findViewById(R.id.btn_register);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerButtonClick();

            }
        });
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.travel_liking_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        travelLikingSpinner.setAdapter(adapter);
        ImageView profilePictureImageView = findViewById(R.id.iv_profile_picture);
        userImageSelector = new UserImageSelector(this, profilePictureImageView);
        Button choosePictureButton = findViewById(R.id.btn_choose_picture);
        choosePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userImageSelector.showImageSourceDialog();
            }
        });

    }
    private void registerButtonClick() {
        Log.d(TAG, "Register button clicked");
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        String selectedValue = travelLikingSpinner.getSelectedItem().toString();
        int level = Integer.parseInt(selectedValue);

        RegistrationManager registrationManager = new RegistrationManager(RegistrationActivity.this);
        registrationManager.startRegistration(
                emailEditText.getText().toString(),
                passwordEditText.getText().toString(),
                et_nickname.getText().toString(),
                Integer.parseInt(et_age.getText().toString()),
                level,
                userImageSelector.createImageFile(),


                        new RegistrationManager.OnResultCallback(){
                    @Override
                    public void onResult(boolean success, String message) {
                        if (success) {
                            Toast.makeText(RegistrationActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else {
                            Toast.makeText(RegistrationActivity.this, "Registration failed: " + message, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

}