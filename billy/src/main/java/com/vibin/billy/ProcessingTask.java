package com.vibin.billy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

/**
 * Do all processing tasks here. Like parsing XML, JSON, modifying strings and more.
 */
public class ProcessingTask {

    private static final String TAG = ProcessingTask.class.getSimpleName();

    String[] billySong, billyArtist;
    int billySize;
    Context context;
    int quality;

    public ProcessingTask(Context c) {
        this.context = c;
    }

    /**
     * Used for populating arrays of song and artist names
     *
     * @param billySize Number of songs
     */
    public ProcessingTask(int billySize, Context c) {
        this.billySize = billySize;
        billySong = new String[billySize];
        billyArtist = new String[billySize];
        this.context = c;
        getArtworkUrlResolution();
    }

    public ProcessingTask(String[] billySong, String[] billyArtist, Context c) {
        this.billySong = billySong;
        this.billyArtist = billyArtist;
        this.context = c;
        getArtworkUrlResolution();
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
        in = getStringAsInputStream(response);

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(in, null);

        int event = parser.getEventType();
        int i = 0;
        while (event != XmlPullParser.END_DOCUMENT) {
            String name = parser.getName();

            if (i <= billySize && event == XmlPullParser.START_TAG) {
                if (name.equals("description")) {
                    if (parser.next() == XmlPullParser.TEXT) {
                        billySong[i] = extractSong(parser.getText());
                        billyArtist[i] = extractArtist(parser.getText());
                        if (response.length() > 10000) {
                            //Log.d(TAG, billySong[i] + " " + billyArtist[i]);
                        }
                        i++;
                    }
                }

            } else if (i >= billySize) {
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
        String trackName, artworkUrl, collectionName, artistName;
        JSONArray mJsonArray;
        while (counter < 2) {
            mJsonArray = jsonObject.getJSONArray("results");
            if (jsonObject.getInt("resultCount") == 0) {
                Log.e(TAG, "resultCount is zero " + jsonObject.toString());
                Log.e(TAG, "Artist name is " + mJsonArray.getJSONObject(counter).getString("artistName"));
            }
            artistName = mJsonArray.getJSONObject(counter).getString("artistName");
            collectionName = mJsonArray.getJSONObject(counter).getString("collectionName");
            artworkUrl = mJsonArray.getJSONObject(counter).getString("artworkUrl100");
            trackName = mJsonArray.getJSONObject(counter).getString("trackName");

            // Change quality of artwork according to user settings
            if (quality == 2) {
                artworkUrl = artworkUrl.replaceAll("100x100", "600x600");
            } else {
                artworkUrl = artworkUrl.replaceAll("100x100", "400x400");
            }

            Log.d(TAG, "artwork url is " + artworkUrl);

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

            // Replace Smart Quotes with Dumb Quotes
            trackName = replaceSmartQuotes(trackName).trim();

            int match = matchMagic(billySong, trackName);

            // Track name from Billboard and iTunes don't match, also LOL
            if (match == -1) {
                Log.e(TAG, "The unmatched billysong is " + trackName);
                int matchArtist = matchMagic(billyArtist, artistName);
                counter++;
                if (matchArtist == -1) {
                    Log.e(TAG, "Artists haven't matched " + artistName + " and counter is " + counter);
                } else {
                    Log.e(TAG, "Something wrong with text manipulation " + trackName + artistName);
                    //       return null;
                }
            } else {
                // Most ideal situation
                return new String[]{collectionName, artistName, artworkUrl, trackName, match + ""};
            }
        }
        return null;
    }

    /**
     * Parse song substring from Billboard <description> tag
     */
    private String extractSong(String text) {
        char symbols[] = {'!', '(', '#', '&', '+'};
//        String extractedSong = text.substring(text.indexOf(":") + 2, text.indexOf(","));
        String extractedSong = text.substring(0, text.indexOf("by"));
        if (StringUtils.containsAny(extractedSong, symbols)) {
            if (extractedSong.contains("!")) {
                extractedSong = extractedSong.replace("!", "");
            } else if (extractedSong.contains("(")) {
                extractedSong = extractedSong.substring(0, extractedSong.indexOf("("));
            } else if (extractedSong.contains("+")) {
                extractedSong = extractedSong.replace("+", "and");
            }
        }
        return extractedSong.trim();
    }

    /**
     * Parse artist substring from Billboard <description> tags
     */
    private String extractArtist(String text) {
//        String extractedArtist = text.substring(text.indexOf(",") + 2);
        String extractedArtist = text.substring(text.indexOf("by") + 3, text.indexOf("ranks"));
        if (extractedArtist.contains("Featuring")) {
            extractedArtist = extractedArtist.substring(0, extractedArtist.indexOf("Featuring"));
        } else if (extractedArtist.contains("Ft")) {
            extractedArtist = extractedArtist.substring(0, extractedArtist.indexOf("Ft"));
        } else if (extractedArtist.contains("Duet")) {
            extractedArtist = extractedArtist.substring(0, extractedArtist.indexOf("Duet"));
        }
        return extractedArtist.trim();
    }

    /**
     * Searches for given String in the String array and returns index. Case-insensitive.
     */

    private int matchMagic(String[] billySong, String trackName) {
        int index = 0;
        for (String name : billySong) {
            if (name == null) {
                Log.d(TAG, "Index is " + index + " trackName is " + trackName);
            }
            if (name.equalsIgnoreCase(trackName)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * Encode the song name to be concatenated to URL
     *
     */
    public String paramEncode(String text) {
        String paramEncode = text;
        if (text.contains("&")) {
            paramEncode = text.replaceAll("&", "and");
        } else if (text.contains("#")) {
            paramEncode = text.replace("#", "");
        }
        paramEncode = paramEncode.replaceAll(" ", "+").trim();
        return paramEncode;
    }

    /**
     * Replaces microsoft "smart quotes" (curly " and ') with their
     * ascii counterparts aka 'dumb quotes'
     * via GData Utils
     */

    String replaceSmartQuotes(String str) {
        str = replaceChars(str, "\u0091\u0092\u2018\u2019", '\'');
        str = replaceChars(str, "\u0093\u0094\u201c\u201d", '"');
        return str;
    }

    String replaceChars(String str, String oldchars, char newchar) {
        int pos = indexOfChars(str, oldchars, 0);
        if (pos == -1) {
            return str;
        }
        StringBuilder buf = new StringBuilder(str);
        do {
            buf.setCharAt(pos, newchar);
            pos = indexOfChars(str, oldchars, pos + 1);
        } while (pos != -1);

        return buf.toString();
    }

    int indexOfChars(String str, String chars, int fromIndex) {
        for (int pos = fromIndex; pos < str.length(); pos++) {
            if (chars.indexOf(str.charAt(pos)) >= 0) {
                return pos;
            }
        }
        return -1;
    }

    public void getArtworkUrlResolution() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        quality = Integer.parseInt(pref.getString("albumArtQuality", "1"));
    }

    /**
     * We try to avoid songs which have remix/cover/live mentioned in their title. We also make sure we get a relevant
     * result by checking if the first word of song is present in song's name.
     * If we don't find any song which matches our conditions, we use the first song itself.
     *
     * @param response the JSON response
     * @return the SoundCloud stream link
     */
    public String[] parseSoundcloud(String response, String song) throws IOException, XmlPullParserException {
        String soundcloudKey = context.getResources().getStringArray(R.array.keys)[0];
        String streamLink = "", firstLink = "", permaLink = "", firstPermaLink = "";
        String[] links = new String[2];
        String firstWord;
        if (song.contains(" ")) {
            firstWord = song.substring(0, song.indexOf(" "));
        } else {
            firstWord = song;
        }
        firstWord = firstWord.toLowerCase();
        boolean ignore = false;
        Pattern pat = Pattern.compile("\\b(remix|cover|live)\\b");

        InputStream in = getStringAsInputStream(response);
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(in, null);

        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            String name = parser.getName();

            if (event == XmlPullParser.START_TAG) {
                if (name.equals("title")) {
                    if (parser.next() == XmlPullParser.TEXT) {
                        if (pat.matcher(parser.getText().toLowerCase()).find() || !parser.getText().toLowerCase().contains(firstWord)) {
                            Log.d(TAG, "Track ignored: " + parser.getText() + " " + firstWord);
                            ignore = true;
                        }
                    }
                } else if (name.equals("permalink-url")) {
                    if (firstPermaLink.isEmpty() && parser.next() == XmlPullParser.TEXT) {
                        firstPermaLink = parser.getText();
                        permaLink = firstPermaLink;
                    } else if (parser.next() == XmlPullParser.TEXT && !ignore) {
                        permaLink = parser.getText();
                    }
                    if (!ignore) {
                        links[0] = permaLink;
                    }
                } else if (name.equals("stream-url")) {
                    if (firstLink.isEmpty() && parser.next() == XmlPullParser.TEXT) {
                        firstLink = parser.getText() + "?client_id=" + soundcloudKey;
                        streamLink = firstLink;

                    } else if (parser.next() == XmlPullParser.TEXT && !ignore) {
                        streamLink = parser.getText() + "?client_id=" + soundcloudKey;
                    }
                    if (ignore) {
                        ignore = false;
                    } else {
                        links[1] = streamLink;
                        return links;
                    }
                }
            }
            event = parser.next();
        }

        links[0] = firstPermaLink;
        links[1] = firstLink;
        return links;
    }

    InputStream getStringAsInputStream(String text) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(text.getBytes(CharEncoding.UTF_8));
    }
}
