package dev.nutral.librespot.android.runnables;

import android.os.Handler;
import android.os.Looper;

import org.jetbrains.annotations.NotNull;

import dev.nutral.librespot.android.LibrespotHolder;
import dev.nutral.librespot.android.runnables.callbacks.SimpleCallback;
import xyz.gianlu.librespot.player.Player;

public class PreviousRunnable implements Runnable {
    private final SimpleCallback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public PreviousRunnable(SimpleCallback callback) {
        this.callback = callback;
    }

    public PreviousRunnable() {
        this(null);
    }

    @Override
    public void run() {
        Player player = LibrespotHolder.getPlayer();
        if (player == null) return;

        player.previous();
        if (callback == null) return;
        handler.post(callback::done);
    }
}