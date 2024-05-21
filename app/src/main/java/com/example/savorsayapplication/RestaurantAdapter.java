package com.example.savorsayapplication;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.savorsayapplication.R;

import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.ViewHolder> {

    private List<Restaurant> restaurantList;
    private Context context;
    private OnItemClickListener listener;

    public RestaurantAdapter(Context context, List<Restaurant> restaurantList) {
        this.context = context;
        this.restaurantList = restaurantList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setRestaurantList(List<Restaurant> restaurants) {
        this.restaurantList = restaurants;
        notifyDataSetChanged(); // Notify adapter that data has changed
    }

    public interface OnItemClickListener {
        void onItemClick(Restaurant restaurant);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Restaurant restaurant = restaurantList.get(position);
        holder.bind(restaurant);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(restaurant);
            }
        });
    }

    @Override
    public int getItemCount() {
        return restaurantList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewRestaurantName;
        private ImageView imageViewRestaurant;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewRestaurantName = itemView.findViewById(R.id.textViewRestaurantName);
            imageViewRestaurant = itemView.findViewById(R.id.imageViewRestaurant);
        }

        public void bind(Restaurant restaurant) {
            textViewRestaurantName.setText(restaurant.getName());
            Glide.with(itemView.getContext())
                    .load(restaurant.getImageUrl())
                    .apply(RequestOptions.centerCropTransform())
                    .into(imageViewRestaurant);
        }
    }
}
