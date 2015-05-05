package com.vibin.billy;

import android.os.Parcel;
import android.os.Parcelable;

public class BillyItem implements Parcelable {
    private static final String TAG = BillyItem.class.getSimpleName();
    String song, album, artist, artwork, streamLink, simpleSong, simpleArtist;
    int rank = 0;
    long duration;

    public BillyItem() {
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
        dest.writeString(song);
        dest.writeString(album);
        dest.writeString(artist);
        dest.writeString(artwork);
        dest.writeString(streamLink);
        dest.writeString(simpleSong);
        dest.writeString(simpleArtist);
        dest.writeInt(rank);
        dest.writeLong(duration);
    }

    private void readFromParcel(Parcel in) {
        song = in.readString();
        album = in.readString();
        artist = in.readString();
        artwork = in.readString();
        streamLink = in.readString();
        simpleSong = in.readString();
        simpleArtist = in.readString();
        rank = in.readInt();
        duration = in.readLong();
    }

    public static final Parcelable.Creator<BillyItem> CREATOR = new Parcelable.Creator<BillyItem>() {
        public BillyItem createFromParcel(Parcel in) {
            return new BillyItem(in);
        }

        public BillyItem[] newArray(int size) {
            return new BillyItem[size];
        }

    };

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

    public void setItunes(String song, String album, String artist, String artwork, int rank) {
        setSong(song);
        setAlbum(album);
        setArtist(artist);
        setArtwork(artwork);
        setRank(rank);
    }

    /**
     * Get the first artist, when there are more than one
     * Example: Bang Bang - Jessie J, Ariana Grande, Nicki Minaj. We return "Jessie J".
     */
    public String getSingleArtist() {
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

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getSimpleSong() {
        return simpleSong;
    }

    public void setSimpleSong(String simpleSong) {
        this.simpleSong = simpleSong;
    }

    public String getSimpleArtist() {
        return simpleArtist;
    }

    public void setSimpleArtist(String simpleArtist) {
        this.simpleArtist = simpleArtist;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }
}
