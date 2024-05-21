package com.example.savorsayapplication;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import android.widget.ImageView;

public class ImageViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        // Get the image URL passed from the HomeActivity
        String imageUrl = getIntent().getStringExtra("image_url");

        // Find the ImageView in the layout
        ImageView imageView = findViewById(R.id.imageView);

        // Load the image into the ImageView using Glide
        Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.color.darker_gray)
                .into(imageView);
    }
}
