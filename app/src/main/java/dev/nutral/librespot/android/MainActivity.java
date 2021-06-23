package dev.nutral.librespot.android;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;

import com.spotify.connectstate.Connect;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.nutral.librespot.android.databinding.ActivityMainBinding;
import dev.nutral.librespot.android.runnables.PlayRunnable;
import dev.nutral.librespot.android.runnables.TogglePlaybackRunnable;
import dev.nutral.librespot.android.search.SearchActivity;
import dev.nutral.librespot.android.sink.AndroidSinkOutput;
import dev.nutral.librespot.android.runnables.callbacks.SimpleCallback;
import dev.nutral.librespot.android.utils.Utils;
import xyz.gianlu.librespot.audio.MetadataWrapper;
import xyz.gianlu.librespot.core.Session;
import xyz.gianlu.librespot.mercury.MercuryClient;
import xyz.gianlu.librespot.metadata.PlayableId;
import xyz.gianlu.librespot.player.Player;
import xyz.gianlu.librespot.player.PlayerConfiguration;

public final class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LibrespotHolder.clear();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        File credentialsFile = Utils.getCredentialsFile(this);
        if (!credentialsFile.exists() || !credentialsFile.canRead()) {
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return;
        }

        binding.logout.setOnClickListener(v -> {
            credentialsFile.delete();
            LibrespotHolder.clear();
            startActivity(new Intent(MainActivity.this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });


        binding.togglePlayback.setOnClickListener(v -> {
            executorService.execute(new TogglePlaybackRunnable());
            Log.d(TAG, "onCreate: " + LibrespotHolder.getPlayer().currentMetadata().duration());
        });

        binding.prev.setOnClickListener((v) ->
                executorService.execute(new PrevRunnable(null)));

        binding.next.setOnClickListener((v) ->
                executorService.execute(new NextRunnable(null)));

        binding.search.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SearchActivity.class)));

        executorService.submit(new SetupRunnable(credentialsFile, new SetupCallback() {
            @Override
            public void playerReady(@NotNull String username) {
                Toast.makeText(MainActivity.this, R.string.playerReady, Toast.LENGTH_SHORT).show();
                binding.username.setText(username);
                binding.playControls.setVisibility(View.VISIBLE);
                binding.search.setVisibility(View.VISIBLE);
                // For play/pause and stuff like that
                LibrespotHolder.getPlayer().addEventsListener(new EventListener(MainActivity.this));
            }

            @Override
            public void notLoggedIn() {
                startActivity(new Intent(MainActivity.this, LoginActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            }

            @Override
            public void failedGettingReady(@NotNull Exception ex) {
                Toast.makeText(MainActivity.this, R.string.somethingWentWrong, Toast.LENGTH_SHORT).show();
                binding.playControls.setVisibility(View.GONE);
            }
        }));
    }

    @UiThread
    private interface SetupCallback {
        void playerReady(@NotNull String username);

        void notLoggedIn();

        void failedGettingReady(@NotNull Exception ex);
    }

    private static class SetupRunnable implements Runnable {
        private final File credentialsFile;
        private final SetupCallback callback;
        private final Handler handler;

        SetupRunnable(@NotNull File credentialsFile, @NotNull SetupCallback callback) {
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

    private static class PrevRunnable implements Runnable {
        private final SimpleCallback callback;
        private final Handler handler = new Handler(Looper.getMainLooper());

        PrevRunnable(@NotNull SimpleCallback callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            Player player = LibrespotHolder.getPlayer();
            if (player == null) return;

            player.previous();
            if (callback == null) return;
            handler.post(callback::done);
        }
    }

    private static class NextRunnable implements Runnable {
        private final SimpleCallback callback;
        private final Handler handler = new Handler(Looper.getMainLooper());

        NextRunnable(@NotNull SimpleCallback callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            Player player = LibrespotHolder.getPlayer();
            if (player == null) return;

            player.next();
            if (callback == null) return;
            handler.post(callback::done);
        }
    }
}
