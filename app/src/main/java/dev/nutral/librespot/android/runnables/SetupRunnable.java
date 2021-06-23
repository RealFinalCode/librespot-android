package dev.nutral.librespot.android.runnables;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.spotify.connectstate.Connect;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Locale;

import dev.nutral.librespot.android.LibrespotHolder;
import dev.nutral.librespot.android.MainActivity;
import dev.nutral.librespot.android.runnables.callbacks.SetupCallback;
import dev.nutral.librespot.android.sink.AndroidSinkOutput;
import xyz.gianlu.librespot.core.Session;
import xyz.gianlu.librespot.mercury.MercuryClient;
import xyz.gianlu.librespot.player.Player;
import xyz.gianlu.librespot.player.PlayerConfiguration;

public class SetupRunnable implements Runnable {

    private static final String TAG = SetupRunnable.class.getSimpleName();

    private final File credentialsFile;
    private final SetupCallback callback;
    private final Handler handler;

    public SetupRunnable(@NotNull File credentialsFile, @NotNull SetupCallback callback) {
        this.credentialsFile = credentialsFile;
        this.callback = callback;
        this.handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void run() {
        Session session;
        if (LibrespotHolder.hasSession()) {
            session = LibrespotHolder.getSession();
            if (session == null) throw new IllegalStateException();
        } else if (credentialsFile.exists() && credentialsFile.canRead()) {
            try {
                Session.Configuration conf = new Session.Configuration.Builder()
                        .setStoreCredentials(true)
                        .setStoredCredentialsFile(credentialsFile)
                        .setCacheEnabled(false)
                        .build();

                Session.Builder builder = new Session.Builder(conf)
                        .setPreferredLocale(Locale.getDefault().getLanguage())
                        .setDeviceType(Connect.DeviceType.SMARTPHONE)
                        .setDeviceId(null).setDeviceName("librespot-android");

                session = builder.stored(credentialsFile).create();
                Log.i(TAG, "Logged in as: " + session.username());

                LibrespotHolder.set(session);
            } catch (IOException |
                    GeneralSecurityException |
                    Session.SpotifyAuthenticationException |
                    MercuryClient.MercuryException ex) {
                Log.e(TAG, "Session creation failed!", ex);
                handler.post(() -> callback.failedGettingReady(ex));
                return;
            }
        } else {
            handler.post(callback::notLoggedIn);
            return;
        }

        Player player;
        if (LibrespotHolder.hasPlayer()) {
            player = LibrespotHolder.getPlayer();
            if (player == null) throw new IllegalStateException();
        } else {
            PlayerConfiguration configuration = new PlayerConfiguration.Builder()
                    .setOutput(PlayerConfiguration.AudioOutput.CUSTOM)
                    .setOutputClass(AndroidSinkOutput.class.getName())
                    .build();

            player = new Player(configuration, session);
            LibrespotHolder.set(player);
        }

        try {
            player.waitReady();
        } catch (InterruptedException ex) {
            LibrespotHolder.clear();
            return;
        }

        handler.post(() -> callback.playerReady(session.username()));
    }
}