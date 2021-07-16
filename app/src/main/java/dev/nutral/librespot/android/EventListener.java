package dev.nutral.librespot.android;

import android.app.Activity;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.util.Log;
import android.widget.ImageButton;

import androidx.annotation.UiThread;

import com.spotify.metadata.Metadata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import dev.nutral.librespot.android.mediaSession.MediaControlManager;
import xyz.gianlu.librespot.audio.MetadataWrapper;
import xyz.gianlu.librespot.metadata.ImageId;
import xyz.gianlu.librespot.metadata.PlayableId;
import xyz.gianlu.librespot.player.Player;

public class EventListener implements Player.EventsListener {

    private static final String TAG = EventListener.class.getSimpleName();

    private final MediaControlManager mediaControlManager = MediaControlManager.getInstance();
    private final ImageButton togglePlayback;
    private final MediaSession mediaSession;

    public EventListener(@NotNull Activity activity, @NotNull MediaSession mediaSession) {
        this.togglePlayback  = activity.findViewById(R.id.toggle_playback);
        this.mediaSession = mediaSession;
    }

    @Override
    public void onContextChanged(@NotNull Player player, @NotNull String s) {

    }

    @Override
    public void onTrackChanged(@NotNull Player player, @NotNull PlayableId playableId, @Nullable MetadataWrapper metadataWrapper, boolean b) {

    }

    @UiThread
    @Override
    public void onPlaybackEnded(@NotNull Player player) {
        togglePlayback.setImageResource(android.R.drawable.ic_media_play);

        mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setState(PlaybackState.STATE_STOPPED, player.time(), 1)
                // TODO: Make seeking possible
//                .setActions(PlaybackState.ACTION_SEEK_TO)
                .build());
    }

    @UiThread
    @Override
    public void onPlaybackPaused(@NotNull Player player, long l) {
        togglePlayback.setImageResource(android.R.drawable.ic_media_play);

        mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setState(PlaybackState.STATE_PAUSED, player.time(), 1)
                .setActions(PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_PLAY | PlaybackState.ACTION_SKIP_TO_NEXT)
                // TODO: Make seeking possible
//                .setActions(PlaybackState.ACTION_SEEK_TO)
                .build());
        mediaControlManager.showNotification();
    }

    @UiThread
    @Override
    public void onPlaybackResumed(@NotNull Player player, long l) {
        togglePlayback.setImageResource(android.R.drawable.ic_media_pause);

        mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, player.time(), 1)
                .setActions(PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT)
                // TODO: Make seeking possible
//                .setActions(PlaybackState.ACTION_SEEK_TO)
                .build());
        mediaControlManager.showNotification();
    }

    @Override
    public void onTrackSeeked(@NotNull Player player, long l) {
        mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, player.time(), 1)
                .setActions(PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT)
                // TODO: Make seeking possible
//                .setActions(PlaybackState.ACTION_SEEK_TO)
                .build());
        mediaControlManager.showNotification();
    }

    @Override
    public void onMetadataAvailable(@NotNull Player player, @NotNull MetadataWrapper metadataWrapper) {
        Log.i(TAG, "onMetadataAvailable: Metadata found!");
        String albumArtUrl = "";
        Metadata.ImageGroup coverImages = metadataWrapper.getCoverImage();

        if(coverImages != null) {
            ImageId imageFileId = ImageId.biggestImage(coverImages);

            albumArtUrl = "https://i.scdn.co/image/" + imageFileId.hexId();
        }

        mediaSession.setMetadata(new MediaMetadata.Builder()
                .putText(MediaMetadata.METADATA_KEY_TITLE, metadataWrapper.getName())
                .putText(MediaMetadata.METADATA_KEY_ARTIST, metadataWrapper.getArtist())
                .putText(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, albumArtUrl)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, metadataWrapper.duration())
                .build());
        mediaControlManager.showNotification();
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
