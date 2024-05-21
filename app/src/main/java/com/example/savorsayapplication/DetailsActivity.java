package com.example.savorsayapplication;

import static com.google.firebase.appcheck.internal.util.Logger.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DetailsActivity extends AppCompatActivity {


    private ImageView imageViewRestaurant;
    private TextView textViewRestaurantName;
    private TextView restaurantDescription;
    private FloatingActionButton fabPostReview;
    private RecyclerView reviewPostsView;

    private List<Posts_model> postList;
    private PostsAdapter postsAdapter;
    private DatabaseReference postsRef;
    private String restaurantId;
    private String currentUserId;
    private DatabaseReference userLikesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        imageViewRestaurant = findViewById(R.id.imageViewRestaurant);
        textViewRestaurantName = findViewById(R.id.textViewRestaurantName);
        restaurantDescription = findViewById(R.id.restaurantDescription);
        fabPostReview = findViewById(R.id.fabPostReview);
        reviewPostsView = findViewById(R.id.restaurantPostsView);


        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userLikesRef = FirebaseDatabase.getInstance().getReference().child("userLikes").child(currentUserId);


        Intent intent = getIntent();
        String restaurantDetails = intent.getStringExtra("restaurant_details");
        String restaurantName = intent.getStringExtra("restaurant_name");
        String imageUrl = intent.getStringExtra("restaurant_image_url");
        restaurantId = intent.getStringExtra("restaurant_id");


        textViewRestaurantName.setText(restaurantName);
        restaurantDescription.setText(restaurantDetails);


        Glide.with(this)
                .load(imageUrl)
                .into(imageViewRestaurant);


        postList = new ArrayList<>();
        postsAdapter = new PostsAdapter(this, postList, currentUserId, userLikesRef);

        // Set the LayoutManager before setting the adapter
        reviewPostsView.setLayoutManager(new LinearLayoutManager(this));
        reviewPostsView.setAdapter(postsAdapter);

        // Fetch and display posts for the specific restaurant
        fetchRestaurantPosts();


        fabPostReview.setOnClickListener(view -> showReviewInputActivity());
    }

    private void showReviewInputActivity() {
        // Create an intent to start the ReviewInputActivity
        Intent intent = new Intent(this, ReviewInputActivity.class);
        intent.putExtra("restaurant_id", restaurantId);
        startActivity(intent);
    }

    private void fetchRestaurantPosts() {
        postsRef = FirebaseDatabase.getInstance().getReference().child("posts");
        postsRef.orderByChild("restaurantId").equalTo(restaurantId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Posts_model post = snapshot.getValue(Posts_model.class);
                    if (post != null) {
                        fetchUserFullName(post);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle possible errors.
                Log.e(TAG, "Error fetching posts: ", databaseError.toException());
            }
        });
    }

    private void fetchUserFullName(Posts_model post) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(post.getUserId());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String fullName = dataSnapshot.child("fullName").getValue(String.class);
                    if (fullName != null) {
                        post.setFullName(fullName);
                        postList.add(post);
                        postsAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(DetailsActivity.this, "Failed to fetch user data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }
}
