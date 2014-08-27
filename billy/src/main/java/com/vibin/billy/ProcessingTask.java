package com.vibin.billy;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class ProcessingTask {

    private static final String TAG = ProcessingTask.class.getSimpleName();

    String[] billySong, billyArtist, result;
    JSONArray mJsonArray;
    String artistName, collectionName, artworkUrl, trackName, extractedSong, extractedArtist, paramEncode, streamLink;
    int billySize;
    boolean ignore;

    public ProcessingTask() {
    }

    public ProcessingTask(int billySize) {
        this.billySize = billySize;
        billySong = new String[billySize];
        billyArtist = new String[billySize];
    }

    public static class BillyData implements Parcelable {
        String song, album, artist, artwork;

        BillyData() {
        }

        public BillyData(Parcel in) {
            super();
            readFromParcel(in);
        }

        public void setItunes(String album, String artist, String artwork, String song) {
            this.album = album;
            this.artist = artist;
            this.artwork = artwork;
            this.song = song;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(song);
            parcel.writeString(album);
            parcel.writeString(artist);
            parcel.writeString(artwork);
        }

        public void readFromParcel(Parcel in) {
            song = in.readString();
            album = in.readString();
            artist = in.readString();
            artwork = in.readString();
        }

        public static final Parcelable.Creator<BillyData> CREATOR = new Parcelable.Creator<BillyData>() {
            public BillyData createFromParcel(Parcel in) {
                return new BillyData(in);
            }

            public BillyData[] newArray(int size) {
                return new BillyData[size];
            }

        };

    }

    /**
     * Parses XML from Billboard
     *
     * @param response A String containing XML
     * @return A String Array containing song titles
     * @throws IOException
     * @throws XmlPullParserException
     */
    public String[] parseBillboard(String response) throws IOException, XmlPullParserException {
        InputStream in;
        in = IOUtils.toInputStream(response, "UTF-8");

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(in, null);

        int event = parser.getEventType();
        int i = 0;
        while (event != XmlPullParser.END_DOCUMENT) {
            String name = parser.getName();

            if (i <= billySize && event == XmlPullParser.START_TAG) {
                if (name.equals("title")) {
                    if (parser.next() == XmlPullParser.TEXT) {
                        if (i != 0) {
                            billySong[i - 1] = extractSong(parser.getText()).trim();
                            billyArtist[i - 1] = extractArtist(parser.getText()).trim();
                            Log.d(TAG, billySong[i - 1] + " " + billyArtist[i - 1]);
                        }
                        i++;
                    }
                }

            } else if(i>billySize){
                break;
            }
            event = parser.next();
        }
        return billySong;
    }

    public String[] getArtists() {
        return billyArtist;
    }

    /**
     * Parses JSON from iTunes
     *
     * @param jsonObject JSON response
     * @return A String Array containing metadata of a song
     * @throws JSONException
     */
    public String[] parseItunes(JSONObject jsonObject) throws JSONException {
        int counter = 0;
        while (counter <= 1) {
            mJsonArray = jsonObject.getJSONArray("results");
            if (jsonObject.getInt("resultCount") == 0) {
                Log.e(TAG, "resultCount is zero");
                Log.e(TAG, "Artist name is " + mJsonArray.getJSONObject(counter).getString("artistName"));
            }
            artistName = mJsonArray.getJSONObject(counter).getString("artistName");
            collectionName = mJsonArray.getJSONObject(counter).getString("collectionName");
            artworkUrl = mJsonArray.getJSONObject(counter).getString("artworkUrl100").replaceAll("100x100", "400x400");
            trackName = mJsonArray.getJSONObject(counter).getString("trackName");

            // Capitalize first letter of every word
            if (!StringUtils.isAllUpperCase(trackName)) {
                trackName = WordUtils.capitalize(trackName);
            }

            if (trackName.contains("(")) {
                trackName = trackName.substring(0, trackName.indexOf("("));
            } else if (trackName.toLowerCase().contains("feat.")) {
                Log.d(TAG, trackName + " contains Featuring");
                trackName = trackName.substring(0, StringUtils.indexOfIgnoreCase(trackName, "feat."));
            } else if (trackName.contains("!")) {
                trackName = trackName.substring(0, trackName.indexOf("!"));
            }
            trackName = trackName.trim();
            String match = Integer.toString(Arrays.asList(billySong).indexOf(trackName));

            // Trackname from Billboard and iTunes don't match
            if (Integer.parseInt(match) == -1) {
                Log.e(TAG, "The unmatched billysong is " + trackName);
                String matchArtist = Integer.toString(Arrays.asList(billyArtist).indexOf(artistName));
                if (Integer.parseInt(matchArtist) == -1) {
                    counter++;
                    Log.e(TAG, "Artists haven't matched " + artistName + " and counter is " + counter);
                } else {
                    Log.e(TAG, "Something wrong with text manipulation " + trackName + artistName);
                    return null;
                }
            } else {
                // Most ideal situation
                result = new String[]{collectionName, artistName, artworkUrl, trackName, match};
                return result;
            }
        }
        return null;
    }

    /*
        String Utility Methods
    */
    private String extractSong(String text) {
        extractedSong = text.substring(text.indexOf(":") + 2, text.indexOf(","));
        if (!extractedSong.contains("(") && !extractedSong.contains("!") && !extractedSong.contains("#") && !extractedSong.contains("&")) {
            return extractedSong;
        } else if (extractedSong.contains("!")) {
            //Log.d(TAG, "Exclamation mark detected "+extractedSong);
            return extractedSong.replace("!", "");
        } else if (extractedSong.contains("(")) {
            //Log.d(TAG, "Brackets detected "+extractedSong);
            return extractedSong.substring(0, extractedSong.indexOf("("));
        }
/*        else if(extractedSong.contains("#")){
            Log.d(TAG, "Hash sign detected "+extractedSong +  extractedSong.substring(1));
            return extractedSong.substring(1);
        }*/
/*        else if(extractedSong.contains("&"))
        {
            Log.d(TAG, "Ampersand detected "+extractedSong);
            return extractedSong.replace("&","and");
        }*/
        else {
            //Log.e(TAG, "The song "+text+" couldn't be extracted properly");
            return extractedSong;
        }
    }

    private String extractArtist(String text) {
        extractedArtist = text.substring(text.indexOf(",") + 2);
        if (!extractedArtist.contains("Featuring") && !extractedArtist.contains("Ft")) {
            return extractedArtist;
        } else if (extractedArtist.contains("Featuring")) {
            //Log.d(TAG, "Contains featuring "+ extractedArtist);
            extractedArtist = extractedArtist.substring(0, extractedArtist.indexOf("Featuring"));
            return extractedArtist;
        } else if (extractedArtist.contains("Ft")) {
            //Log.d(TAG, "Contains Ft "+ extractedArtist);
            extractedArtist = extractedArtist.substring(0, extractedArtist.indexOf("Ft"));
            return extractedArtist;
        } else {
            //Log.e(TAG, "The song "+text+" couldn't be extracted properly");
            return extractedArtist;
        }
    }

    /**
     * @param text the Song itself
     * @return encoded Song string
     */
    public String paramEncode(String text) {
        paramEncode = text;
        if (text.contains("&")) {
            paramEncode = text.replaceAll("&", "and");
        } else if (text.contains("#")) {
            paramEncode = text.replace("#", "");
        }
        paramEncode = paramEncode.replaceAll(" ", "+").trim();
        return paramEncode;
    }

    /**
     * @param response the JSON response
     * @return the SoundCloud stream link
     * @throws IOException
     * @throws XmlPullParserException
     */
    public String parseSoundcloud(String response) throws IOException, XmlPullParserException {
        InputStream in;
        String firstLink="";
        boolean firstTrack = false;
        in = IOUtils.toInputStream(response, "UTF-8");

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(in, null);

        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            String name = parser.getName();

            if (event == XmlPullParser.START_TAG) {
                if(name.equals("title")){
                    if (parser.next() == XmlPullParser.TEXT) {
                        if(parser.getText().toLowerCase().contains("remix") || parser.getText().toLowerCase().contains("cover") || parser.getText().toLowerCase().contains("download")){
                            Log.d(TAG,"track ignored: "+parser.getText());
                            ignore = true;
                        }
                    }
                }
                if (name.equals("stream-url")) {
                    if(firstLink.isEmpty() && parser.next() == XmlPullParser.TEXT)
                    {
                        firstLink = parser.getText()+"?client_id=d0f2d22083bc8233aab32f3f7d1d0bbc";
                        firstTrack = true;
                    }
                    if(firstTrack)
                    {
                        firstTrack = false;
                        if(!ignore){
                            streamLink = firstLink;
                            return streamLink;
                        }
                    }
                    else if (parser.next() == XmlPullParser.TEXT && !ignore) {
                        Log.d(TAG,"ignore is "+ignore);
                        streamLink = parser.getText()+"?client_id=d0f2d22083bc8233aab32f3f7d1d0bbc";
                        return streamLink;
                    }
                    if(ignore){
                        ignore = false;
                    }
                }
            }
            event = parser.next();
        }
        return firstLink;
    }
}
