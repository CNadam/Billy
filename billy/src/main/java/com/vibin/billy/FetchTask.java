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

public class FetchTask {

    String[] billySong, billyArtist, result;
    JSONArray mJsonArray;
    String artistName, collectionName, artworkUrl, trackName, extractedSong, extractedArtist, newTrack, paramEncode;
    int billySize;

    public FetchTask(int billySize) {
        this.billySize = billySize;
        billySong = new String[billySize];
        billyArtist = new String[billySize];
    }

    public static class BillyData implements Parcelable{
        String song, album, artist, artwork;

        BillyData() {
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

        private void readFromParcel(Parcel in) {
            song = in.readString();
            album = in.readString();
            artist = in.readString();
            artwork = in.readString();
        }
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
                            Log.d(getClass().getName(), billySong[i - 1] + " " + billyArtist[i - 1]);
                            // Log.d(getClass().getName(), billySong[i - 1]);
                        }
                        i++;
                    }
                }

            } else if (i > billySize) {
                Log.d(getClass().getName(), "Parsed 20 tracks!");
                break;
            }
            event = parser.next();
        }
        return billySong;
    }

    /**
     * Parses JSON from iTunes
     *
     * @param jsonObject
     * @return A String Array containing metadata of a song
     * @throws JSONException
     */
    public String[] parseItunes(JSONObject jsonObject) throws JSONException {
        int counter = 0;
        while (counter <= 1) {
            mJsonArray = jsonObject.getJSONArray("results");
            if (jsonObject.getInt("resultCount") == 0) {
                Log.e(getClass().getName(), "resultCount is zero");
                Log.e(getClass().getName(), "Artist name is "+mJsonArray.getJSONObject(counter).getString("artistName"));
            }
            artistName = mJsonArray.getJSONObject(counter).getString("artistName");
            collectionName = mJsonArray.getJSONObject(counter).getString("collectionName");
            artworkUrl = mJsonArray.getJSONObject(counter).getString("artworkUrl100").replaceAll("100x100", "400x400");
            trackName = mJsonArray.getJSONObject(counter).getString("trackName");

            if (!StringUtils.isAllUpperCase(trackName)) {
                newTrack = WordUtils.capitalize(trackName);
            } // Capitalize first letter of every word
            if (trackName.contains("(")) {
                newTrack = newTrack.substring(0, trackName.indexOf("("));
            } else if (trackName.contains("feat")) {
                Log.d(getClass().getName(), newTrack + " contains Featuring");
                newTrack = newTrack.substring(0, trackName.indexOf("feat"));
                //Log.d(getClass().getName(), newTrack);
            }
            newTrack = newTrack.trim();
            String match = Integer.toString(Arrays.asList(billySong).indexOf(newTrack));

            // Trackname from Billboard and iTunes don't match
            if (Integer.parseInt(match) == -1) {
                Log.e(getClass().getName(), "The unmatched billysong is " + trackName + newTrack);
                String matchArtist = Integer.toString(Arrays.asList(billyArtist).indexOf(artistName));
                if (Integer.parseInt(matchArtist) == -1) {
                    counter++;
                    Log.e(getClass().getName(), "Artists haven't matched " + artistName + " and counter is " + counter);
                } else {
                    Log.e(getClass().getName(), "Something wrong with text manipulation " + trackName + artistName);
                    return null;
                }
            } else {
                // Most ideal situation
                result = new String[]{collectionName, artistName, artworkUrl, newTrack, match};
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
            //Log.d(getClass().getName(), "Exclamation mark detected "+extractedSong);
            return extractedSong.replace("!", "");
        } else if (extractedSong.contains("(")) {
            //Log.d(getClass().getName(), "Brackets detected "+extractedSong);
            return extractedSong.substring(0, extractedSong.indexOf("("));
        }
/*        else if(extractedSong.contains("#")){
            Log.d(getClass().getName(), "Hash sign detected "+extractedSong +  extractedSong.substring(1));
            return extractedSong.substring(1);
        }*/
/*        else if(extractedSong.contains("&"))
        {
            Log.d(getClass().getName(), "Ampersand detected "+extractedSong);
            return extractedSong.replace("&","and");
        }*/
        else {
            //Log.e(getClass().getName(), "The song "+text+" couldn't be extracted properly");
            return extractedSong;
        }
    }

    private String extractArtist(String text) {
        extractedArtist = text.substring(text.indexOf(",") + 2);
        if (!extractedArtist.contains("Featuring") && !extractedArtist.contains("Ft")) {
            return extractedArtist;
        } else if (extractedArtist.contains("Featuring")) {
            //Log.d(getClass().getName(), "Contains featuring "+ extractedArtist);
            extractedArtist = extractedArtist.substring(0, extractedArtist.indexOf("Featuring"));
            return extractedArtist;
        } else if (extractedArtist.contains("Ft")) {
            //Log.d(getClass().getName(), "Contains Ft "+ extractedArtist);
            extractedArtist = extractedArtist.substring(0, extractedArtist.indexOf("Ft"));
            return extractedArtist;
        } else {
            //Log.e(getClass().getName(), "The song "+text+" couldn't be extracted properly");
            return extractedArtist;
        }
    }

    public String paramEncode(String[] stuff, int i) {
        if (stuff[i].contains("&")) {
            paramEncode = stuff[i].replaceAll("&", "and");
            paramEncode = paramEncode.replaceAll(" ", "+");
        } else if (stuff[i].contains("#")) {
            paramEncode = stuff[i].replace("#", "");
            paramEncode = paramEncode.replaceAll(" ", "+").trim();
        } else {
            paramEncode = stuff[i].replaceAll(" ", "+");
        }
        return paramEncode;
    }
}
