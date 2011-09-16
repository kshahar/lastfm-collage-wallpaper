package org.kwimbo.lastfm;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MakeCollageTask extends AsyncTask<List<Bitmap>, String, String> {
    TaskManager taskManager;
    CollageMaker collageMaker;
    String imagesDir;
    Exception exception;

    public MakeCollageTask(TaskManager taskManager,
                           CollageMaker collageMaker,
                           String imagesDir) {
        this.taskManager = taskManager;
        this.collageMaker = collageMaker;
        this.imagesDir = imagesDir;
    }

    @Override
    protected String doInBackground(List<Bitmap>... tiles) {
        if (tiles.length == 0) {
            exception = new Exception("No tiles received");
            return null;
        }

        List<Bitmap> tilesList = tiles[0];

        publishProgress("Creating collage from image files...");
        Bitmap collage = null;
        try {
            collage = collageMaker.createCollage(tilesList);
        } catch (Exception e) {
            exception = new Exception("Unable to create collage image");
            return null;
        }

        publishProgress("Post-processing...");

        String path = String.format("%s/temp.png", imagesDir);

        File file = new File(path);
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            collage.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();
        } catch (FileNotFoundException e) {
            exception = new Exception("Unable to create a file on cache");
            return null;
        } catch (IOException e) {
            exception = new Exception("Error while writing image to cache");
            return null;
        }

        return path;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        if (D.DBG) Log.i(D.TAG, values[0]);
    }

    @Override
    protected void onPostExecute(String collagePath) {
        if (exception != null) {
            taskManager.errorOccurred(exception);
            return;
        }

        if (collagePath != null) {
            taskManager.collageComplete(collagePath);
        }
    }
}
