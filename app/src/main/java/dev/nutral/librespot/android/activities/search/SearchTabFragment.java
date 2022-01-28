package dev.nutral.librespot.android.activities.search;

import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.spotify.connectstate.Connect;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.nutral.librespot.android.R;
import dev.nutral.librespot.android.runnables.PlayRunnable;
import dev.nutral.librespot.android.utils.LibrespotHolder;
import xyz.gianlu.librespot.player.Player;

public class SearchTabFragment extends Fragment {

    private static final String TAG = "SearchTabFragment";

    private static final String ARG_JSON_ARRAY_NAME = "json_array_name";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static SearchTabFragment newInstance(String jsonArrayName, MutableLiveData<JsonObject> responseData) {
        SearchTabFragment fragment = new SearchTabFragment(responseData);
        Bundle bundle = new Bundle();
        bundle.putString(ARG_JSON_ARRAY_NAME, jsonArrayName);
        fragment.setArguments(bundle);
        return fragment;
    }

    private MutableLiveData<JsonObject> responseData;

    public SearchTabFragment(MutableLiveData<JsonObject> responseData) {
        this.responseData = responseData;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_search_tab, container, false);

        final ListView resultListView = root.findViewById(R.id.resultList);

        responseData.observe(getViewLifecycleOwner(), response -> {
            String jsonName = getArguments().getString(ARG_JSON_ARRAY_NAME);
            JsonObject resultObj = new JsonObject();
            resultObj.addProperty("type", jsonName);
            JsonArray results;
            if (jsonName.equals("topHit")) {
                results = new JsonArray();
                for (JsonElement best : response.getAsJsonObject(jsonName).getAsJsonArray("hits")) {
                    JsonObject tmp = best.getAsJsonObject();
                    // TODO: !Needs rework! Works just for now until the search changes the returned parameters
                    if (tmp.has("artists")) {
                        if (tmp.has("lyricsMatch")) {
                            tmp.addProperty("type", "tracks");
                        } else {
                            tmp.addProperty("type", "albums");
                        }
                    } else if (tmp.has("author")) {
                        tmp.addProperty("type", "playlists");
                    } else if (tmp.has("showType")) {
                        tmp.addProperty("type", "shows");
                    } else if (tmp.has("image")) {
                        tmp.addProperty("type", "artists");
                    } else {
                        tmp.addProperty("type", "profiles");
                    }
                    results.add(tmp);
                }
                // TODO: sort by priority of some kind
                for (String name : new String[]{"tracks", "playlists", "artists"}) {
                    if (!response.has(name))
                        continue;
                    JsonArray hits = response.getAsJsonObject(name).getAsJsonArray("hits");
                    for (int i = 0; i < Math.min(3, hits.size()); i++) {
                        JsonObject tmp = hits.get(i).getAsJsonObject();
                        tmp.addProperty("type", name);
                        if (results.contains(tmp))
                            continue;
                        results.add(tmp);
                    }
                }
            } else {
                results = response.getAsJsonObject(jsonName).getAsJsonArray("hits");
            }
            resultObj.add("resultArray", results);

            resultListView.setAdapter(new CustomResultList(getActivity(), resultObj));
        });


        resultListView.setOnItemClickListener((parent, view, position, id) -> {
            String trackUri = parent.getItemAtPosition(position).toString();
            Log.d(TAG, "onItemSelected: " + trackUri);

            executorService.execute(new PlayRunnable(trackUri, false, null));
        });

        registerForContextMenu(resultListView);

        return root;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.getId() == R.id.resultList) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.result_list, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int resultPosition = info.position;
        String trackUri = ((ListView) getView().findViewById(R.id.resultList)).getItemAtPosition(resultPosition).toString();

        switch (item.getItemId()) {
            case R.id.add_to_queue:
                Player player = LibrespotHolder.getPlayer();
                if (player == null)
                    return false;

                Log.d(TAG, "onContextItemSelected: " + player);
                player.addToQueue(trackUri);
                break;
            case R.id.add_to_playlist:
                // edit stuff here
                Toast.makeText(getActivity(), "Not yet implemented!", Toast.LENGTH_SHORT).show();
                break;
            case R.id.copy_link:
                // remove stuff here
                Toast.makeText(getActivity(), "Not yet implemented!", Toast.LENGTH_SHORT).show();
                break;
            default:
                return super.onContextItemSelected(item);
        }

        return true;
    }
}
