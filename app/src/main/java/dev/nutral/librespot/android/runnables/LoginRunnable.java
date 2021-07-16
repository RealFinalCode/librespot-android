package dev.nutral.librespot.android.runnables;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.spotify.connectstate.Connect;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Locale;

import dev.nutral.librespot.android.utils.LibrespotHolder;
import dev.nutral.librespot.android.runnables.callbacks.LoginCallback;
import xyz.gianlu.librespot.core.Session;
import xyz.gianlu.librespot.mercury.MercuryClient;

public class LoginRunnable implements Runnable {
    private static final String TAG = LoginRunnable.class.getSimpleName();

    private final String username;
    private final String password;
    private final File credentialsFile;
    private final LoginCallback callback;
    private final Handler handler;

    public LoginRunnable(String username, String password, File credentialsFile, LoginCallback callback) {
        this.username = username;
        this.password = password;
        this.credentialsFile = credentialsFile;
        this.callback = callback;
        this.handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void run() {
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

            Session session = builder.userPass(username, password).create();
            Log.i(TAG, "run: Logged in as: " + session.username());

            LibrespotHolder.set(session);

            handler.post(callback::loggedIn);
        } catch (IOException | GeneralSecurityException | Session.SpotifyAuthenticationException | MercuryClient.MercuryException ex) {
            Log.e(TAG, "run: Session creation failed!", ex);
            handler.post(() -> callback.failedLoggingIn(ex));
        }
    }
}