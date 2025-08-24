package com.example.animeapp.adapters;

import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import com.example.animeapp.models.HistoryItem;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.View;
import android.view.LayoutInflater;
import com.example.animeapp.R;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List<HistoryItem> historyList;
    private OnHistoryItemClickListener listener;

    public interface OnHistoryItemClickListener {
        void onHistoryItemClick(HistoryItem item);
    }

    public HistoryAdapter(List<HistoryItem> historyList, OnHistoryItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = historyList.get(position);
        holder.titleText.setText(item.getTitle());
        holder.episodeText.setText("ตอนที่ " + item.getEpisode());
        holder.timeText.setText(item.getFormattedTimestamp());
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(item.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.warning)
                .into(holder.historyImage);
        } else {
            holder.historyImage.setImageResource(R.drawable.placeholder_image);
        }
        
        holder.itemView.setOnClickListener(v -> listener.onHistoryItemClick(item));
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, episodeText, timeText;
        ImageView historyImage;

        public ViewHolder(View itemView) {
            super(itemView);
            historyImage = itemView.findViewById(R.id.history_image);
            titleText = itemView.findViewById(R.id.title_text);
            episodeText = itemView.findViewById(R.id.episode_text);
            timeText = itemView.findViewById(R.id.time_text);
        }
    }
}