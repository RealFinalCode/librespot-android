package dev.nutral.librespot.android.search;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.nutral.librespot.android.utils.LibrespotHolder;
import dev.nutral.librespot.android.R;
import dev.nutral.librespot.android.databinding.ActivitySearchBinding;
import dev.nutral.librespot.android.runnables.PlayRunnable;
import dev.nutral.librespot.android.runnables.SearchRunnable;
import xyz.gianlu.librespot.player.Player;

public class SearchActivity extends AppCompatActivity {

    private static final String TAG = SearchActivity.class.getSimpleName();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySearchBinding b = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());


        b.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                executorService.execute(new SearchRunnable(query, result -> {
                    JsonObject results = result.get("results").getAsJsonObject(); // keySet: [tracks, albums, artists, playlists, profiles, genres, topHit, shows, audioepisodes, topRecommendations]

                    JsonArray tracks = results.getAsJsonObject("tracks").getAsJsonArray("hits");
                    System.out.println(tracks);

                    b.listView.setAdapter(new CustomResultList(SearchActivity.this, tracks));
                }));

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });

        b.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String trackUri = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "onItemSelected: " + trackUri);

                executorService.execute(new PlayRunnable(trackUri, false, null));
            }
        });
        // Long Click Context Menu
        registerForContextMenu(b.listView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if(v.getId() == R.id.listView) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.result_list, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int resultPosition = info.position;
        String trackUri = ((ListView)findViewById(R.id.listView)).getItemAtPosition(resultPosition).toString();

        switch(item.getItemId()) {
            case R.id.add_to_queue:
                Player player = LibrespotHolder.getPlayer();
                if(player == null)
                    return false;

                Log.d(TAG, "onContextItemSelected: " + player);
                player.addToQueue(trackUri);
                break;
            case R.id.add_to_playlist:
                // edit stuff here
                break;
            case R.id.copy_link:
                // remove stuff here
                break;
            default:
                return super.onContextItemSelected(item);
        }

        return true;
    }
}