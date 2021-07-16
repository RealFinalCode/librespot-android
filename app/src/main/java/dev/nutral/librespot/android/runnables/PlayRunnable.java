package dev.nutral.librespot.android.runnables;

import android.os.Handler;
import android.os.Looper;

import org.jetbrains.annotations.NotNull;

import dev.nutral.librespot.android.utils.LibrespotHolder;
import dev.nutral.librespot.android.runnables.callbacks.SimpleCallback;
import xyz.gianlu.librespot.player.Player;

public class PlayRunnable  implements Runnable {
    private final String playUri;
    private final boolean shuffle;
    private final SimpleCallback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public PlayRunnable(@NotNull String playUri, boolean shuffle, SimpleCallback callback) {
        this.playUri = playUri;
        this.callback = callback;
        this.shuffle = shuffle;
    }

    @Override
    public void run() {
        Player player = LibrespotHolder.getPlayer();
        if (player == null) return;

//        player.addToQueue();

        player.load(playUri, true, shuffle);
        if(callback == null) return;
        handler.post(callback::done);
    }
}