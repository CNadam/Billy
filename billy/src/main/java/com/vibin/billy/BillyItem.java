package com.vibin.billy;

import android.os.Parcel;
import android.os.Parcelable;

public class BillyItem implements Parcelable {
    String song, album, artist, artwork, streamLink, permaLink, lastFmBio, ytThumbnail, ytId;
    int index;

    public String getSong() {
        return song;
    }

    public void setSong(String song) {
        this.song = song;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    /**
     * Get the first artist, when there are more than one
     * Example: Bang Bang - Jessie J, Ariana Grande, Nicki Minaj. We return "Jessie J".
     */
    public String getOnlyArtist() {
        if (artist.contains(",")) {
            return artist.substring(0, artist.indexOf(","));
        }
        return getArtist();
    }

    public String getArtwork() {
        return artwork;
    }

    public void setArtwork(String artwork) {
        this.artwork = artwork;
    }

    public String getStreamLink() {
        return streamLink;
    }

    public void setStreamLink(String streamLink) {
        this.streamLink = streamLink;
    }

    public String getPermaLink() {
        return permaLink;
    }

    public void setPermaLink(String permaLink) {
        this.permaLink = permaLink;
    }

    public String getLastFmBio() {
        return lastFmBio;
    }

    public void setLastFmBio(String lastFmBio) {
        this.lastFmBio = lastFmBio;
    }

    public String getYtThumbnail() {
        return ytThumbnail;
    }

    public void setYtThumbnail(String ytThumbnail) {
        this.ytThumbnail = ytThumbnail;
    }

    public String getYtId() {
        return ytId;
    }

    public void setYtId(String ytId) {
        this.ytId = ytId;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public BillyItem(Parcel in) {
        super();
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

    private void readFromParcel(Parcel in) {

    }

    public static final Parcelable.Creator<BillyItem> CREATOR = new Parcelable.Creator<BillyItem>() {
        public BillyItem createFromParcel(Parcel in) {
            return new BillyItem(in);
        }

        public BillyItem[] newArray(int size) {
            return new BillyItem[size];
        }

    };
}
