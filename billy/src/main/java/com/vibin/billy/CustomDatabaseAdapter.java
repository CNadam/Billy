package com.vibin.billy;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class CustomDatabaseAdapter {
    DatabaseHelper hlp;
    Context c;
    public CustomDatabaseAdapter(Context context) {
        this.c = context;
        hlp = new DatabaseHelper(c);
    }

/*   long insertData(String album, String artist, String artwork, String song, int pos){
       Log.d(getClass().getName(), "InsertData called");
       SQLiteDatabase db = hlp.getWritableDatabase();
       ContentValues cv = new ContentValues();
       cv.put(DatabaseHelper.UID,pos);
       cv.put(DatabaseHelper.ALBUM,album);
       cv.put(DatabaseHelper.ARTIST,artist);
       cv.put(DatabaseHelper.ARTWORK,artwork);
       cv.put(DatabaseHelper.SONG,song);
       long yolo = db.insert(DatabaseHelper.TABLE_NAME,null,cv); // Returns -1 if insertion fails
       db.close();
       return yolo;
   }*/

    public long replaceData() {
        Log.d(getClass().getName(), "InsertData called");
        SQLiteDatabase db = hlp.getWritableDatabase();
        ContentValues cv = new ContentValues();
        long lol = 1;
        return lol;
    }

    static class DatabaseHelper extends SQLiteOpenHelper{
        private static final int VERSION = 5;
        private static final String DATABASE_NAME = "BillyDatabase";
        private static final String TABLE_NAME = "MostPopular";
        private static final String UID = "_id";
        private static final String ArrayList = "ArrayList";
        /*private static final String ALBUM = "Album";
        private static final String ARTIST = "Artist";
        private static final String ARTWORK = "Artwork";
        private static final String SONG = "Song";
*/
        private static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+" ("+UID+" TINYINT(255) PRIMARY KEY, "+ArrayList+" TEXT "+");";
        private static final String DROP_TABLE = "DROP TABLE IF EXISTS "+TABLE_NAME;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(getClass().getName(), "DB On Create");
            try {
                db.execSQL(CREATE_TABLE);
            } catch (SQLException e) {
                Log.d(getClass().getName(), ""+e);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int i, int i2) {
            Log.d(getClass().getName(), "DB On Upgrade");

            try {
                db.execSQL(DROP_TABLE);
            } catch (SQLException e) {
                Log.d(getClass().getName(), ""+e);
            }

            onCreate(db);
        }
    }
}
