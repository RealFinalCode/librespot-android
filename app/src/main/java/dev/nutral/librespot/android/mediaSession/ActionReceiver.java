package dev.nutral.librespot.android.mediaSession;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.Serializable;

import dev.nutral.librespot.android.utils.LibrespotHolder;
import xyz.gianlu.librespot.player.Player;

public class ActionReceiver extends BroadcastReceiver {

    private static final String TAG = ActionReceiver.class.getSimpleName();

    private final MediaControlManager mediaControlManager = MediaControlManager.getInstance();

    @Override
    public void onReceive(Context context, Intent intent) {
        Serializable actionTypeSerialized = intent.getSerializableExtra("action_type");
        Log.d(TAG, "onReceive: " + actionTypeSerialized);
        Log.d(TAG, "onReceive: " + actionTypeSerialized.getClass().getSimpleName());
        if(!(actionTypeSerialized instanceof ActionType)) {
            Log.e(TAG, "onReceive: Intent Extra (action_type) is not a instance of ActionType");
            return;
        }

        ActionType actionType = (ActionType) actionTypeSerialized;

        Log.d(TAG, "onReceive: " + actionType);

        Player player = LibrespotHolder.getPlayer();
        if (player == null) return;

        switch (actionType) {
            case PLAY_PAUSE:
                player.playPause();
                break;
            case SKIP_NEXT:
                player.next();
                break;
            case SKIP_PREVIOUS:
                try {
                    player.previous();
                } catch (IllegalStateException e) {
                    Log.e(TAG, "run: skip to previous failed", e);
                }
                break;
            default:
                Log.d(TAG, "onReceive: unknown Action Type -> " + actionType);
                return;

        }

        // Always update the Notification except in the default Case
        mediaControlManager.showNotification();
    }
}
