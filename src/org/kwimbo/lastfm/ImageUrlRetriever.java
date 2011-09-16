package org.kwimbo.lastfm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.umass.lastfm.*;

public class ImageUrlRetriever {
    enum ImageKind {
        ARTISTS,
        ALBUMS
    }

    public static class Query {
        public String lastfmUsername = null;
        public ImageKind imageKind = null;
        public Period timePeriod = null;
        public int downloadLimit = 0;
    }

    private static String apiKey = "<REDACTED>";
    private ImageSize imageSize = ImageSize.LARGE;

    public ImageUrlRetriever() {
        Caller.getInstance().setCache(null);
    }

    public static String getApiKey() {
        return apiKey;
    }

    public static void setApiKey(String apiKey) {
        ImageUrlRetriever.apiKey = apiKey;
    }

    public ImageSize getImageSize() {
        return imageSize;
    }

    public void setImageSize(ImageSize imageSize) {
        this.imageSize = imageSize;
    }

    public List<String> getUrls(Query query) {
        if (query.imageKind == ImageKind.ARTISTS) {
            return getUrlsFromTopArtists(query.lastfmUsername, query.timePeriod, query.downloadLimit);
        } else if (query.imageKind == ImageKind.ALBUMS) {
            return getUrlsFromTopAlbums(query.lastfmUsername, query.timePeriod, query.downloadLimit);
        }

        return null;
    }

    public List<String> getUrlsFromTopArtists(String lastfmUsername, Period timePeriod, int limit) {
        try {
            Collection<Artist> artists = User.getTopArtists(lastfmUsername, timePeriod, limit, getApiKey());
            return getUrlsFromImageHolders(artists, getImageSize());
        } catch (Exception e) {
            return new ArrayList<String>();
        }
    }

    public List<String> getUrlsFromTopAlbums(String lastfmUsername, Period timePeriod, int limit) {
        try {
            Collection<Album> albums = User.getTopAlbums(lastfmUsername, timePeriod, limit, getApiKey());
            return getUrlsFromImageHolders(albums, getImageSize());
        } catch (Exception e) {
            return new ArrayList<String>();
        }
    }

    private List<String> getUrlsFromImageHolders(Collection imageHolders, ImageSize imageSize) {
        List<String> result = new ArrayList<String>();

        Iterator<ImageHolder> itr = imageHolders.iterator();
        while (itr.hasNext()) {
            ImageHolder imageHolder = itr.next();
            String url = imageHolder.getImageURL(imageSize);

            if (url.contains("default_album")) {
                continue;
            }

            result.add(url);
        }

        return result;
    }
}
