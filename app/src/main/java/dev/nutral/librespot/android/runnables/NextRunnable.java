package dev.nutral.librespot.android.runnables;

import android.os.Handler;
import android.os.Looper;

import org.jetbrains.annotations.NotNull;

import dev.nutral.librespot.android.LibrespotHolder;
import dev.nutral.librespot.android.runnables.callbacks.SimpleCallback;
import xyz.gianlu.librespot.player.Player;

public class NextRunnable implements Runnable {
    private final SimpleCallback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public NextRunnable(SimpleCallback callback) {
        this.callback = callback;
    }

    public NextRunnable() {
        this(null);
    }

    @Override
    public void run() {
        Player player = LibrespotHolder.getPlayer();
        if (player == null) return;

        player.next();
        if (callback == null) return;
        handler.post(callback::done);
    }
}