package dev.nutral.librespot.android;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import xyz.gianlu.librespot.audio.decoders.Decoders;
import xyz.gianlu.librespot.audio.format.SuperAudioFormat;
import dev.nutral.librespot.player.decoders.AndroidNativeDecoder;
import dev.nutral.librespot.player.decoders.TremoloVorbisDecoder;

public final class LibrespotApp extends Application {
    private static final String TAG = LibrespotApp.class.getSimpleName();

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
}
