package com.example.savorsayapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import de.hdodenhof.circleimageview.CircleImageView;



public class ReviewInputActivity extends Activity {
    // Define variables
    private String selectedRestaurantId;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_GALLERY = 2;
    private ArrayList<Uri> selectedImageUris = new ArrayList<>();

    private RecyclerView recyclerView;
    private SelectedImagesAdapter adapter;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.review_input);

        // Retrieve the selected restaurant ID
        selectedRestaurantId = getIntent().getStringExtra("restaurant_id");

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.SelectedImages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new SelectedImagesAdapter(this, selectedImageUris);
        recyclerView.setAdapter(adapter);


        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Posting...");
        progressDialog.setCancelable(false);


        // Set OnClickListener for the "Post" button
        Button buttonPost = findViewById(R.id.buttonPost);
        buttonPost.setOnClickListener(this::onPostButtonClick);

        // Set OnClickListener for the "Capture Image" button
        CardView buttonCaptureImage = findViewById(R.id.openCamera);
        buttonCaptureImage.setOnClickListener(v -> captureImage());

        // Set OnClickListener for the "Select Image" button
        CardView buttonSelectImage = findViewById(R.id.openGallery);
        buttonSelectImage.setOnClickListener(v -> selectImage());
        
        displayProfilePicture();
    }

    private void displayProfilePicture() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String profileImageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            // Load profile picture into CircleImageView
                            CircleImageView profileImageView = findViewById(R.id.imageViewProfileUser);
                            Glide.with(ReviewInputActivity.this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.profile_default) // Placeholder image while loading
                                    .into(profileImageView);
                        } else {
                            // Profile picture URL not found in database
                            Toast.makeText(ReviewInputActivity.this, "Profile picture URL not found in database", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // User data not found in database
                        Toast.makeText(ReviewInputActivity.this, "User data not found in database", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Error handling
                    Toast.makeText(ReviewInputActivity.this, "Failed to fetch user data from database: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Current user not found
            Toast.makeText(ReviewInputActivity.this, "Current user not found", Toast.LENGTH_SHORT).show();
        }
    }



    private void captureImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "Camera app not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectImage() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickPhotoIntent.setType("image/*");
        startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_GALLERY);
    }

    // Fetch the full name of the current user
    private void fetchFullName() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId);
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String fullName = dataSnapshot.child("fullName").getValue(String.class);
                        // Use the full name as needed
                        Toast.makeText(ReviewInputActivity.this, "Welcome, " + fullName, Toast.LENGTH_SHORT).show();
                    } else {
                        // User data not found
                        Toast.makeText(ReviewInputActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Error handling
                    Toast.makeText(ReviewInputActivity.this, "Failed to fetch user data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void onPostButtonClick(View view) {
        // Get review text from EditText
        EditText editTextReview = findViewById(R.id.editPost);
        String content = editTextReview.getText().toString();


        if (!content.isEmpty()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                StorageReference storageReference = FirebaseStorage.getInstance().getReference();
                String fullName = currentUser.getDisplayName();
                String postId = databaseReference.child("posts").push().getKey();
                long timestamp = System.currentTimeMillis();
                Posts_model post;

                if (!selectedImageUris.isEmpty()) {
                    uploadImagesToStorage(selectedImageUris, postId, databaseReference, storageReference, new ImageUploadCallback() {
                        @Override
                        public void onUploadComplete(ArrayList<String> imageUrls) {
                           final Posts_model post = new Posts_model(postId, userId, selectedRestaurantId, content, timestamp, fullName, imageUrls);
                            databaseReference.child("posts").child(postId).setValue(post);
                            Toast.makeText(ReviewInputActivity.this, "Post successful", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onUploadFailure(String errorMessage) {
                            Toast.makeText(ReviewInputActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    post = new Posts_model(postId, userId, selectedRestaurantId, content, timestamp, fullName, null);
                    databaseReference.child("posts").child(postId).setValue(post);
                    Toast.makeText(ReviewInputActivity.this, "Post successful", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(ReviewInputActivity.this, "Current user not found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No content", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImagesToStorage(ArrayList<Uri> imageUris, String postId, DatabaseReference databaseReference, StorageReference storageReference, ImageUploadCallback callback) {
        ArrayList<String> imageUrls = new ArrayList<>();
        AtomicInteger uploadedCount = new AtomicInteger(0);
        int totalImages = imageUris.size();

        for (Uri imageUri : imageUris) {
            StorageReference imageRef = storageReference.child("images/" + postId + "/" + UUID.randomUUID().toString());
            UploadTask uploadTask = imageRef.putFile(imageUri);

            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return imageRef.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    if (downloadUri != null) {
                        imageUrls.add(downloadUri.toString());
                    }
                }

                int count = uploadedCount.incrementAndGet();
                if (count == totalImages) {
                    callback.onUploadComplete(imageUrls);
                }
            }).addOnFailureListener(e -> {
                callback.onUploadFailure("Failed to upload image: " + e.getMessage());
            });
        }
    }

    interface ImageUploadCallback {
        void onUploadComplete(ArrayList<String> imageUrls);
        void onUploadFailure(String errorMessage);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (data != null && data.getExtras() != null) {
                    Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                    if (imageBitmap != null) {
                        Uri imageUri = getImageUri(getApplicationContext(), imageBitmap);
                        selectedImageUris.add(imageUri);
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (requestCode == REQUEST_IMAGE_GALLERY && data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    selectedImageUris.add(selectedImageUri);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this, "Failed to select image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }


    //When Content and editText is not empty, prompt AlertDialog if you want to discard the post or not
    @Override
    public void onBackPressed() {

        EditText editTextReview = findViewById(R.id.editPost);
        String content = editTextReview.getText().toString();

        if (!content.isEmpty() || !selectedImageUris.isEmpty()) {

            new AlertDialog.Builder(this)
                    .setTitle("Discard Post")
                    .setMessage("Are you sure you want to discard this post?")
                    .setPositiveButton("Discard", (dialog, which) -> {
                        // Finish activity
                        super.onBackPressed();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        // Dismiss dialog
                        dialog.dismiss();
                    })
                    .show();
        } else {
            // If content is empty, proceed with back press
            super.onBackPressed();
        }
    }
}
