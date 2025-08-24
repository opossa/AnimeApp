package com.example.animeapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.animeapp.R;
import com.example.animeapp.models.AnimeItem;
import java.util.List;

public class AnimeGridAdapter extends RecyclerView.Adapter<AnimeGridAdapter.ViewHolder> {
    private List<AnimeItem> animeItems;
    private OnAnimeClickListener listener;

    public interface OnAnimeClickListener {
        void onAnimeClick(AnimeItem animeItem);
    }

    public AnimeGridAdapter(List<AnimeItem> animeItems, OnAnimeClickListener listener) {
        this.animeItems = animeItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_anime_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnimeItem item = animeItems.get(position);
        holder.title.setText(item.getTitle());

        Glide.with(holder.itemView.getContext())
                .load(item.getImageUrl())
                .placeholder(R.drawable.warning)
                .into(holder.image);
        
        if (item.getDubText() != null && !item.getDubText().isEmpty()) {
            holder.dubTextView.setVisibility(View.VISIBLE);
            holder.dubTextView.setText(item.getDubText());
        } else {
            holder.dubTextView.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onAnimeClick(item));

        holder.itemView.setOnFocusChangeListener(
                (v, hasFocus) -> {
                    if (hasFocus) {
                        v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start();
                        v.setBackgroundResource(
                                R.drawable.focus_background);
                    } else {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                        v.setBackgroundResource(
                                R.drawable.focus_background);
                    }
                });
    }

    @Override
    public int getItemCount() {
        return animeItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView image;
        public TextView title, dubTextView;

        public ViewHolder(View view) {
            super(view);
            image = view.findViewById(R.id.anime_image);
            title = view.findViewById(R.id.anime_title);
            dubTextView = view.findViewById(R.id.dub_text);
            
        }
    }
}