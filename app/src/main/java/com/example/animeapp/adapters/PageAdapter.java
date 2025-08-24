package com.example.animeapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.animeapp.models.PageItem;
import java.util.List;
import com.example.animeapp.R;

public class PageAdapter extends RecyclerView.Adapter<PageAdapter.PageViewHolder> {
    private List<PageItem> pages;
    private OnPageClickListener listener;

    public interface OnPageClickListener {
        void onPageClick(String url);
    }

    public PageAdapter(List<PageItem> pages, OnPageClickListener listener) {
        this.pages = pages;
        this.listener = listener;
    }

    public void setPages(List<PageItem> newPages) {
        pages.clear();
        pages.addAll(newPages);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_item, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        PageItem page = pages.get(position);
        holder.button.setText(page.getPageNumber());
        holder.button.setOnClickListener(v -> listener.onPageClick(page.getUrl()));
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        Button button;

        public PageViewHolder(View itemView) {
            super(itemView);
            button = itemView.findViewById(R.id.page_button);
        }
    }
}