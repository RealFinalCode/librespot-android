package dev.nutral.librespot.android.runnables.callbacks;

import androidx.annotation.UiThread;

import org.jetbrains.annotations.NotNull;

@UiThread
public interface SetupCallback {
    void playerReady(@NotNull String username);

    void notLoggedIn();

    void failedGettingReady(@NotNull Exception ex);
}