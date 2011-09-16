package org.kwimbo.lastfm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class RetrieveImagesTask extends AsyncTask<ImageUrlRetriever.Query, String, List<Bitmap>> {
    TaskManager taskManager;
    ImageUrlRetriever imageUrlRetriever;
    Exception exception;

    public RetrieveImagesTask(TaskManager taskManager, ImageUrlRetriever imageUrlRetriever)
    {
        this.taskManager = taskManager;
        this.imageUrlRetriever = imageUrlRetriever;
    }

    @Override
    protected List<Bitmap> doInBackground(ImageUrlRetriever.Query... query) {
        if (query.length == 0) {
            exception = new Exception("No query received");
            return null;
        }

        publishProgress("Retrieving URLs...");
        List<String> imageUrls = imageUrlRetriever.getUrls(query[0]);
        if (imageUrls.isEmpty()) {
            exception = new Exception("Error receiving user data from Last.fm");
            return null;
        }

        publishProgress("Retrieving image files...");
        List<Bitmap> tiles = new ArrayList<Bitmap>();
        for (String imageUrl : imageUrls) {
            Bitmap albumBitmap = getBitmapFromURL(imageUrl);
            if (albumBitmap != null) {
                tiles.add(albumBitmap);
            }
        }

        if (tiles.isEmpty()) {
            exception = new Exception("Unable to download images from Last.fm");
            return null;
        }

        return tiles;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        if (D.DBG) Log.i(D.TAG, values[0]);
    }

    @Override
    protected void onPostExecute(List<Bitmap> tiles) {
        if (exception != null) {
            if (D.DBG) Log.w(D.TAG, exception.getMessage());
            taskManager.errorOccurred(exception);
            return;
        }
        else if (tiles != null) {
            taskManager.createCollage(tiles);
        }
    }

    private Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);

            URLConnection connection = url.openConnection();
            connection.setUseCaches(true);
            Object response = connection.getContent();

            Bitmap bitmap = null;
            if (response instanceof Bitmap) {
                bitmap = (Bitmap) response;
                return bitmap;
            } else if (response instanceof InputStream) {
                bitmap = BitmapFactory.decodeStream((InputStream) response);
            }
            return bitmap;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
