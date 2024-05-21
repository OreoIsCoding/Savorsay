package com.example.savorsayapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class HomeActivity extends AppCompatActivity implements PostsAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private PostsAdapter adapter;
    private List<Posts_model> postList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String currentUserId;
    private DatabaseReference userLikesRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        recyclerView = findViewById(R.id.recyclerViewNewsfeed);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        postList = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Fetch user ID first
        fetchCurrentUserId();


        // Set up bottom navigation view
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_home) {
                    // Refresh the newsfeed
                    refreshData();
                    return true;
                } else if (item.getItemId() == R.id.nav_restaurants) {
                    startActivity(new Intent(HomeActivity.this, RestaurantsActivity.class));
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.menu_profile) {
                    startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                    finish();
                    return true;
                }
                return false;
            }
        });

        // Set the refresh listener
        setRefreshListener();
    }

    private void fetchCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            userLikesRef = FirebaseDatabase.getInstance().getReference().child("userLikes").child(currentUserId);
            adapter = new PostsAdapter(this, postList, currentUserId, userLikesRef);
            adapter.setOnItemClickListener(this);
            recyclerView.setAdapter(adapter); // Adapter initialized here
            fetchNewsfeedData();
            Log.d("fetchCurrentUserId", "Current user ID: " + currentUserId);
            // Fetch newsfeed data after obtaining user ID
        } else {
            Log.e("fetchCurrentUserId", "Current user is null");
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

    private void setRefreshListener() {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
            }
        });
    }

    private void refreshData() {

        postList.clear();

        fetchNewsfeedData();

        swipeRefreshLayout.setRefreshing(false); // Set refreshing state to false

        // Check if the post list is empty
        if (postList.isEmpty()) {
            // Show a message indicating no data
            Toast.makeText(this, "No new posts available", Toast.LENGTH_SHORT).show();
        }
    }


    private void fetchNewsfeedData() {
        if (!Network.isNetworkConnected(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference().child("posts");

        postsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Posts_model> allPosts = new ArrayList<>();
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Posts_model post = postSnapshot.getValue(Posts_model.class);
                    if (post != null) {
                        allPosts.add(post);
                    }
                }

                // Sort all posts based on timestamps
                Collections.sort(allPosts, new Comparator<Posts_model>() {
                    @Override
                    public int compare(Posts_model post1, Posts_model post2) {
                        return Long.compare(post2.getTimestamp(), post1.getTimestamp());
                    }
                });

                // Add sorted posts to the list
                postList.addAll(allPosts);

                // Fetch users' full names
                fetchUsersFullNames(allPosts);

                // Notify adapter of data change
                adapter.notifyDataSetChanged();

                // Scroll to the top of the list
                recyclerView.scrollToPosition(0);

                // Set refreshing state to false
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(HomeActivity.this, "Failed to retrieve posts: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Define ChildEventListener methods here
        postsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                // Handle new posts if needed
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                // Handle post changes if needed
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // Handle post removal if needed
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                // Handle post movement if needed
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(HomeActivity.this, "Failed to retrieve posts: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUsersFullNames(List<Posts_model> posts) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("users");

        for (Posts_model post : posts) {
            usersRef.child(post.getUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Retrieve user's full name
                        String fullName = dataSnapshot.child("fullName").getValue(String.class);
                        if (fullName != null) {
                            post.setFullName(fullName);
                            // Notify adapter of data change after updating full name
                            adapter.notifyDataSetChanged();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle error
                    Toast.makeText(HomeActivity.this, "Failed to fetch user data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onDeleteClick(Posts_model post) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you sure you want to delete this post?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Perform deletion
                deletePost(post);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Cancel deletion
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onLikeClick(Posts_model post) {
        // Update the like status of the post
        post.setLiked(!post.isLiked());

        // Update the like count in the database
        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("posts").child(post.getPostId());
        postRef.child("likes").setValue(post.getLikes());

        // Notify the adapter of the data change
        adapter.notifyDataSetChanged();
    }

    private void deletePost(Posts_model post) {
        // Check if the post belongs to the current user
        if (post.getUserId().equals(currentUserId)) {
            // Get the post ID
            String postId = post.getPostId();

            // Access the posts node in the Firebase database
            DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference().child("posts");

            // Query to find the post with the matching post ID
            Query query = postsRef.orderByChild("postId").equalTo(postId);

            // ValueEventListener to listen for the result of the query
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Check if the query returned any results
                    if (dataSnapshot.exists()) {
                        // Delete the post directly by accessing the reference of the first result
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            snapshot.getRef().removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                // Post deleted successfully
                                                Toast.makeText(HomeActivity.this, "Post deleted successfully", Toast.LENGTH_SHORT).show();

                                                // Remove the deleted post from the postList
                                                postList.remove(post);

                                                // Notify the adapter of the data change
                                                adapter.notifyDataSetChanged();

                                                // Delete images from Firebase Storage
                                                List<String> imageUrls = post.getImageUrls();
                                                if (imageUrls != null && !imageUrls.isEmpty()) {
                                                    for (String imageUrl : imageUrls) {
                                                        // Get a reference to the image file
                                                        StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);

                                                        // Delete the image file
                                                        imageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                // Image deleted successfully
                                                                Log.d("DeleteImage", "Image deleted successfully");
                                                            }
                                                        }).addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                // Failed to delete image
                                                                Log.e("DeleteImage", "Failed to delete image: " + e.getMessage());
                                                            }
                                                        });
                                                    }
                                                }

                                                // Delete likes associated with the post
                                                DatabaseReference userLikesRef = FirebaseDatabase.getInstance().getReference().child("userLikes");
                                                userLikesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                                            String userId = userSnapshot.getKey();
                                                            boolean likeDeleted = false; // Track if any like is deleted
                                                            for (DataSnapshot postSnapshot : userSnapshot.getChildren()) {
                                                                String likedPostId = postSnapshot.getKey();
                                                                if (postId.equals(likedPostId)) {
                                                                    Log.d("DeleteLike", "Found like by user: " + userId + " for post: " + postId);
                                                                    postSnapshot.getRef().removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if (task.isSuccessful()) {
                                                                                Log.d("DeleteLike", "Like deleted successfully for post: " + postId + " by user: " + userId);
                                                                            } else {
                                                                                Log.e("DeleteLike", "Failed to delete like: " + task.getException().getMessage());
                                                                            }
                                                                        }
                                                                    });
                                                                    likeDeleted = true;
                                                                }
                                                            }
                                                            // Check if userSnapshot is empty after deletions
                                                            if (likeDeleted) {
                                                                userSnapshot.getRef().addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot userSnapshotAfterDeletion) {
                                                                        if (!userSnapshotAfterDeletion.hasChildren()) {
                                                                            userSnapshotAfterDeletion.getRef().removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    if (task.isSuccessful()) {
                                                                                        Log.d("DeleteUserLikes", "User likes node deleted for user: " + userId);
                                                                                    } else {
                                                                                        Log.e("DeleteUserLikes", "Failed to delete user likes node: " + task.getException().getMessage());
                                                                                    }
                                                                                }
                                                                            });
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                                                        Log.e("DeleteUserLikes", "Failed to check user likes node: " + databaseError.getMessage());
                                                                    }
                                                                });
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                                        Log.e("DeleteLike", "Failed to delete likes: " + databaseError.getMessage());
                                                    }
                                                });

                                            } else {
                                                // Error occurred while deleting the post
                                                Toast.makeText(HomeActivity.this, "Failed to delete post: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    } else {
                        // No post found with the given post ID
                        Toast.makeText(HomeActivity.this, "Post not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Error occurred while querying the database
                    Toast.makeText(HomeActivity.this, "Failed to delete post: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Post does not belong to the current user
            Toast.makeText(HomeActivity.this, "You can only delete your own posts", Toast.LENGTH_SHORT).show();
        }
    }
}
