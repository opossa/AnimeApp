package com.example.animeapp.activities;

import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.animeapp.R;
import com.example.animeapp.adapters.HistoryAdapter;
import com.example.animeapp.database.DatabaseHelper;
import com.example.animeapp.models.HistoryItem;
import java.util.List;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class HistoryActivity extends AppCompatActivity {
    private List<HistoryItem> historyList;
    private HistoryAdapter adapter;
    private DatabaseHelper dbHelper;
    private TextView emptyText;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        
        initializeViews();
        setupToolbar();
        loadHistoryData();
        setupSwipeToDelete();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.history_recycler_view);
        emptyText = findViewById(R.id.empty_history_text);
        dbHelper = new DatabaseHelper(this);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void loadHistoryData() {
        historyList = dbHelper.getAllHistory();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (historyList == null || historyList.isEmpty()) {
            showEmptyState();
        } else {
            showHistoryList();
        }
    }

    private void showEmptyState() {
        emptyText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showHistoryList() {
        emptyText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter = new HistoryAdapter(historyList, this::onHistoryItemClick);
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeToDeleteCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, 
                                 @NonNull RecyclerView.ViewHolder viewHolder, 
                                 @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                deleteItem(viewHolder.getAdapterPosition());
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                   @NonNull RecyclerView.ViewHolder viewHolder,
                                   float dX, float dY, int actionState, boolean isCurrentlyActive) {
                drawSwipeDecorator(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(swipeToDeleteCallback).attachToRecyclerView(recyclerView);
    }

    private void deleteItem(int position) {
        if (position != RecyclerView.NO_POSITION && position < historyList.size()) {
            HistoryItem item = historyList.get(position);
            dbHelper.deleteHistory(item.getId());
            historyList.remove(position);
            adapter.notifyItemRemoved(position);
            
            if (historyList.isEmpty()) {
                showEmptyState();
            }
            
            Toast.makeText(this, "ลบรายการแล้ว", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawSwipeDecorator(Canvas c, RecyclerView recyclerView, 
                                  RecyclerView.ViewHolder viewHolder,
                                  float dX, float dY, int actionState, 
                                  boolean isCurrentlyActive) {
        new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, 
            dX, dY, actionState, isCurrentlyActive)
            .addSwipeLeftBackgroundColor(ContextCompat.getColor(this, R.color.red))
            .addSwipeLeftActionIcon(R.drawable.ic_delete)
            .create()
            .decorate();
    }

    private void onHistoryItemClick(HistoryItem item) {
        AnimeDetailActivity.start(this, item.getAnimeUrl(), item.getTitle());
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}