package dev.nutral.librespot.android;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import dev.nutral.librespot.android.utils.ImageManager;
import xyz.gianlu.librespot.audio.decoders.Decoders;
import xyz.gianlu.librespot.audio.format.SuperAudioFormat;
import dev.nutral.librespot.player.decoders.AndroidNativeDecoder;
import dev.nutral.librespot.player.decoders.TremoloVorbisDecoder;

public final class LibrespotApp extends Application {
    private static final String TAG = LibrespotApp.class.getSimpleName();
    public static final String MUSIC_INFO_CHANNEL_ID = "mInfo";

    static {
        Decoders.registerDecoder(SuperAudioFormat.VORBIS, AndroidNativeDecoder.class);
        Decoders.registerDecoder(SuperAudioFormat.MP3, AndroidNativeDecoder.class);

        if (isArm()) {
            Decoders.registerDecoder(SuperAudioFormat.VORBIS, 0, TremoloVorbisDecoder.class);
            Log.i(TAG, "Using ARM optimized Vorbis decoder");
        }
    }

    private static boolean isArm() {
        for (String abi : Build.SUPPORTED_ABIS)
            if (abi.contains("arm"))
                return true;

        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel notificationChannel = new NotificationChannel(MUSIC_INFO_CHANNEL_ID, getResources().getString(R.string.music_playback_channel_name), NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.enableVibration(false);
        notificationChannel.setDescription(getResources().getString(R.string.music_playback_info_description));
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel);
        ImageManager.init(getApplicationContext());
        Runtime.getRuntime().addShutdownHook(new Thread(ImageManager::close));
    }
}
