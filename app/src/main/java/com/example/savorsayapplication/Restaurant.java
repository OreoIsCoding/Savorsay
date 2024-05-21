package com.example.savorsayapplication;

public class Restaurant {
    private String details;
    private String name;
    private String imageUrl;
    private String restaurantId;
    private float rating;
    private int numRatings;

    // Constructors, getters, and setters
    public Restaurant() {
        // Default constructor required for Firebase
    }

    public Restaurant(String name, String imageUrl, String details, String restaurantId) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.details = details;
        this.restaurantId=restaurantId;
    }

    // Getters and setters for each field

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDetails() {
        return details;
    }

    public String getRestaurantId(){return restaurantId;}

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getNumRatings() {
        return numRatings;
    }

    public void setNumRatings(int numRatings) {
        this.numRatings = numRatings;
    }
}
