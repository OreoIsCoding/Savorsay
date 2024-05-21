package com.example.savorsayapplication;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 2;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;

    private CircleImageView profileImageView;
    private TextView fullNameTextView;
    private TextView emailTextView;
    private TextView settings, about_us;
    private String currentProfileImageUrl;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        // Initialize views
        profileImageView = findViewById(R.id.profile_image);
        fullNameTextView = findViewById(R.id.fullName);
        emailTextView = findViewById(R.id.email);
        settings = findViewById(R.id.buttonSettings);

        settings.setOnClickListener(v -> openSettingsPrivacyActivity());


        // Set up logout button click listener
        Button logoutButton = findViewById(R.id.buttonLogout);
        logoutButton.setOnClickListener(v -> logoutUser());

        // Set up profile image click listener
        CircleImageView addPhoto = findViewById(R.id.addPhoto);
        addPhoto.setOnClickListener(this::onAddPhotoClicked);

        // Bottom navigation item click listener
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.menu_profile);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                Intent homeIntent = new Intent(ProfileActivity.this, HomeActivity.class);
                startActivity(homeIntent);
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_restaurants) {
                Intent profileIntent = new Intent(ProfileActivity.this, RestaurantsActivity.class);
                startActivity(profileIntent);
                finish();
                return true;
            }
            return false;
        });

        // Display user's full name, email, and profile picture
        displayUserInfo();
    }

    private void onAddPhotoClicked(View view) {
        // Check if there is a current profile picture
        if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
            // Show options to change or delete the profile picture
            CharSequence[] options = {"Change Profile Picture", "Delete Profile Picture"};
            new AlertDialog.Builder(this)
                    .setTitle("Profile Picture")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            // Change profile picture
                            Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(pickIntent, REQUEST_IMAGE_PICK);
                        } else if (which == 1) {
                            // Delete profile picture
                            deleteProfileImage();
                        }
                    })
                    .show();
        } else {
            // No profile picture, only option to upload
            Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickIntent, REQUEST_IMAGE_PICK);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                uploadProfileImage(selectedImageUri);
            }
        }
    }

    private void uploadProfileImage(Uri imageUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            StorageReference profileImageRef = mStorageRef.child("profile_images").child(userId + ".jpg");

            // Show progress dialog
            showProgressDialog("Uploading image...");

            profileImageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                try {
                                    String imageUrl = uri.toString();
                                    mDatabase.child("users").child(userId).child("profileImageUrl").setValue(imageUrl)
                                            .addOnCompleteListener(task -> {
                                                // Dismiss progress dialog
                                                dismissProgressDialog();

                                                if (task.isSuccessful()) {
                                                    Toast.makeText(ProfileActivity.this, "Profile image updated", Toast.LENGTH_SHORT).show();
                                                    Glide.with(ProfileActivity.this).load(imageUrl).into(profileImageView);
                                                    currentProfileImageUrl = imageUrl;
                                                } else {
                                                    Toast.makeText(ProfileActivity.this, "Failed to update profile image", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                } catch (Exception e) {
                                    // Dismiss progress dialog
                                    dismissProgressDialog();
                                    Toast.makeText(ProfileActivity.this, "Error uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }))
                    .addOnFailureListener(e -> {
                        // Dismiss progress dialog
                    });
        }
    }


    private void deleteProfileImage() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            StorageReference profileImageRef = mStorageRef.child("profile_images").child(userId + ".jpg");

            // Show progress dialog
            showProgressDialog("Deleting image...");

            profileImageRef.delete().addOnSuccessListener(aVoid -> {
                mDatabase.child("users").child(userId).child("profileImageUrl").removeValue()
                        .addOnCompleteListener(task -> {
                            // Dismiss progress dialog
                            dismissProgressDialog();

                            if (task.isSuccessful()) {
                                Toast.makeText(ProfileActivity.this, "Profile image deleted", Toast.LENGTH_SHORT).show();
                                profileImageView.setImageResource(R.drawable.profile_default);
                                currentProfileImageUrl = null;
                            } else {
                                Toast.makeText(ProfileActivity.this, "Failed to delete profile image from database", Toast.LENGTH_SHORT).show();
                            }
                        });
            }).addOnFailureListener(e -> {
                // Dismiss progress dialog
                dismissProgressDialog();
                Toast.makeText(ProfileActivity.this, "Failed to delete profile image from storage: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void logoutUser() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            markSessionInactive(user.getUid()); // Mark user's session as inactive in all devices
        }

        // Initialize SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        // Edit SharedPreferences to set isLoggedIn to false
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.apply();

        mAuth.signOut(); // Sign out the current user

        // Navigate to the login screen
        navigateToLogin();
    }

    private void markSessionInactive(String userId) {
        DatabaseReference sessionsRef = FirebaseDatabase.getInstance().getReference("active_sessions");
        String deviceId = getUniqueDeviceId(); // Implement a method to get a unique identifier for the current device
        sessionsRef.child(userId).child(deviceId).removeValue(); // Remove user's session for the current device
    }

    @SuppressLint("HardwareIds")
    private String getUniqueDeviceId() {
        // Use Android's Secure.ANDROID_ID as a unique device identifier
        // This method requires the READ_PHONE_STATE permission
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Finish the current activity to prevent the user from returning to it after logout
    }

    private void displayUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = mDatabase.child("users").child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    try {
                        String fullName = dataSnapshot.child("fullName").getValue(String.class);
                        String email = dataSnapshot.child("email").getValue(String.class);
                        String profileImageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);

                        if (fullName != null && !fullName.isEmpty()) {
                            fullNameTextView.setText(fullName);
                        }

                        if (email != null && !email.isEmpty()) {
                            emailTextView.setText(email);
                        }

                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(ProfileActivity.this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.profile_default)
                                    .into(profileImageView);
                            currentProfileImageUrl = profileImageUrl;
                        } else {
                            profileImageView.setImageResource(R.drawable.profile_default);
                        }
                    } catch (Exception e) {
                        // Handle error
                        Toast.makeText(ProfileActivity.this, "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle error
                    Toast.makeText(ProfileActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void openSettingsPrivacyActivity() {
        Intent intent = new Intent(this, SettingsPrivacyActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Finish the current activity to prevent it from being added to the stack again
    }
}
