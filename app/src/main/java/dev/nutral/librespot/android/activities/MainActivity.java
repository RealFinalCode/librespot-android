package dev.nutral.librespot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.nutral.librespot.android.EventListener;
import dev.nutral.librespot.android.R;
import dev.nutral.librespot.android.databinding.ActivityMainBinding;
import dev.nutral.librespot.android.mediaSession.ActionType;
import dev.nutral.librespot.android.mediaSession.MediaControlManager;
import dev.nutral.librespot.android.runnables.SetupRunnable;
import dev.nutral.librespot.android.runnables.callbacks.SetupCallback;
import dev.nutral.librespot.android.activities.search.SearchActivity;
import dev.nutral.librespot.android.utils.LibrespotHolder;
import dev.nutral.librespot.android.utils.Utils;

public final class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final MediaControlManager mediaControlManager = MediaControlManager.getInstance();

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

        /*
            Playback Controls
         */
        binding.prev.setOnClickListener(v -> mediaControlManager.performAction(ActionType.SKIP_PREVIOUS));
        binding.togglePlayback.setOnClickListener(v -> mediaControlManager.performAction(ActionType.PLAY_PAUSE));
        binding.next.setOnClickListener(v -> mediaControlManager.performAction(ActionType.SKIP_NEXT));

        /*
            BottomNavigation
         */
        binding.search.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SearchActivity.class)));

        mediaControlManager.createMediaSession(this);

        // Setup the Player
        executorService.submit(new SetupRunnable(credentialsFile, new SetupCallback() {
            @Override
            public void playerReady(@NotNull String username) {
                Log.d(TAG, "playerReady: Player is now ready");
                Toast.makeText(MainActivity.this, R.string.playerReady, Toast.LENGTH_SHORT).show();
                binding.username.setText(username);
                toggleMenuVisibility(binding, true);
                // For play/pause and stuff like that
                LibrespotHolder.getPlayer().addEventsListener(new EventListener(MainActivity.this, mediaControlManager.getMediaSession()));
            }

            @Override
            public void notLoggedIn() {
                Log.d(TAG, "notLoggedIn: Opening LoginActivity");
                startActivity(new Intent(MainActivity.this, LoginActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            }

            @Override
            public void failedGettingReady(@NotNull Exception ex) {
                Log.e(TAG, "failedGettingReady: Something went wrong", ex);
                Toast.makeText(MainActivity.this, R.string.somethingWentWrong, Toast.LENGTH_SHORT).show();
                toggleMenuVisibility(binding, false);
            }
        }));
    }

    /**
     * Used to conveniently show or hide the BottomNavigation, Playback Controls and Playback Info
     *
     * @param binding The MainActivityBinding used to inflate the View
     * @param visible whether the Menu should be shown or hidden
     */
    private void toggleMenuVisibility(ActivityMainBinding binding, boolean visible) {
        binding.playControls.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.search.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaControlManager.releaseMediaSession();
        LibrespotHolder.clear();
    }
}
