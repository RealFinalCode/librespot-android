package dev.nutral.librespot.android.search;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.text.method.MovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import dev.nutral.librespot.android.R;

public class CustomResultList extends ArrayAdapter {

    private static final String TAG = CustomResultList.class.getSimpleName();

    private final Activity context;
    private final JsonArray results;

    public CustomResultList(@NonNull Activity context, JsonArray results) {
        super(context, R.layout.result_item,
                StreamSupport.stream(results.spliterator(), false).map(jsonElement -> jsonElement.getAsJsonObject().get("uri").getAsString()).collect(Collectors.toList()));

        this.context = context;
        this.results = results;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View item = convertView;

        LayoutInflater inflater = context.getLayoutInflater();
        if (convertView == null)
            item = inflater.inflate(R.layout.result_item, null, true);

        TextView name = (TextView) item.findViewById(R.id.textViewName);
        TextView artists = (TextView) item.findViewById(R.id.textViewArtists);
        ImageView cover = (ImageView) item.findViewById(R.id.imageViewCover);

        // Name
        name.setText(results.get(position).getAsJsonObject().get("name").getAsString());
        // Artists
        JsonArray artistsArray = results.get(position).getAsJsonObject().get("artists").getAsJsonArray();
        String artistsNames = StreamSupport.stream(artistsArray.spliterator(), false).map(jsonElement -> jsonElement.getAsJsonObject().get("name").getAsString()).collect(Collectors.joining(", "));
        artists.setText(artistsNames);
        // Reset Image to Set it again
        cover.setImageBitmap(null);
        new DownloadImageTask(cover).execute(results.get(position).getAsJsonObject().get("image").getAsString());

        return item;
    }

    private static final HashMap<String, Bitmap> imagesForUri = new HashMap<>();

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        private final ImageView bmImage;

        public DownloadImageTask(ImageView imageView) {
            super();
            this.bmImage = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            if (imagesForUri.containsKey(strings[0]))
                return imagesForUri.get(strings[0]);

            Bitmap bitmap = null;
            try {
                Log.d(TAG, "doInBackground: Image downloaded ->" + strings[0]);
                InputStream in = new java.net.URL(strings[0]).openStream();
                bitmap = BitmapFactory.decodeStream(in);
                imagesForUri.put(strings[0], bitmap);
            } catch (Exception e) {
                Log.e(TAG, "doInBackground: ", e);
                e.printStackTrace();
            }

            return bitmap;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

}
