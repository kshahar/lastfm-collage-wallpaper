package org.kwimbo.lastfm;

import android.graphics.*;
import android.util.Log;

import java.util.List;
import java.util.Random;

public class CollageMaker {
    private static final int blurRadius = 100;
    private static final float darknessFactor = 0.3f;
    private static final Random random = new Random();

    private Canvas canvas = new Canvas();
    private Paint paint = new Paint();
    private Rect wallpaperSize;
    private Rect tileSize;

    public CollageMaker(Rect wallpaperSize, Rect tileSize) {
        this.wallpaperSize = wallpaperSize;
        this.tileSize = tileSize;

        paint.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL));
    }

    public Bitmap createCollage(List<Bitmap> tiles) {
        Bitmap collage = Bitmap.createBitmap(wallpaperSize.width(),
                wallpaperSize.height(), Bitmap.Config.ARGB_8888);
        createCollage(collage, tiles, tileSize.width(), tileSize.height(), 1);

        Bitmap darkCollage = changeBitmapBrightness(collage, false, darknessFactor);
        return darkCollage;
    }

    private void createCollage(Bitmap targetImage, List<Bitmap> images, int tileWidth, int tileHeight, int passes ) {
        canvas.setBitmap(targetImage);

        final int width = targetImage.getWidth();
        final int height = targetImage.getHeight();

        long start = System.currentTimeMillis();

        final int rowTiles = 1+((width-(tileWidth/2))/tileWidth);
        final int colTiles = 1+((height-(tileHeight/2))/tileHeight);

        // Very simple heuristic to compute the required number of images to draw
        final int imagesToDraw = Math.max(rowTiles*colTiles*2, images.size());

        RingIterator<Bitmap> itr = new RingIterator<Bitmap>(images, imagesToDraw);

        // Cover all screen with tiles
        for (int i=0; i<rowTiles; i++) {
            for (int j=0; j<colTiles; j++) {
                if (!itr.hasNext()) {
                    break;
                }
                Bitmap image = itr.next();
                drawTile(image, tileWidth, tileHeight, i*tileWidth, j*tileHeight);
            }
        }

        // Draw tiles at random positions
        while (itr.hasNext()) {
            Bitmap image = itr.next();
            int x = random.nextInt(width - tileWidth);
            int y = random.nextInt(height - tileHeight);
            drawTile(image, tileWidth, tileHeight, x, y);
        }

        if (D.DBG) Log.i(D.TAG, String.format("Total time %d", System.currentTimeMillis() - start));
    }

    private void drawTile(Bitmap image, int tileWidth, int tileHeight, int x, int y) {
        final long start = System.currentTimeMillis();
        canvas.drawBitmap(image,
                new Rect(0, 0, image.getWidth(), image.getHeight()),
                new RectF(x, y, x+tileWidth, y+tileHeight),
                paint);

        final long end = System.currentTimeMillis();
        if (D.DBG) Log.i(D.TAG, String.format("Drawing tile at (%d, %d) took %d", x, y, end-start));
    }

    public static Bitmap changeBitmapBrightness(Bitmap bitmap, boolean brighter, float level) {
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        int levelValue = (int)(level*255);

        if (brighter) {
            paint.setColorFilter(new PorterDuffColorFilter(
                    Color.argb(levelValue, 255, 255, 255), PorterDuff.Mode.SRC_OVER));
        }
        else {
            paint.setColorFilter(new PorterDuffColorFilter(
                    Color.argb(levelValue, 0, 0, 0), PorterDuff.Mode.SRC_ATOP));
        }

        canvas.drawBitmap(bitmap, 0, 0, paint);
        return result;
    }
}
