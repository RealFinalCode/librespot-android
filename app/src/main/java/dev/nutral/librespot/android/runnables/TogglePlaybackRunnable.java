package dev.nutral.librespot.android.runnables;

import dev.nutral.librespot.android.LibrespotHolder;
import xyz.gianlu.librespot.player.Player;

public class TogglePlaybackRunnable implements Runnable {
    @Override
    public void run() {
        Player player = LibrespotHolder.getPlayer();
        if(player == null) return;

        player.playPause();
    }
}
