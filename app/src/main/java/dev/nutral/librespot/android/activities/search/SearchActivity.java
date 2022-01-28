package dev.nutral.librespot.android.activities.search;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.widget.SearchView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.JsonObject;

import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.nutral.librespot.android.R;
import dev.nutral.librespot.android.runnables.SearchRunnable;

public class SearchActivity extends AppCompatActivity {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private SearchStateAdapter searchStateAdapter;

    private String lastQuerySearched;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_search);
        super.onCreate(savedInstanceState);

        // Tabs
        searchStateAdapter = new SearchStateAdapter(this);
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(searchStateAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        new TabLayoutMediator(tabs, viewPager, (tab, position) -> tab.setText(SearchStateAdapter.getTabTitle(position))).attach();

        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnSearchClickListener((e) -> getSupportActionBar().hide());
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
//                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                performSearch(newText);
                return true;
            }
        });
    }

    private void performSearch(String query) {
        if(query.equals(lastQuerySearched) || query.trim().isEmpty())
            return;
        lastQuerySearched = query;
        executorService.execute(new SearchRunnable(query, result -> {
            JsonObject resultsFromSearch = result.get("results").getAsJsonObject(); // keySet: [tracks, albums, artists, playlists, profiles, genres, topHit, shows, audioepisodes]
            searchStateAdapter.setResponseData(resultsFromSearch);
        }));
    }
}