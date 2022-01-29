package dev.nutral.librespot.android.runnables;


import android.os.Handler;
import android.os.Looper;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import dev.nutral.librespot.android.utils.LibrespotHolder;
import dev.nutral.librespot.android.runnables.callbacks.OneParameterCallback;
import xyz.gianlu.librespot.core.SearchManager;
import xyz.gianlu.librespot.core.Session;

public class SearchRunnable implements Runnable {
    private final String query;
    private final OneParameterCallback<JsonObject> callback;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public SearchRunnable(@NotNull CharSequence query, @NotNull OneParameterCallback<JsonObject> callback) {
        this.query = query.toString();
        this.callback = callback;
    }

    @Override
    public void run() {
        Session session = LibrespotHolder.getSession();
        if (session == null) return;

        SearchManager searchManager = session.search();

        try {
            JsonObject result = searchManager.request(new SearchManager.SearchRequest(query).limit(20));

            handler.post(() -> callback.done(result));
        } catch (IOException e) {
            e.printStackTrace();
            handler.post(() -> callback.done(null));
        }
    }
}