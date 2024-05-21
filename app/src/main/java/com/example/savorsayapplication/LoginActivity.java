package com.example.savorsayapplication;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import java.util.UUID;
import android.provider.Settings;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText;
    private FirebaseAuth mAuth;
    private DatabaseReference activeSessionsRef;
    private SharedPreferences sharedPreferences;
    private ProgressDialog progressDialog;
    private ImageButton togglePasswordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        activeSessionsRef = FirebaseDatabase.getInstance().getReference("active_sessions");


        usernameEditText = findViewById(R.id.textUsername);
        passwordEditText = findViewById(R.id.textPassword);
        togglePasswordButton = findViewById(R.id.toggle_password);


        Button loginButton = findViewById(R.id.buttonLogin);
        loginButton.setOnClickListener(v -> loginUser());

        Button signUpButton = findViewById(R.id.buttonSignup);
        signUpButton.setOnClickListener(view -> navigateToSignUp());

        TextView forgotPasswordTextView = findViewById(R.id.forgotPassword);
        forgotPasswordTextView.setOnClickListener(view -> navigateToForgotPassword());


        togglePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (passwordEditText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                    passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    togglePasswordButton.setImageResource(R.drawable.password_toggle); // change the icon to 'visible'
                } else {
                    passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    togglePasswordButton.setImageResource(R.drawable.password_toggle); // change the icon back to 'hidden'
                }
                // Move the cursor to the end of the text
                passwordEditText.setSelection(passwordEditText.getText().length());
            }
        });

        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Logging in...");

        if (isLoggedIn()) {

            progressDialog.show();

            handleExistingSession();

        }


    }

    private boolean isLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    private void handleExistingSession() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference userSessionsRef = activeSessionsRef.child(userId);

            userSessionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    boolean existingSessionFound = false;
                    String currentDeviceId = getUniqueDeviceId();

                    for (DataSnapshot sessionSnapshot : dataSnapshot.getChildren()) {
                        String deviceId = sessionSnapshot.child("device_id").getValue(String.class);
                        if (deviceId != null && deviceId.equals(currentDeviceId)) {
                            // Existing session found on current device
                            existingSessionFound = true;
                            break;
                        }
                    }

                    if (existingSessionFound) {
                        // No need to prevent login, proceed with HomeActivity
                        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    } else {
                        // Existing session found on another device, show session expired dialog
                        new AlertDialog.Builder(LoginActivity.this)
                                .setTitle("Session Expired")
                                .setMessage("Your session is expired because you are logged in from another device.")
                                .setPositiveButton("OK", (dialog, which) -> {
                                    // Sign out the user
                                    mAuth.signOut();
                                    // Proceed to login
                                    loginUser();
                                })
                                .setCancelable(false)
                                .show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle error
                }
            });
        }
    }

    private void loginUser() {
        String email = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog.show();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
               progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            String sessionId = UUID.randomUUID().toString();
                            DatabaseReference userSessionsRef = activeSessionsRef.child(userId).child(sessionId);
                            String deviceId = getUniqueDeviceId(); // Get unique device ID
                            userSessionsRef.child("device_id").setValue(deviceId);
                            long currentTimeMillis = System.currentTimeMillis();
                            userSessionsRef.child("login_timestamp").setValue(currentTimeMillis);

                            checkAndDeleteExistingSessions(userId, deviceId);
                            // Redirect to HomeActivity
                            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                            navigateToHome();
                        }
                    } else {
                        // Handle login failure
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkAndDeleteExistingSessions(String userId, String currentDeviceId) {
        DatabaseReference userSessionsRef = activeSessionsRef.child(userId);
        userSessionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot sessionSnapshot : dataSnapshot.getChildren()) {
                    String deviceId = sessionSnapshot.child("device_id").getValue(String.class);
                    if (!TextUtils.isEmpty(deviceId) && !deviceId.equals(currentDeviceId)) {
                        // Session exists on another device, delete it
                        sessionSnapshot.getRef().removeValue();
                        // Optionally, you can notify the user about the expired session on the other device
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });
    }


    @SuppressLint("HardwareIds")
    private String getUniqueDeviceId() {

        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private void navigateToHome() {
        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
        finish();
    }

    private void navigateToSignUp() {
        Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
        startActivity(intent);
    }

    private void navigateToForgotPassword() {
        Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
        startActivity(intent);
    }
}
