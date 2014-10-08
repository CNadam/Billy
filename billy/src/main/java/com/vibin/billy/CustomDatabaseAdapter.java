package com.vibin.billy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class CustomDatabaseAdapter {

    private static final String TAG = CustomDatabaseAdapter.class.getSimpleName();
    DatabaseHelper hlp;
    Context c;

    public CustomDatabaseAdapter(Context context) {
        this.c = context;
        hlp = new DatabaseHelper(c);
    }

    public long insertArrayList(String data, String table) {
        Log.d(TAG, "insertArrayList called");
        SQLiteDatabase db = hlp.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.UID, 0);
        cv.put(DatabaseHelper.ArrayList, data);
        return db.replace(table, null, cv);
    }

    public String getArrayList(String table) {
        Log.d(TAG, "getArrayList called");
        SQLiteDatabase db = hlp.getWritableDatabase();
        String[] columns = {DatabaseHelper.UID, DatabaseHelper.ArrayList};
        Cursor cursor = db.query(table, columns, DatabaseHelper.UID + " = '" + 0 + "'", null, null, null, null, null);
        String data = null;
        while (cursor.moveToNext()) {
            int columnIndex = cursor.getColumnIndex(DatabaseHelper.ArrayList);
            data = cursor.getString(columnIndex);
        }
        return data;
    }

    static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int VERSION = 7;
        private static final String DATABASE_NAME = "BillyDatabase";
        private static final String[] TABLE_NAME = {"MostPopular", "Pop", "Rock", "Dance"};
        private static final String UID = "_id";
        private static final String ArrayList = "ArrayList";

        public DatabaseHelper(Context context) {
            super(context.getApplicationContext(), DATABASE_NAME, null, VERSION);
        }

        String getCreateTableString(int i) {
            return "CREATE TABLE " + TABLE_NAME[i] + " (" + UID + " TINYINT(255) PRIMARY KEY, " + ArrayList + " TEXT " + ");";
        }

        String getDropTableString(int i) {
            return "DROP TABLE IF EXISTS " + TABLE_NAME[i];
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "DB On Create");
            try {
                for (int i = 0; i < TABLE_NAME.length; i++) {
                    db.execSQL(getCreateTableString(i));
                }
            } catch (SQLException e) {
                Log.d(TAG, "" + e);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int i, int i2) {
            Log.d(TAG, "DB On Upgrade");

            try {
                for (int j = 0; i < TABLE_NAME.length; i++) {
                    db.execSQL(getDropTableString(j));
                }
            } catch (SQLException e) {
                Log.d(TAG, "" + e);
            }

            onCreate(db);
        }
    }
}
