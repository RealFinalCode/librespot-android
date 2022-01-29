package dev.nutral.librespot.android.runnables;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.InputStream;

import dev.nutral.librespot.android.runnables.callbacks.OneParameterCallback;
import dev.nutral.librespot.android.utils.ImageManager;

public class DownloadImageRunnable implements Runnable {

    private static final String TAG = "DownloadImageTask";

    private final String url, key;
    private final OneParameterCallback<Bitmap> callback;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public DownloadImageRunnable(@NonNull String url, @NonNull String key, @NonNull OneParameterCallback<Bitmap> callback) {
        this.url = url;
        this.key = key;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            Log.d(TAG, "doInBackground: Image downloaded ->" + url);
            InputStream in = new java.net.URL(url).openStream();
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            ImageManager.addBitmapToCache(key, bitmap);
            handler.post(() -> callback.done(bitmap));
        } catch (Exception e) {
            Log.e(TAG, "doInBackground: Failed to download image", e);
            e.printStackTrace();
        }
    }
}