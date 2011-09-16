package org.kwimbo.lastfm;

import android.app.WallpaperManager;
import android.content.*;
import android.graphics.*;
import android.os.AsyncTask;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import de.umass.lastfm.Period;

import java.util.List;

public class CollageWallpaper extends WallpaperService implements TaskManager {

    public static final String SHARED_PREFS_NAME = "collage_wallpaper_settings";
    public static final String COLLAGE_PATH_NAME = "collage_path";
    private static final int defaultUpdateInterval = 1000 * 60 * 60 * 24 * 7; // A week in milliseconds
    private static final int defaultRetryInterval = 1000 * 60 * 60 * 2; // 2 hours

    private TestPatternEngine engine;
    private Bitmap bitmap;
    private SharedPreferences sharedPreferences;
    private ImageUrlRetriever imageUrlRetriever = new ImageUrlRetriever();
    private CollageMaker collageMaker;
    private RetrieveImagesTask retrieveImagesTask;
    private MakeCollageTask makeCollageTask;
    private UpdateScheduler updateScheduler;
    private boolean inProgress;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            restartCollage();
        }
    };

    private Runnable restartTask = new Runnable() {
        public void run() {
            restartCollage();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_RUN);
        registerReceiver(receiver, filter);

        updateScheduler = new UpdateScheduler(restartTask, sharedPreferences);
        updateScheduler.scheduleFromPreferences();

        if (bitmap == null) {
            String collagePath = sharedPreferences.getString(COLLAGE_PATH_NAME, "");
            if (collagePath.length() == 0) {
                // Regenerate
            } else {
                loadBitmap(collagePath);
            }
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        engine = new TestPatternEngine();
        return engine;
    }

    @SuppressWarnings("deprecation")
    private Rect getWallpaperSize() {
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        final int width = wallpaperManager.getDesiredMinimumWidth();
        final int height = wallpaperManager.getDesiredMinimumHeight();
        if (width > 0 && height > 0) {
            return new Rect(0, 0, width, height);
        }
        else {
            final WindowManager windowManager = ((WindowManager) getSystemService(Context.WINDOW_SERVICE));
            final Display display = windowManager.getDefaultDisplay();
            final DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            return new Rect(0, 0, metrics.widthPixels, metrics.heightPixels);
        }
    }

    private ImageUrlRetriever.Query getPreferences() throws Exception {
        String lastfmUsername = sharedPreferences.getString("lastfm_username", "");
        String imageKindStr = sharedPreferences.getString("image_kind", "");
        String timePeriodStr = sharedPreferences.getString("time_period", "");
        String downloadLimitStr = sharedPreferences.getString("download_limit", "");

        if (lastfmUsername.length() == 0) {
            throw new Exception("Last.fm username must be configured");
        }

        ImageUrlRetriever.Query query = new ImageUrlRetriever.Query();
        try {
            query.lastfmUsername = lastfmUsername;
            query.imageKind = ImageUrlRetriever.ImageKind.valueOf(imageKindStr);
            query.timePeriod = Period.valueOf(timePeriodStr);
            query.downloadLimit = Integer.parseInt(downloadLimitStr);
        } catch (IllegalArgumentException e) {
            throw new Exception("Configuration error, please reset");
        }

        return query;
    }

    void restartCollage() {
        retrieveImages();
    }

    public void retrieveImages() {
        if (inProgress) {
            return;
        }
        inProgress = true;

        retrieveImagesTask = new RetrieveImagesTask(this, imageUrlRetriever);

        try {
            retrieveImagesTask.execute(getPreferences());
        } catch (Exception e) {
            retrieveImagesTask = null;
            e.printStackTrace();
            errorOccurred(e);
        }
    }

    public void createCollage(List<Bitmap> tiles) {
        Rect wallpaperSize = getWallpaperSize();
        Rect tileSize = new Rect(0, 0, 240, 240);
        String cacheDir = getCacheDir().getAbsolutePath();
        collageMaker = new CollageMaker(wallpaperSize, tileSize);
        makeCollageTask = new MakeCollageTask(this, collageMaker, cacheDir);

        try {
            makeCollageTask.execute(tiles);
        } catch (Exception e) {
            makeCollageTask = null;
            e.printStackTrace();
        }
    }

    public void collageComplete(String path) {
        loadBitmap(path);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(COLLAGE_PATH_NAME, path);
        editor.commit();

        retrieveImagesTask = null;
        collageMaker = null;
        makeCollageTask = null;

        inProgress = false;

        reschedule();
    }

    public void errorOccurred(Exception exception) {
        inProgress = false;

        if (D.DBG) Log.w(D.TAG, exception.getMessage());
        Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();

        updateScheduler.rescheduleIn(defaultRetryInterval);
    }

    private boolean shouldStartTask(AsyncTask<?, ?, ?> task) {
        return (task == null || task.getStatus() == AsyncTask.Status.FINISHED);
    }

    private void loadBitmap(String path) {
        bitmap = BitmapFactory.decodeFile(path);
        if (engine == null) {
            return;
        }

        // Sometimes the first draw is not seen (screen is black), until the
        // screen is redrawn (e.g screen turned off and on, scrolling notification bar).
        // The only way I found to solve this is to draw twice.
        engine.drawFrame();
        engine.drawFrame();
    }

    private void reschedule() {
        updateScheduler.rescheduleIn(defaultUpdateInterval);
    }

    class TestPatternEngine extends Engine {
        private int offsetXPixels = 0;
        private int offsetYPixels = 0;

        TestPatternEngine() {
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (visible) {
                drawFrame();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
        }


        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xStep,
                                     float yStep, int xPixels, int yPixels) {
            offsetXPixels = xPixels;
            offsetYPixels = yPixels;
            drawFrame();
        }

        /*
           * Draw one frame of the animation. This method gets called repeatedly
           * by posting a delayed Runnable. You can do any drawing you want in
           * here. This example draws a wireframe cube.
           */
        void drawFrame() {
            if (bitmap == null) {
                return;
            }

            final SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    long start = System.currentTimeMillis();

                    c.translate(offsetXPixels, offsetYPixels);
                    c.drawBitmap(bitmap, 0, 0, null);

                    if (D.DBG) Log.d(D.TAG, String.format("Drawing bitmap on surface took %d", (System.currentTimeMillis() - start)));
                }

            } finally {
                if (c != null)
                    holder.unlockCanvasAndPost(c);
            }
        }

    }
}