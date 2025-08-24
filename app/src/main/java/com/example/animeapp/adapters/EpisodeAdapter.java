package com.example.animeapp.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.animeapp.R;
import com.example.animeapp.models.Episode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {
    private List<Episode> episodes;
    private OnEpisodeClickListener listener;

    public interface OnEpisodeClickListener {
        void onEpisodeClick(Episode episode);
    }

    public EpisodeAdapter(List<Episode> episodes, OnEpisodeClickListener listener) {
        this.episodes = episodes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_episode, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Episode episode = episodes.get(position);
        holder.title.setText(episode.getTitle());
        
        
        Glide.with(holder.itemView.getContext())
                .load(episode.getImageUrl())
                .into(holder.image);

        holder.itemView.setOnClickListener(v -> listener.onEpisodeClick(episode));
        
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
        return episodes != null ? episodes.size() : 0;
    }


     /*
    public void updateEpisodes(List<Episode> newEpisodes) {
        episodes = newEpisodes;
        notifyDataSetChanged();
    }
    */
    
    public void updateEpisodes(List<Episode> newEpisodes) {
    Set<String> seen = new HashSet<>();
    episodes.clear();

    for (Episode ep : newEpisodes) {
        String key = ep.getTitle() + "|" + ep.getInfoUrl();
        if (!seen.contains(key)) {
            seen.add(key);
            episodes.add(ep);
        }
    }

    notifyDataSetChanged();
}

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView image;
        public TextView title;
        

        public ViewHolder(View view) {
            super(view);
            image = view.findViewById(R.id.episode_image);
            title = view.findViewById(R.id.episode_title);
            
        }
    }
}