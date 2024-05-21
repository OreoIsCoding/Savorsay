package com.example.savorsayapplication;

import java.util.ArrayList;

public class Posts_model {
    private String postId;
    private String userId;
    private String restaurantId;
    private String content;
    private String reviewText;
    private long timestamp;
    private String fullName;
    private ArrayList<String> imageUrls;
    private String profileImageUrl;
    private boolean liked;
    private int likes;

    // Default constructor required for Firebase
    public Posts_model() {
    }

    // Constructor without images
    public Posts_model(String postId, String userId, String restaurantId, String content, long timestamp, String fullName) {
        this(postId, userId, restaurantId, content, timestamp, fullName, new ArrayList<>());
    }

    // Constructor with images
    public Posts_model(String postId, String userId, String restaurantId, String content, long timestamp, String fullName, ArrayList<String> imageUrls) {
        this.postId = postId;
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.content = content;
        this.timestamp = timestamp;
        this.fullName = fullName;
        this.imageUrls = (imageUrls != null) ? imageUrls : new ArrayList<>();
        this.profileImageUrl = null;
    }

    // Getters and setters...
    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public ArrayList<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(ArrayList<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    // Method to check if the post belongs to the current user
    public boolean isBelongsToCurrentUser(String currentUserId) {
        return this.userId.equals(currentUserId);
    }



    }


