package com.example.inforingkas.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.example.inforingkas.model.Berita;
import com.example.inforingkas.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    private static final String CREATE_TABLE_BERITA = "CREATE TABLE " + Constants.TABLE_BERITA + "("
            + Constants.COLUMN_ARTICLE_ID + " TEXT PRIMARY KEY,"
            + Constants.COLUMN_TITLE + " TEXT,"
            + Constants.COLUMN_LINK + " TEXT,"
            + Constants.COLUMN_PUB_DATE + " TEXT,"
            + Constants.COLUMN_IMAGE_URL + " TEXT,"
            + Constants.COLUMN_SOURCE_NAME + " TEXT,"
            + Constants.COLUMN_SOURCE_ICON + " TEXT,"
            + Constants.COLUMN_LANGUAGE + " TEXT,"
            + Constants.COLUMN_CATEGORY + " TEXT,"
            + Constants.COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0,"
            + Constants.COLUMN_RANGKUMAN + " TEXT,"
            + Constants.COLUMN_IS_TERKINI + " INTEGER DEFAULT 0"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_BERITA);
        Log.d(TAG, "Database tables created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_BERITA);
        onCreate(db);
        Log.d(TAG, "Database upgraded from version " + oldVersion + " to " + newVersion);
    }

    // --- Operasi CRUD untuk Berita ---
    public void addAllBerita(List<Berita> beritaList, boolean isTerkini) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Berita berita : beritaList) {
                // Lewati berita jika tidak memiliki article_id yang valid
                if (berita == null || TextUtils.isEmpty(berita.getArticleId())) {
                    Log.w(TAG, "Skipping news item with null or empty article_id.");
                    continue; // Lanjut ke berita berikutnya
                }

                Berita existing = getBerita(db, berita.getArticleId());

                ContentValues values = new ContentValues();
                values.put(Constants.COLUMN_ARTICLE_ID, berita.getArticleId());
                values.put(Constants.COLUMN_TITLE, berita.getTitle());
                values.put(Constants.COLUMN_LINK, berita.getLink());
                values.put(Constants.COLUMN_PUB_DATE, berita.getPubDate());
                values.put(Constants.COLUMN_IMAGE_URL, berita.getImageUrl());
                values.put(Constants.COLUMN_SOURCE_NAME, berita.getSourceName());
                values.put(Constants.COLUMN_SOURCE_ICON, berita.getSourceIcon());
                values.put(Constants.COLUMN_LANGUAGE, berita.getLanguage());
                values.put(Constants.COLUMN_CATEGORY, berita.getCategoryString());

                if (existing != null) {
                    values.put(Constants.COLUMN_IS_FAVORITE, existing.isFavorite() ? 1 : 0);
                    values.put(Constants.COLUMN_RANGKUMAN, existing.getRangkuman());
                    values.put(Constants.COLUMN_IS_TERKINI, isTerkini ? 1 : 0);
                    db.update(Constants.TABLE_BERITA, values, Constants.COLUMN_ARTICLE_ID + " = ?", new String[]{berita.getArticleId()});
                } else {
                    values.put(Constants.COLUMN_IS_FAVORITE, berita.isFavorite() ? 1 : 0);
                    values.put(Constants.COLUMN_RANGKUMAN, berita.getRangkuman());
                    values.put(Constants.COLUMN_IS_TERKINI, isTerkini ? 1 : 0);
                    db.insert(Constants.TABLE_BERITA, null, values);
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error adding all berita: " + e.getMessage());
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    private Berita getBerita(SQLiteDatabase db, String articleId) {
        try (Cursor cursor = db.query(Constants.TABLE_BERITA, null,
                Constants.COLUMN_ARTICLE_ID + "=?", new String[]{articleId}, null, null, null, null)) {
            if (cursor.moveToFirst()) {
                return cursorToBerita(cursor);
            }
        }
        return null;
    }

    public Berita getBerita(String articleId) {
        try (SQLiteDatabase db = this.getReadableDatabase(); Cursor cursor = db.query(Constants.TABLE_BERITA, null,
                Constants.COLUMN_ARTICLE_ID + "=?", new String[]{articleId}, null, null, null, null)) {
            if (cursor.moveToFirst()) {
                return cursorToBerita(cursor);
            }
        }
        return null;
    }

    public List<Berita> getAllBerita() {
        List<Berita> beritaList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + Constants.TABLE_BERITA + " ORDER BY " + Constants.COLUMN_PUB_DATE + " DESC";
        try (SQLiteDatabase db = this.getReadableDatabase(); Cursor cursor = db.rawQuery(selectQuery, null)) {
            if (cursor.moveToFirst()) {
                do {
                    beritaList.add(cursorToBerita(cursor));
                } while (cursor.moveToNext());
            }
        }
        return beritaList;
    }

    public List<Berita> getFavoriteBerita() {
        List<Berita> beritaList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + Constants.TABLE_BERITA + " WHERE "
                + Constants.COLUMN_IS_FAVORITE + " = 1 ORDER BY " + Constants.COLUMN_PUB_DATE + " DESC";
        try (SQLiteDatabase db = this.getReadableDatabase(); Cursor cursor = db.rawQuery(selectQuery, null)) {
            if (cursor.moveToFirst()) {
                do {
                    beritaList.add(cursorToBerita(cursor));
                } while (cursor.moveToNext());
            }
        }
        return beritaList;
    }

    public List<Berita> getBeritaTerkiniFromDb() {
        List<Berita> beritaList = new ArrayList<>();
        // Perbaikan: Menghapus pengurutan ganda. Cukup andalkan `ORDER BY` dari SQL.
        String selectQuery = "SELECT * FROM " + Constants.TABLE_BERITA + " WHERE "
                + Constants.COLUMN_IS_TERKINI + " = 1 ORDER BY " + Constants.COLUMN_PUB_DATE + " DESC";
        try (SQLiteDatabase db = this.getReadableDatabase(); Cursor cursor = db.rawQuery(selectQuery, null)) {
            if (cursor.moveToFirst()) {
                do {
                    beritaList.add(cursorToBerita(cursor));
                } while (cursor.moveToNext());
            }
        }
        return beritaList;
    }

    // Perbaikan: Mengembalikan jumlah baris yang terpengaruh
    public int updateFavoriteStatus(String articleId, boolean isFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_IS_FAVORITE, isFavorite ? 1 : 0);

        int rowsAffected = db.update(Constants.TABLE_BERITA, values, Constants.COLUMN_ARTICLE_ID + " = ?",
                new String[]{articleId});
        db.close();
        return rowsAffected;
    }

    // Mengembalikan jumlah baris yang terpengaruh
    public int updateRangkuman(String articleId, String rangkuman) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_RANGKUMAN, rangkuman);

        int rowsAffected = db.update(Constants.TABLE_BERITA, values, Constants.COLUMN_ARTICLE_ID + " = ?",
                new String[]{articleId});
        db.close();
        return rowsAffected;
    }

    public void clearIsTerkiniFlags() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_IS_TERKINI, 0);
        db.update(Constants.TABLE_BERITA, values, null, null);
        db.close();
        Log.d(TAG, "Cleared all is_terkini flags.");
    }

    private Berita cursorToBerita(Cursor cursor) {
        Berita berita = new Berita();
        berita.setArticleId(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_ARTICLE_ID)));
        berita.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_TITLE)));
        berita.setLink(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_LINK)));
        berita.setPubDate(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_PUB_DATE)));
        berita.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_IMAGE_URL)));
        berita.setSourceName(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_SOURCE_NAME)));
        berita.setSourceIcon(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_SOURCE_ICON)));
        berita.setLanguage(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_LANGUAGE)));
        berita.setCategoryFromString(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_CATEGORY)));
        berita.setFavorite(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.COLUMN_IS_FAVORITE)) == 1);
        berita.setRangkuman(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_RANGKUMAN)));
        berita.setTerkini(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.COLUMN_IS_TERKINI)) == 1);
        return berita;
    }
}