package dev.nutral.librespot.android.mediaSession;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.util.Log;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.nutral.librespot.android.LibrespotApp;
import dev.nutral.librespot.android.R;
import dev.nutral.librespot.android.utils.LibrespotHolder;

import static dev.nutral.librespot.android.mediaSession.ActionType.*;

public class MediaControlManager {

    private static final String TAG = MediaControlManager.class.getSimpleName();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static MediaControlManager instance;

    private MediaSession mediaSession;
    private Context context;

    public void createMediaSession(Context context) {
        this.context = context;

        mediaSession = new MediaSession(context, "LibrespotSession");
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                Log.d(TAG, "onPlay: ");
                performAction(PLAY_PAUSE);
            }

            @Override
            public void onPause() {
                Log.d(TAG, "onPause: ");
                performAction(PLAY_PAUSE);
            }

            @Override
            public void onSkipToNext() {
                Log.d(TAG, "onSkipToNext: ");
                performAction(SKIP_NEXT);
            }

            @Override
            public void onSkipToPrevious() {
                Log.d(TAG, "onSkipToPrevious: ");
                performAction(SKIP_PREVIOUS);
            }
        });
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
    }

    public void showNotification() {
        if(!LibrespotHolder.hasPlayer())
            return;
        executorService.execute(() -> {
            String trackTitle = "Track Title", artistAlbum = "Artist - Album";
            Bitmap albumArt = null;
            MediaController controller = mediaSession.getController();
            MediaMetadata metadata = controller.getMetadata();

            if(metadata != null) {
                trackTitle = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                artistAlbum = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);

                String albumArtUrl = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI);
                if(!albumArtUrl.equals("")) {
                    try {
                        albumArt = BitmapFactory.decodeStream(new URL(albumArtUrl).openStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            Notification.Style mediaStyle = new Notification.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken())
                    .setShowActionsInCompactView(0, 1, 2);

            Notification.Builder builder = new Notification.Builder(context, LibrespotApp.MUSIC_INFO_CHANNEL_ID)
                    .addAction(new Notification.Action.Builder(Icon.createWithResource("", android.R.drawable.ic_media_previous), "Prev", PendingIntent.getBroadcast(context, 0, new Intent(context, ActionReceiver.class).putExtra("action_type", SKIP_PREVIOUS), 0)).build())
                    .setStyle(mediaStyle)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(trackTitle)
                    .setOnlyAlertOnce(true)
                    .setContentText(artistAlbum);

            if(controller.getPlaybackState() == null) {
                Log.e(TAG, "showNotification: PlaybackState is null");
                return;
            }

            // Play / Pause Action
            if(controller.getPlaybackState().getState() == PlaybackState.STATE_PLAYING) {
                builder.addAction(new Notification.Action.Builder(Icon.createWithResource("", android.R.drawable.ic_media_pause), "Pause", PendingIntent.getBroadcast(context, 1, new Intent(context, ActionReceiver.class).putExtra("action_type", PLAY_PAUSE), 0)).build());
            } else {
                builder.addAction(new Notification.Action.Builder(Icon.createWithResource("", android.R.drawable.ic_media_play), "Play", PendingIntent.getBroadcast(context, 2, new Intent(context, ActionReceiver.class).putExtra("action_type", PLAY_PAUSE), 0)).build());
            }
            builder.addAction(new Notification.Action.Builder(Icon.createWithResource("", android.R.drawable.ic_media_next), "Next", PendingIntent.getBroadcast(context, 3, new Intent(context, ActionReceiver.class).putExtra("action_type", SKIP_NEXT), 0)).build());

            // Album Art
            if(albumArt != null) {
                builder.setLargeIcon(albumArt);
            }

            Notification notification = builder.build();
            context.getSystemService(NotificationManager.class).notify(1, notification);
        });
    }

    /**
     *
     * @param actionType The
     */
    public void performAction(ActionType actionType) {
        Intent intent = new Intent(context, ActionReceiver.class);
        intent.putExtra("action_type", actionType);
        context.sendBroadcast(intent);
    }

    public void releaseMediaSession() {
        if(mediaSession != null) {
            mediaSession.release();
        }
    }

    /*
        Getters & Setters
     */

    public static MediaControlManager getInstance() {
        if(instance == null) {
            instance = new MediaControlManager();
        }

        return instance;
    }

    public MediaSession getMediaSession() {
        return mediaSession;
    }
}
