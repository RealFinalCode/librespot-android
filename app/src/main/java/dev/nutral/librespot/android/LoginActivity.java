package dev.nutral.librespot.android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.nutral.librespot.android.databinding.ActivityLoginBinding;
import dev.nutral.librespot.android.runnables.LoginRunnable;
import dev.nutral.librespot.android.runnables.callbacks.LoginCallback;
import dev.nutral.librespot.android.utils.Utils;

public final class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        executorService.shutdown();
        super.onCreate(savedInstanceState);
        ActivityLoginBinding binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        LoginCallback callback = new LoginCallback() {
            @Override
            public void loggedIn() {
                Toast.makeText(LoginActivity.this, R.string.loggedIn, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "loggedIn: logged in successfully");
                startActivity(new Intent(LoginActivity.this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            }

            @Override
            public void failedLoggingIn(@NotNull Exception ex) {
                Log.e(TAG, "failedLoggingIn: login failed", ex);
                Toast.makeText(LoginActivity.this, R.string.failedLoggingIn, Toast.LENGTH_SHORT).show();
            }
        };

        binding.login.setOnClickListener(v -> {
            String username = Utils.getText(binding.username);
            String password = Utils.getText(binding.password);
            if (username.isEmpty() || password.isEmpty())
                return;

            File credentialsFile = Utils.getCredentialsFile(this);
            executorService.execute(new LoginRunnable(username, password, credentialsFile, callback));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}