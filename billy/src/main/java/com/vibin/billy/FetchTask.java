package com.vibin.billy;

import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class FetchTask{

    String[] billySong,result;
    JSONArray mJsonArray;
    String artistName, collectionName, artworkUrl,trackName;

    public FetchTask() {
        billySong = new String[24];
    }

    public static class BillyData {
        String song, album, artist, artwork;
        BillyData() {}
        public void setItunes(String album, String artist, String artwork, String song) {
            this.album = album;
            this.artist = artist;
            this.artwork = artwork;
            this.song = song;
        }
    }

    /**
     * Parses XML
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

            if (parser.getLineNumber() < 200 && event == XmlPullParser.START_TAG) {
                if (name.equals("title")) {

                    if (parser.next() == XmlPullParser.TEXT) {
                        if (i != 0) {
                            Log.d(getClass().getName(), "logged "+parser.getText());
                            billySong[i - 1] = cleanSongTitle(parser.getText());
                            // Log.d(getClass().getName(), billySong[i - 1]);
                        }
                        i++;
                    }
                }

            } else if (parser.getLineNumber() >= 200) {
                Log.d(getClass().getName(), "Parsed 200 lines!");
                break;
            }
            event = parser.next();
        }
        return billySong;
    }

    /**
     * Parses JSON
     * @param jsonObject
     * @return A String Array containing metadata of a song
     * @throws JSONException
     */
    public String[] parseItunes(JSONObject jsonObject) throws JSONException {
        mJsonArray = jsonObject.getJSONArray("results");
        artistName = mJsonArray.getJSONObject(0).getString("artistName");
        collectionName = mJsonArray.getJSONObject(0).getString("collectionName");
        artworkUrl = mJsonArray.getJSONObject(0).getString("artworkUrl100").replaceAll("100x100", "400x400");
        trackName = mJsonArray.getJSONObject(0).getString("trackName");

        String newTrack;
        if (trackName.contains("(")) {
            newTrack = capitalize(trackName.substring(0, trackName.indexOf("("))).trim();
        } else {
            newTrack = capitalize(trackName).trim();
        }
        String match = Integer.toString(Arrays.asList(billySong).indexOf(newTrack));
        result = new String[]{artistName,collectionName,artworkUrl,newTrack,match};
        return result;
    }

    /*
        String Utility Methods
     */
    private String cleanSongTitle(String text) {
        return text.substring(text.indexOf(":") + 2, text.indexOf(","));
    }
    private String capitalize(String text) {
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        if (words[0].length() > 0) {
            sb.append(Character.toUpperCase(words[0].charAt(0)) + words[0].subSequence(1, words[0].length()).toString().toLowerCase());
            for (int i = 1; i < words.length; i++) {
                sb.append(" ");
                sb.append(Character.toUpperCase(words[i].charAt(0)) + words[i].subSequence(1, words[i].length()).toString().toLowerCase());
            }
        }
        return sb.toString();
    }
}
