package com.example.savorsayapplication;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class SelectedImagesAdapter extends RecyclerView.Adapter<SelectedImagesAdapter.ImageViewHolder> {

    private ArrayList<Uri> selectedImageUris;
    private Context context; // Add a Context variable

    public SelectedImagesAdapter(Context context, ArrayList<Uri> selectedImageUris) {
        this.context = context;
        this.selectedImageUris = selectedImageUris;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_images, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri imageUri = selectedImageUris.get(position);
        // Set the image to ImageView using Glide or any other image loading library
        Glide.with(context)
                .load(imageUri)
                .into(holder.imageView);

        // Set OnClickListener to delete the image when clicked
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeImage(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return selectedImageUris.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }

    // Method to remove an image at a specific position
    public void removeImage(int position) {
        if (position >= 0 && position < selectedImageUris.size()) {
            selectedImageUris.remove(position);
            notifyItemRemoved(position);
        } else {
            Toast.makeText(context, "Invalid position", Toast.LENGTH_SHORT).show();
        }
    }
}
