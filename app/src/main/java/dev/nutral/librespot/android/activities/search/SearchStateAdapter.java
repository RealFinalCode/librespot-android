package dev.nutral.librespot.android.activities.search;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.gson.JsonObject;

import dev.nutral.librespot.android.R;

public class SearchStateAdapter extends FragmentStateAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[] {R.string.search_category_best, R.string.search_category_tracks, R.string.search_category_playlists, R.string.search_category_artist, R.string.search_category_album, R.string.search_category_podcast, R.string.search_category_profiles};
    private static final String[] RESPONSE_JSON_ARRAY_NAMES = {"topHit", "tracks", "playlists", "artists", "albums", "shows", "profiles"};

    private SearchTabFragment[] createdFragments = new SearchTabFragment[TAB_TITLES.length];
    private MutableLiveData<JsonObject> responseData = new MutableLiveData<>();

    public SearchStateAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        SearchTabFragment frag = SearchTabFragment.newInstance(RESPONSE_JSON_ARRAY_NAMES[position], responseData);
        createdFragments[position] = frag;
        return frag;
    }

    public void setResponseData(JsonObject response) {
        responseData.setValue(response);
    }

    public static int getTabTitle(int position) {
        return TAB_TITLES[position];
    }

    @Override
    public int getItemCount() {
        return TAB_TITLES.length;
    }
}
