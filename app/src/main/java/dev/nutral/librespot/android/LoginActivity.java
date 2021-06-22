package dev.nutral.librespot.android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;

import com.spotify.connectstate.Connect;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Locale;

import dev.nutral.librespot.android.databinding.ActivityLoginBinding;
import dev.nutral.librespot.android.utils.Utils;
import xyz.gianlu.librespot.core.Session;
import xyz.gianlu.librespot.mercury.MercuryClient;

public final class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLoginBinding binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        LoginCallback callback = new LoginCallback() {
            @Override
            public void loggedIn() {
                Toast.makeText(LoginActivity.this, R.string.loggedIn, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            }

            @Override
            public void failedLoggingIn(@NotNull Exception ex) {
                Toast.makeText(LoginActivity.this, R.string.failedLoggingIn, Toast.LENGTH_SHORT).show();
            }
        };

        binding.login.setOnClickListener(v -> {
            String username = Utils.getText(binding.username);
            String password = Utils.getText(binding.password);
            if (username.isEmpty() || password.isEmpty())
                return;

            File credentialsFile = Utils.getCredentialsFile(this);
            new LoginThread(username, password, credentialsFile, callback).start();
        });
    }

    @UiThread
    private interface LoginCallback {
        void loggedIn();

        void failedLoggingIn(@NotNull Exception ex);
    }

    private static class LoginThread extends Thread {
        private final String username;
        private final String password;
        private final File credentialsFile;
        private final LoginCallback callback;
        private final Handler handler;

        LoginThread(String username, String password, File credentialsFile, LoginCallback callback) {
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
                Log.i(TAG, "Logged in as: " + session.username());

                LibrespotHolder.set(session);

                handler.post(callback::loggedIn);
            } catch (IOException |
                    GeneralSecurityException |
                    Session.SpotifyAuthenticationException |
                    MercuryClient.MercuryException ex) {
                Log.e(TAG, "Session creation failed!", ex);
                handler.post(() -> callback.failedLoggingIn(ex));
            }
        }
    }
}