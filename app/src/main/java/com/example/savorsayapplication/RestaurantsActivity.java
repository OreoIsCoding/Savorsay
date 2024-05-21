package com.example.savorsayapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RestaurantsActivity extends AppCompatActivity implements RestaurantAdapter.OnItemClickListener {

    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private RestaurantAdapter adapter;

    private static final int MENU_PROFILE = R.id.menu_profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurants);

        mAuth = FirebaseAuth.getInstance();
        recyclerView = findViewById(R.id.recyclerViewRestaurants);
        List<Restaurant> restaurantList = new ArrayList<>();
        adapter = new RestaurantAdapter(this, restaurantList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchRestaurantData();

        //Bottom navigation item click listener
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_restaurants);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                if (item.getItemId() == R.id.nav_home) {
                    Intent homeIntent = new Intent(RestaurantsActivity.this, HomeActivity.class);
                    startActivity(homeIntent);
                    finish();
                    return true;

                } else if (item.getItemId()==MENU_PROFILE){
                    Intent profileIntent = new Intent(RestaurantsActivity.this, ProfileActivity.class);
                    startActivity(profileIntent);
                    finish();
                    return true;

                }
                return false;

            }
        });

        adapter.setOnItemClickListener(this);

    }

    @Override
    public void onItemClick(Restaurant restaurant) {
        // Handle the item click here
        // For example, navigate to restaurant details activity
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra("restaurant_name", restaurant.getName());
        intent.putExtra("restaurant_image_url", restaurant.getImageUrl());
        intent.putExtra("restaurant_details", restaurant.getDetails());
        intent.putExtra("restaurant_id", restaurant.getRestaurantId());
        startActivity(intent);
    }

    private void fetchRestaurantData() {
        DatabaseReference restaurantsRef = FirebaseDatabase.getInstance().getReference().child("Restaurants");
        restaurantsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Restaurant> restaurants = new ArrayList<>();
                for (DataSnapshot restaurantSnapshot : snapshot.getChildren()) {
                    String name = restaurantSnapshot.child("name").getValue(String.class);
                    String imageUrl = restaurantSnapshot.child("imageUrl").getValue(String.class);
                    String details = restaurantSnapshot.child("details").getValue(String.class);
                    String restaurantId = restaurantSnapshot.getKey();
                    Restaurant restaurant = new Restaurant(name, imageUrl, details, restaurantId);
                    restaurants.add(restaurant);
                }
                updateRecyclerView(restaurants);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RestaurantsActivity", "Error getting restaurant data: " + error.getMessage());
                // Handle error here
                Toast.makeText(RestaurantsActivity.this, "Failed to fetch restaurant data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRecyclerView(List<Restaurant> restaurants) {
        adapter.setRestaurantList(restaurants);
        adapter.notifyDataSetChanged();
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
