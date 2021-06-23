package dev.nutral.librespot.android.runnables.callbacks;

import androidx.annotation.UiThread;

import org.jetbrains.annotations.NotNull;

@UiThread
public interface LoginCallback {
    void loggedIn();

    void failedLoggingIn(@NotNull Exception ex);
}