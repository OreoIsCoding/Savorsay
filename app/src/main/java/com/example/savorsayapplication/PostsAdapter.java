package com.example.savorsayapplication;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    private Context context;
    private List<Posts_model> postList;
    private OnItemClickListener listener;
    private String currentUserId;
    private PagerSnapHelper pagerSnapHelper;
    private DatabaseReference userLikesRef;

    public PostsAdapter(Context context, List<Posts_model> postList, String currentUserId, DatabaseReference userLikesRef) {
        this.context = context;
        this.postList = postList;
        this.currentUserId = currentUserId;
        this.userLikesRef = userLikesRef;
        pagerSnapHelper = new PagerSnapHelper();
    }

    public interface OnItemClickListener {
        void onDeleteClick(Posts_model post);

        void onLikeClick(Posts_model post);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_posts_design, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Posts_model post = postList.get(position);
        holder.textViewFullname.setText(post.getFullName());
        holder.textViewTimestamp.setText(formatTimestamp(post.getTimestamp()));
        holder.textViewContent.setText(post.getContent());
        holder.textViewLikeCount.setText(String.valueOf(post.getLikes()));

        // Fetch the restaurant name based on the restaurant ID associated with the post
        fetchRestaurantName(post.getRestaurantId(), holder);
        //Fetch the like count based on the post id
        fetchLikeCount(post.getPostId(), holder.textViewLikeCount);


        if (post.isBelongsToCurrentUser(currentUserId)) {
            holder.imageViewPostOptions.setVisibility(View.VISIBLE);
        } else {
            holder.imageViewPostOptions.setVisibility(View.GONE);
        }

        // Fetch user data based on userId and load profile image
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(post.getUserId());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    if (profileImageUrl != null) {
                        // Set profile image URL in the post object
                        post.setProfileImageUrl(profileImageUrl);

                        // Load and display profile image using Glide
                        Glide.with(context)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.profile_default)
                                .transform(new CircleCrop())
                                .into(holder.imageViewProfile);
                    } else {
                        // Load default profile image if profile image URL is null
                        Glide.with(context)
                                .load(R.drawable.profile_default)
                                .transform(new CircleCrop())
                                .into(holder.imageViewProfile);
                    }
                } else {
                    // Handle case where user data doesn't exist
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle errors
            }
        });

        List<String> imageUrls = post.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            holder.recyclerViewImages.setVisibility(View.VISIBLE);

            // Set layout manager and item decoration for RecyclerView
            LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            holder.recyclerViewImages.setLayoutManager(layoutManager);

            // Set up PagerSnapHelper for snapping behavior
            pagerSnapHelper.attachToRecyclerView(holder.recyclerViewImages);

            if (imageUrls.size() > 1) {
                String imageCounterText = String.format(Locale.getDefault(), "1/%d", imageUrls.size());
                holder.textViewImageCount.setText(imageCounterText);
                holder.textViewImageCount.setVisibility(View.VISIBLE);
            } else {
                holder.textViewImageCount.setVisibility(View.GONE);
            }

            // Calculate screen width
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int screenWidth = displayMetrics.widthPixels;

            // Calculate image width and height to fit screen
            int imageWidth = screenWidth;
            int imageHeight = ViewGroup.LayoutParams.WRAP_CONTENT;

            // Set layout params of RecyclerView to match_parent width and fixed height
            ViewGroup.LayoutParams layoutParams = holder.recyclerViewImages.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = imageHeight; // Set fixed height
            holder.recyclerViewImages.setLayoutParams(layoutParams);

            // Set layout params of each image item to center it horizontally
            RecyclerView.LayoutParams itemLayoutParams = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
            itemLayoutParams.width = imageWidth;
            holder.itemView.setLayoutParams(itemLayoutParams);

            ImageAdapter imagesAdapter = new ImageAdapter(context, imageUrls);
            holder.recyclerViewImages.setAdapter(imagesAdapter);

            holder.recyclerViewImages.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    int currentPosition = layoutManager.findFirstVisibleItemPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        // Update image count based on the current position
                        String imageCounterText = String.format(Locale.getDefault(), "%d/%d", currentPosition + 1, imageUrls.size());
                        holder.textViewImageCount.setText(imageCounterText);
                    }
                }
            });

        } else {
            holder.recyclerViewImages.setVisibility(View.GONE);
            holder.textViewImageCount.setVisibility(View.GONE);
        }

        // Check if the post is liked by the current user and update like button state
        checkUserLike(post.getPostId(), holder);

        // Set click listener for like button
        holder.likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                toggleLikeState(post, holder);
            }
        });
    }

    private void fetchLikeCount(String postId, TextView textViewLikeCount) {
        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("posts").child(postId).child("likes");
        postRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long likeCount = snapshot.getValue(Long.class);
                if (likeCount != null) {
                    textViewLikeCount.setText(String.valueOf(likeCount));
                    textViewLikeCount.setVisibility(likeCount > 0 ? View.VISIBLE : View.GONE);
                } else {
                    textViewLikeCount.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PostsAdapter", "Error fetching like count: " + error.getMessage());
            }
        });
    }


    private void checkUserLike(String postId, PostViewHolder holder) {
        // Check if the current user has liked the post
        DatabaseReference userPostLikeRef = userLikesRef.child(postId);
        userPostLikeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    holder.isLiked = true;
                    holder.likeButton.setImageResource(R.drawable.red_heart); // Change like button color to liked state(Red heart)
                } else {
                    holder.isLiked = false;
                    holder.likeButton.setImageResource(R.drawable.like_icon); // Change like button color to unliked state(default like)
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PostsAdapter", "Error checking user like: " + error.getMessage());
            }
        });
    }

    private void toggleLikeState(Posts_model post, PostViewHolder holder) {
        holder.isLiked = !holder.isLiked; // Toggle like state

        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("posts").child(post.getPostId());

        postRef.child("likes").runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Long currentLikes = currentData.getValue(Long.class);
                if (currentLikes == null) {
                    currentLikes = 0L;
                }

                if (holder.isLiked) {
                    currentLikes++;
                } else {
                    currentLikes--;
                    if (currentLikes < 0) {
                        currentLikes = 0L;
                    }
                }

                currentData.setValue(currentLikes);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Log.e("PostsAdapter", "Error updating like count: " + error.getMessage());
                    return;
                }

                if (committed && currentData != null) {
                    Long updatedLikes = currentData.getValue(Long.class);
                    if (updatedLikes != null) {
                        post.setLikes(updatedLikes.intValue()); // Update the local post model
                        holder.textViewLikeCount.setText(String.valueOf(updatedLikes));
                        holder.textViewLikeCount.setVisibility(updatedLikes > 0 ? View.VISIBLE : View.GONE);
                    }

                    // Update the like button image
                    if (holder.isLiked) {
                        holder.likeButton.setImageResource(R.drawable.red_heart); // Liked state
                        addPostToUserLikes(post.getPostId());
                    } else {
                        holder.likeButton.setImageResource(R.drawable.like_icon); // Unliked state
                        removePostFromUserLikes(post.getPostId());
                    }
                }
            }
        });
    }



    private void addPostToUserLikes(String postId) {
        // Add postId to the user's liked posts in the database
        DatabaseReference userPostLikeRef = userLikesRef.child(postId);
        userPostLikeRef.setValue(true);
    }

    private void removePostFromUserLikes(String postId) {
        // Remove postId from the user's liked posts in the database
        DatabaseReference userPostLikeRef = userLikesRef.child(postId);
        userPostLikeRef.removeValue();
    }

    private void fetchRestaurantName(String restaurantId, PostViewHolder holder) {
        if (restaurantId == null) {
            holder.textViewRestaurantName.setVisibility(View.GONE); // Hide the TextView
            return;
        }

        DatabaseReference restaurantRef = FirebaseDatabase.getInstance().getReference().child("Restaurants").child(restaurantId);
        restaurantRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String restaurantName = snapshot.child("name").getValue(String.class);
                    // Set the restaurant name in the TextView
                    holder.textViewRestaurantName.setText(restaurantName);
                    holder.textViewRestaurantName.setVisibility(View.VISIBLE); // restaurant name will be visible
                } else {
                    holder.textViewRestaurantName.setVisibility(View.GONE); // textview will be hidden if no restaurant associated with the post
                    Log.d("PostsAdapter", "Restaurant not found with ID: " + restaurantId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.textViewRestaurantName.setVisibility(View.GONE); // Hide the TextView
                Log.e("PostsAdapter", "Error fetching restaurant name: " + error.getMessage());
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewProfile;
        TextView textViewFullname;
        TextView textViewTimestamp;
        TextView textViewContent;
        TextView textViewLikeCount;
        RecyclerView recyclerViewImages;

        TextView textViewImageCount;
        TextView textViewRestaurantName;
        ImageView likeButton;
        ImageView imageViewPostOptions;
        boolean isLiked;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewProfile = itemView.findViewById(R.id.imageViewProfile);
            textViewFullname = itemView.findViewById(R.id.textViewFullname);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
            textViewLikeCount = itemView.findViewById(R.id.textViewLikeCount);
            textViewContent = itemView.findViewById(R.id.textViewContent);
            recyclerViewImages = itemView.findViewById(R.id.recyclerViewImages);
            imageViewPostOptions = itemView.findViewById(R.id.imageViewPostOptions);
            textViewImageCount = itemView.findViewById(R.id.textViewImageCount);
            textViewRestaurantName = itemView.findViewById(R.id.textViewRestaurantName);
            likeButton = itemView.findViewById(R.id.likeButton);

            imageViewPostOptions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        showPostOptionsMenu(v);
                    }
                }

                private void showPostOptionsMenu(View view) {
                    PopupMenu popupMenu = new PopupMenu(context, view);
                    popupMenu.inflate(R.menu.post_option); //

                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            int position = getAdapterPosition();
                            if (position != RecyclerView.NO_POSITION && listener != null) {
                                if (item.getItemId() == R.id.menu_delete) {
                                    listener.onDeleteClick(postList.get(position));
                                    return true;
                                }

                            }
                            return false;
                        }
                    });

                    popupMenu.show();
                }
            });
        }
    }
}