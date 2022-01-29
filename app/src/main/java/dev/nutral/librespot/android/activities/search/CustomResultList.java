package dev.nutral.librespot.android.activities.search;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import dev.nutral.librespot.android.R;
import dev.nutral.librespot.android.utils.ImageManager;

public class CustomResultList extends ArrayAdapter {

    private static final String TAG = CustomResultList.class.getSimpleName();

    private final FragmentActivity context;
    private final JsonArray results;
    private final String type;

    public CustomResultList(@NonNull FragmentActivity context, JsonObject resultObj) {
        super(context, R.layout.result_item, StreamSupport.stream(resultObj.getAsJsonArray("resultArray").spliterator(), false).map(jsonElement -> jsonElement.getAsJsonObject().get("uri").getAsString()).collect(Collectors.toList()));

        this.context = context;
        this.results = resultObj.getAsJsonArray("resultArray");
        this.type = resultObj.get("type").getAsString();
        Log.d(TAG, "getView: " + type + " -> " + results.get(0).getAsJsonObject().keySet());
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View item = convertView;

        LayoutInflater inflater = context.getLayoutInflater();
        if (convertView == null)
            item = inflater.inflate(R.layout.result_item, null, true);

        TextView name = item.findViewById(R.id.textViewName);
        TextView subText = item.findViewById(R.id.textViewSubText);
        ImageView cover = item.findViewById(R.id.imageViewCover);

        // Name
        name.setText(results.get(position).getAsJsonObject().get("name").getAsString());
        // SubText
        String subTextStr = getSubText(position, type);
        subText.setText(subTextStr);

        if (!type.equals("profiles")) {
            // Reset Image to Set it again
            cover.setImageBitmap(null);
            ImageManager.loadBitmap(results.get(position).getAsJsonObject().get("image").getAsString(), cover);
        }

        return item;
    }

    private String getSubText(int position, String type) {
        // TODO: implement other types
        if (type.equals("tracks") || type.equals("albums")) {
            JsonArray artistsArray = results.get(position).getAsJsonObject().get("artists").getAsJsonArray();
            return StreamSupport.stream(artistsArray.spliterator(), false).map(jsonElement -> jsonElement.getAsJsonObject().get("name").getAsString()).collect(Collectors.joining(", "));
        } else if (type.equals("playlists")) {
            return results.get(position).getAsJsonObject().get("author").getAsString();
        } else if (type.equals("topHit")) {
            String typeOfEntry = results.get(position).getAsJsonObject().get("type").getAsString();
            // TODO: put this in strings.xml
            if (typeOfEntry.equals("artists")) {
                return "Artist";
            } else {
                return typeOfEntry.replace("tracks", "Song").replace("playlists", "Playlist") + " - " + getSubText(position, typeOfEntry);
            }
        }

        return "";
    }

}
