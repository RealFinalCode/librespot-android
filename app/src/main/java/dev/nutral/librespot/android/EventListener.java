package dev.nutral.librespot.android;

import android.app.Activity;
import android.widget.ImageButton;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import xyz.gianlu.librespot.audio.MetadataWrapper;
import xyz.gianlu.librespot.metadata.PlayableId;
import xyz.gianlu.librespot.player.Player;

public class EventListener implements Player.EventsListener {

    private ImageButton togglePlayback;

    public EventListener(Activity activity) {
        this.togglePlayback  = activity.findViewById(R.id.toggle_playback);
    }

    @Override
    public void onContextChanged(@NotNull Player player, @NotNull String s) {

    }

    @Override
    public void onTrackChanged(@NotNull Player player, @NotNull PlayableId playableId, @Nullable MetadataWrapper metadataWrapper, boolean b) {

    }

    @Override
    public void onPlaybackEnded(@NotNull Player player) {
        togglePlayback.setImageResource(android.R.drawable.ic_media_play);
    }

    @Override
    public void onPlaybackPaused(@NotNull Player player, long l) {
        togglePlayback.setImageResource(android.R.drawable.ic_media_play);
    }

    @Override
    public void onPlaybackResumed(@NotNull Player player, long l) {
        togglePlayback.setImageResource(android.R.drawable.ic_media_pause);
    }

    @Override
    public void onTrackSeeked(@NotNull Player player, long l) {

    }

    @Override
    public void onMetadataAvailable(@NotNull Player player, @NotNull MetadataWrapper metadataWrapper) {

    }

    @Override
    public void onPlaybackHaltStateChanged(@NotNull Player player, boolean b, long l) {

    }

    @Override
    public void onInactiveSession(@NotNull Player player, boolean b) {

    }

    @Override
    public void onVolumeChanged(@NotNull Player player, @Range(from = 0L, to = 1L) float v) {

    }

    @Override
    public void onPanicState(@NotNull Player player) {

    }
}
