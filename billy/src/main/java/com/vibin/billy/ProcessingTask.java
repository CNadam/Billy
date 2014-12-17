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

    private String[] billySong, billyArtist;
    private int billySize;
    private Context context;
    private int quality;

    /**
     * For SoundCloud parsing
     */

    public ProcessingTask(Context c) {
        this.context = c;
    }

    /**
     * For Billboard/iTunes fetching and parsing
     *
     * @param billySize Number of songs
     */
    public ProcessingTask(int billySize, Context c) {
        this.billySize = billySize;
        billySong = new String[billySize];
        billyArtist = new String[billySize];
        this.context = c;
        refreshArtworkUrlResolution();
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
     * Parses XML from Billboard and populates {@link #billySong} and {@link #billyArtist}
     *
     * @param response A String containing XML
     */
    public void parseBillboard(String response) throws IOException, XmlPullParserException {
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
    }

    public String[] getBillySong() {
        return billySong;
    }

    public void setBillySong(String[] billySong) {
        this.billySong = billySong;
    }

    public String[] getBillyArtist() {
        return billyArtist;
    }

    public void setBillyArtist(String[] billyArtist) {
        this.billyArtist = billyArtist;
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

            //Log.d(TAG, "artwork url is " + artworkUrl);

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
            trackName = replaceSmartQuotes(trackName);

            int match = matchMagic(billySong, trackName);

            // Track name from Billboard and iTunes don't match
            if (match == -1) {
                Log.e(TAG, "The unmatched itunes song is " + trackName);
                int matchArtist = matchMagic(billyArtist, artistName);
                counter++;
                if (matchArtist == -1) {
                    Log.e(TAG, "Artists haven't matched " + artistName + " and counter is " + counter);
                } else {
                    Log.e(TAG, "Something wrong with text manipulation " + trackName + " " + artistName);
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
        String extractedArtist = text.substring(text.indexOf("by") + 3, text.indexOf("ranks"));
        if (extractedArtist.contains("Featuring")) {
            extractedArtist = extractedArtist.substring(0, extractedArtist.indexOf("Featuring"));
        } else if (extractedArtist.contains("Ft")) {
            extractedArtist = extractedArtist.substring(0, extractedArtist.indexOf("Ft"));
        } else if (extractedArtist.contains("Duet")) {
            extractedArtist = extractedArtist.substring(0, extractedArtist.indexOf("Duet"));
        }
        extractedArtist = StringUtils.stripAccents(extractedArtist);
        return extractedArtist.trim();
    }

    /**
     * Searches for given String in the String array and returns index. Case-insensitive.
     * If no match is found, we try Levenshtein's Algo
     */

    private int matchMagic(String[] billySong, String trackName) {
        int index = 0;
        for (String name : billySong) {
            if (name != null) {
                if (name.equalsIgnoreCase(trackName)) {
                    return index;
                }
                index++;
            } else {
                Log.d(TAG, "Index is " + index + " trackName is " + trackName);
            }
        }
        return getLevenshteinMatch(billySong, trackName);
    }

    /**
     * Uses the Levenshtein's Algorithm to find the closest match for iTunes song in {@link #billySong}
     */

    private int getLevenshteinMatch(String[] billySong, String trackName) {
        int index = 0;
        for (String name : billySong) {
            if (name != null) {
                int match = StringUtils.getLevenshteinDistance(billySong[index].toLowerCase(), trackName.toLowerCase());
                if (match >= 0 && match <= 3) {
                    Log.i(TAG, "Levenshtein Algo passed: " + billySong[index] + " " + trackName);
                    billySong[index] = trackName;
                    return index;
                }
            } else {
                Log.d(TAG, "Index is " + index + " trackName is " + trackName);
            }
            index++;
        }
        return -1;
    }


    /**
     * Encode the song/artist name
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

    private String replaceSmartQuotes(String str) {
        str = replaceChars(str, "\u0091\u0092\u2018\u2019", '\'');
        str = replaceChars(str, "\u0093\u0094\u201c\u201d", '"');
        return str.trim();
    }

    private String replaceChars(String str, String oldchars, char newchar) {
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

    private int indexOfChars(String str, String chars, int fromIndex) {
        for (int pos = fromIndex; pos < str.length(); pos++) {
            if (chars.indexOf(str.charAt(pos)) >= 0) {
                return pos;
            }
        }
        return -1;
    }

    /**
     * Refreshes {@link #quality} by setting it to value from SharedPreference
     *
     * @return true if Album art quality preference is changed
     */

    public boolean refreshArtworkUrlResolution() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        int newQuality = Integer.parseInt(pref.getString("albumArtQuality", "1"));
        if (quality != 0) {
            if (quality != newQuality) {
                quality = newQuality;
                return true;
            } else {
                return false;
            }
        } else {
            // doesn't matter what you return here, as quality is zero,
            // only when this method is called by this class' constructor
            quality = newQuality;
            return true;
        }
    }

    /**
     * We try to avoid songs which have remix/cover/live mentioned in their title.
     * We also make sure we get a relevant result by checking if the first word of song
     * is present in song's name.
     * <p/>
     * If we don't find any song which matches our conditions, we fallback to the first song.
     *
     * @param response the JSONArray response
     * @return the SoundCloud stream link
     */

    public String[] parseSoundcloud(JSONArray response, String song) throws JSONException {
        String soundcloudKey = context.getResources().getStringArray(R.array.keys)[0];
        String streamLink = "", firstLink = "", permaLink = "", firstPermaLink = "", firstUser = "", firstDuration = "";
        String[] links = new String[4];
        String firstWord;
        int count = 0;
        if (song.contains(" ")) {
            firstWord = song.substring(0, song.indexOf(" "));
        } else {
            firstWord = song;
        }
        firstWord = firstWord.toLowerCase();
        boolean ignore = false;
        Pattern pat = Pattern.compile("\\b(remix|cover|guitar|parody|acoustic|instrumental|drums|cloudseeder)\\b");
        while (count < 8) {
            JSONObject object = response.getJSONObject(count);
            boolean streamble = object.getBoolean("streamable");
            if(streamble) {
                String tags = object.getString("tag_list");
                if (pat.matcher(tags.toLowerCase()).find()) {
                    Log.d(TAG, count + " tag-list: " + tags.toLowerCase());
                    ignore = true;
                }
                String title = object.getString("title");
                if (pat.matcher(title.toLowerCase()).find() || !title.toLowerCase().contains(firstWord)) {
                    Log.d(TAG, count + " title: " + title + " " + firstWord);
                    ignore = true;
                }
                String desc = object.getString("description");
                if (pat.matcher(desc.toLowerCase()).find()) {
                    ignore = true;
                }

                String user = object.getJSONObject("user").getString("username");
                if (firstUser.isEmpty()) {
                    firstUser = user;
                }
                if (!ignore) {
                    links[0] = user;
                }

                String duration = String.valueOf(object.getInt("duration"));
                if(firstDuration.isEmpty())
                {
                    firstDuration = duration;
                }
                if(!ignore)
                {
                    links[1] = duration;
                }
                permaLink = object.getString("permalink_url");
                if (firstPermaLink.isEmpty()) {
                    firstPermaLink = permaLink;
                }
                if (!ignore) {
                    links[2] = permaLink;
                }

                streamLink = object.getString("stream_url") + "?client_id=" + soundcloudKey;
                if (firstLink.isEmpty()) {
                    firstLink = streamLink;
                }

                if (ignore) {
                    ignore = false;
                } else {
                    links[3] = streamLink;
                    return links;
                }
            }
            count++;
        }
        links[0] = firstUser;
        links[1] = firstDuration;
        links[2] = firstPermaLink;
        links[3] = firstLink;
        return links;
    }

    private InputStream getStringAsInputStream(String text) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(text.getBytes(CharEncoding.UTF_8));
    }
}
