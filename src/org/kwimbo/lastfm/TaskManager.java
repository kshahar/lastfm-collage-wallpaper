package org.kwimbo.lastfm;

import java.util.List;

import android.graphics.Bitmap;

public interface TaskManager {
    void retrieveImages();

    void createCollage(List<Bitmap> tiles);

    void collageComplete(String path);

    void errorOccurred(Exception exception);
}
