package dev.nutral.librespot.android.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.jakewharton.disklrucache.DiskLruCache;

import org.apache.commons.codec.binary.Base32;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.nutral.librespot.android.runnables.DownloadImageRunnable;

public class ImageManager {

    private static final String TAG = "ImageManager";
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private static final Object diskCacheLock = new Object();
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 80; // 80MB
    private static final Base32 BASE32_ENCODER = new Base32();

    private static LruCache<String, Bitmap> memoryCache;
    private static DiskLruCache diskCache;
    private static boolean diskCacheStarting = true;
    private static MessageDigest SHA256;

    public static void init(Context ctx) {
        // Initialize Memory Cache
        final int cacheSizeInKB = (int) (Runtime.getRuntime().maxMemory() / 1024 / 8);
        Log.d(TAG, "init: Memory Cache created with " + cacheSizeInKB + "kb");

        memoryCache = new LruCache<String, Bitmap>(cacheSizeInKB) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };

        try {
            SHA256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "init: Could not get SHA-256 implementation", e);
        }

        // Initialize Disk Cache
        final File cacheDir = getDiskCacheDir(ctx, "thumbnails");
        executorService.execute(() -> {
            synchronized (diskCacheLock) {
                try {
                    diskCache = DiskLruCache.open(cacheDir, Build.VERSION.SDK_INT, 1, DISK_CACHE_SIZE);
                    diskCacheStarting = false; // Finished initialization
                    diskCacheLock.notifyAll(); // Wake any waiting threads
                } catch (IOException e) {
                    Log.e(TAG, "init: Error occurred while initializing disk Cache", e);
                }
            }
        });
    }

    public static void close() {
        try {
            synchronized (diskCacheLock) {
                // Wait while disk cache is started from background thread
                if (diskCacheStarting) {
                    Log.e(TAG, "close: disk cache was never started");
                    return;
                }
                diskCache.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadBitmap(String imageUrl, ImageView imageView) {
        String key = getKeyFromUrl(imageUrl);
        final Bitmap bitmap = getBitmapFromCache(key);

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            executorService.execute(new DownloadImageRunnable(imageUrl, key, imageView::setImageBitmap));
        }
    }

    public static void addBitmapToCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            memoryCache.put(key, bitmap);
        }

        if (getBitmapFromDiskCache(key) == null) {
            // Also add to disk cache
            synchronized (diskCacheLock) {
                try {
                    if (diskCache != null && diskCache.get(key) == null) {
                        DiskLruCache.Editor editor = diskCache.edit(key);
                        OutputStream out = new BufferedOutputStream(editor.newOutputStream(0));

                        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)) {
                            Log.e(TAG, "addBitmapToMemoryCache: Could not compress & save image to disk Cache");
                        }
                        out.close();
                        editor.commit();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "addBitmapToMemoryCache: Error occurred while saving to disk Cache", e);
                }
            }
        }
    }

    public static Bitmap getBitmapFromCache(String key) {
        Bitmap memCache = getBitmapFromMemCache(key);
        if (memCache != null) {
            return memCache;
        }

        return getBitmapFromDiskCache(key);
    }

    private static Bitmap getBitmapFromMemCache(String key) {
        return memoryCache.get(key);
    }

    private static Bitmap getBitmapFromDiskCache(String key) {
        synchronized (diskCacheLock) {
            // Wait while disk cache is started from background thread
            while (diskCacheStarting) {
                try {
                    diskCacheLock.wait();
                } catch (InterruptedException ignored) {
                }
            }
            if (diskCache != null) {
                try {
                    DiskLruCache.Snapshot snapshot = diskCache.get(key);
                    if (snapshot == null)
                        return null;

                    Bitmap bitmap = BitmapFactory.decodeStream(snapshot.getInputStream(0));

                    snapshot.close();

                    return bitmap;
                } catch (IOException e) {
                    Log.e(TAG, "getBitmapFromDiskCache: Could not read Bitmap from disk", e);
                }
            }
        }
        return null;
    }

    /*
     * Creates a unique subdirectory of the designated app cache directory. Tries to use external
     * but if not mounted, falls back on internal storage.
     */
    private static File getDiskCacheDir(Context context, String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final String cachePath =
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                        !Environment.isExternalStorageRemovable() ? context.getExternalCacheDir().getPath() :
                        context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    /**
     * Our Disk Cache can't use url as key because of key regex [a-z0-9_-]{1,64}
     * so we hash the url so we always have a fixed length of chars
     *
     * @param url The spotify cdn url
     * @return the hashed url
     */
    private static String getKeyFromUrl(String url) {
        return BASE32_ENCODER.encodeToString(SHA256.digest(url.getBytes())).replace("=", "").toLowerCase();
    }

}
