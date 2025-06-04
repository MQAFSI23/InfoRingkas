package com.example.inforingkas.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.inforingkas.model.Berita;
import com.example.inforingkas.util.Constants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    // Query pembuatan tabel
    private static final String CREATE_TABLE_BERITA = "CREATE TABLE " + Constants.TABLE_BERITA + "("
            + Constants.COLUMN_ARTICLE_ID + " TEXT PRIMARY KEY,"
            + Constants.COLUMN_TITLE + " TEXT,"
            + Constants.COLUMN_LINK + " TEXT,"
            + Constants.COLUMN_PUB_DATE + " TEXT," // Simpan sebagai TEXT, bisa di-parse ke Date jika perlu
            + Constants.COLUMN_IMAGE_URL + " TEXT,"
            + Constants.COLUMN_SOURCE_NAME + " TEXT,"
            + Constants.COLUMN_SOURCE_ICON + " TEXT,"
            + Constants.COLUMN_LANGUAGE + " TEXT,"
            + Constants.COLUMN_CATEGORY + " TEXT," // Simpan sebagai comma-separated string
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
        // Jika ada perubahan skema, drop tabel lama dan buat baru (data akan hilang)
        // Untuk migrasi data yang lebih kompleks, perlu implementasi khusus.
        db.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_BERITA);
        onCreate(db);
        Log.d(TAG, "Database upgraded from version " + oldVersion + " to " + newVersion);
    }

    // --- Operasi CRUD untuk Berita ---

    /**
     * Menambahkan berita baru atau memperbarui jika sudah ada (berdasarkan article_id).
     * Tidak mengubah status favorit atau rangkuman jika berita sudah ada, kecuali jika nilai baru diberikan.
     * @param berita Objek Berita yang akan ditambahkan/diperbarui.
     * @param isTerkini Menandakan apakah berita ini dari fetch terkini.
     * @return ID baris dari berita yang baru dimasukkan, atau -1 jika terjadi error.
     */
    public long addOrUpdateBerita(Berita berita, boolean isTerkini) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_ARTICLE_ID, berita.getArticleId());
        values.put(Constants.COLUMN_TITLE, berita.getTitle());
        values.put(Constants.COLUMN_LINK, berita.getLink());
        values.put(Constants.COLUMN_PUB_DATE, berita.getPubDate());
        values.put(Constants.COLUMN_IMAGE_URL, berita.getImageUrl());
        values.put(Constants.COLUMN_SOURCE_NAME, berita.getSourceName());
        values.put(Constants.COLUMN_SOURCE_ICON, berita.getSourceIcon());
        values.put(Constants.COLUMN_LANGUAGE, berita.getLanguage());
        values.put(Constants.COLUMN_CATEGORY, berita.getCategoryString()); // Simpan sebagai string

        // Hanya update is_terkini jika memang ini adalah proses update berita terkini
        // Jika berita sudah ada, jangan reset is_terkini ke 0 kecuali memang bukan berita terkini lagi.
        // Untuk kasus ini, kita set isTerkini berdasarkan parameter.
        values.put(Constants.COLUMN_IS_TERKINI, isTerkini ? 1 : 0);

        // Jika berita sudah ada, kita tidak ingin menimpa status favorit atau rangkuman
        // kecuali jika memang ada perubahan dari objek Berita yang di-pass.
        // Untuk insert awal, isFavorite dan rangkuman dari objek Berita akan digunakan.
        if (beritaExists(berita.getArticleId())) {
            // Jika sudah ada, kita mungkin hanya ingin update beberapa field,
            // atau menggunakan insertWithOnConflict dengan CONFLICT_REPLACE jika ingin menimpa semua.
            // Untuk kasus ini, kita asumsikan jika berita dari API, kita update datanya
            // tapi pertahankan status favorit dan rangkuman yang sudah ada.
            Berita existingBerita = getBerita(berita.getArticleId());
            if (existingBerita != null) {
                values.put(Constants.COLUMN_IS_FAVORITE, existingBerita.isFavorite() ? 1 : 0);
                if (existingBerita.getRangkuman() != null) {
                    values.put(Constants.COLUMN_RANGKUMAN, existingBerita.getRangkuman());
                }
            }
            // Jika objek berita yang di-pass memiliki update untuk favorit/rangkuman, gunakan itu.
            if (berita.isFavorite() != (existingBerita != null && existingBerita.isFavorite())) { // Jika ada perubahan favorit
                values.put(Constants.COLUMN_IS_FAVORITE, berita.isFavorite() ? 1 : 0);
            }
            if (berita.getRangkuman() != null && (existingBerita == null || !berita.getRangkuman().equals(existingBerita.getRangkuman()))) { // Jika ada rangkuman baru/berbeda
                values.put(Constants.COLUMN_RANGKUMAN, berita.getRangkuman());
            }

            long affectedRows = db.update(Constants.TABLE_BERITA, values, Constants.COLUMN_ARTICLE_ID + " = ?",
                    new String[]{berita.getArticleId()});
            db.close();
            return affectedRows > 0 ? 1 : 0; // Return 1 jika update berhasil, 0 jika tidak ada row terupdate
        } else {
            // Berita belum ada, insert baru
            values.put(Constants.COLUMN_IS_FAVORITE, berita.isFavorite() ? 1 : 0);
            values.put(Constants.COLUMN_RANGKUMAN, berita.getRangkuman()); // Bisa null
            long id = db.insert(Constants.TABLE_BERITA, null, values);
            db.close();
            return id;
        }
    }

    /**
     * Menambahkan daftar berita. Berguna untuk batch insert dari API.
     * @param beritaList Daftar objek Berita.
     * @param isTerkini Menandakan apakah daftar berita ini dari fetch terkini.
     */
    public void addAllBerita(List<Berita> beritaList, boolean isTerkini) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Berita berita : beritaList) {
                // Cek apakah berita sudah ada untuk menghindari duplikasi is_terkini jika berita sama dari page berbeda
                Berita existing = getBerita(berita.getArticleId());
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
                    // Berita sudah ada, update dan pertahankan status favorit & rangkuman
                    values.put(Constants.COLUMN_IS_FAVORITE, existing.isFavorite() ? 1 : 0);
                    values.put(Constants.COLUMN_RANGKUMAN, existing.getRangkuman());
                    if (isTerkini) { // Hanya set is_terkini jika memang ini proses update berita terkini
                        values.put(Constants.COLUMN_IS_TERKINI, 1);
                    } else {
                        values.put(Constants.COLUMN_IS_TERKINI, existing.isTerkini() ? 1 : 0); // Pertahankan status terkini jika tidak di-override
                    }
                    db.update(Constants.TABLE_BERITA, values, Constants.COLUMN_ARTICLE_ID + " = ?", new String[]{berita.getArticleId()});
                } else {
                    // Berita baru, insert
                    values.put(Constants.COLUMN_IS_FAVORITE, berita.isFavorite() ? 1 : 0); // default dari objek
                    values.put(Constants.COLUMN_RANGKUMAN, berita.getRangkuman()); // default dari objek
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

    /**
     * Mendapatkan satu berita berdasarkan article_id.
     * @param articleId ID artikel.
     * @return Objek Berita, atau null jika tidak ditemukan.
     */
    public Berita getBerita(String articleId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(Constants.TABLE_BERITA, null,
                Constants.COLUMN_ARTICLE_ID + "=?", new String[]{articleId},
                null, null, null, null);

        Berita berita = null;
        if (cursor.moveToFirst()) {
            berita = cursorToBerita(cursor);
            cursor.close();
        }
        db.close();
        return berita;
    }

    /**
     * Mendapatkan semua berita dari database, diurutkan berdasarkan tanggal publikasi terbaru.
     * @return List objek Berita.
     */
    public List<Berita> getAllBerita() {
        List<Berita> beritaList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + Constants.TABLE_BERITA + " ORDER BY " + Constants.COLUMN_PUB_DATE + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                beritaList.add(cursorToBerita(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return beritaList;
    }

    /**
     * Mendapatkan semua berita yang ditandai sebagai favorit, diurutkan berdasarkan tanggal publikasi terbaru.
     * @return List objek Berita favorit.
     */
    public List<Berita> getFavoriteBerita() {
        List<Berita> beritaList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + Constants.TABLE_BERITA + " WHERE "
                + Constants.COLUMN_IS_FAVORITE + " = 1 ORDER BY " + Constants.COLUMN_PUB_DATE + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                beritaList.add(cursorToBerita(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return beritaList;
    }

    /**
     * Mendapatkan semua berita yang ditandai sebagai terkini (is_terkini = 1),
     * diurutkan berdasarkan tanggal publikasi terbaru.
     * @return List objek Berita terkini.
     */
    public List<Berita> getBeritaTerkiniFromDb() {
        List<Berita> beritaList = new ArrayList<>();
        // Ambil semua berita, lalu filter dan urutkan di Java untuk memastikan urutan yang benar berdasarkan tanggal
        // Karena SQLite mungkin tidak mengurutkan string tanggal dengan benar tanpa format YYYY-MM-DD HH:MM:SS
        String selectQuery = "SELECT * FROM " + Constants.TABLE_BERITA + " WHERE "
                + Constants.COLUMN_IS_TERKINI + " = 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                beritaList.add(cursorToBerita(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        // Sortir di Java untuk memastikan urutan tanggal yang benar
        beritaList.sort(new Comparator<>() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            @Override
            public int compare(Berita b1, Berita b2) {
                try {
                    Date date1 = sdf.parse(b1.getPubDate());
                    Date date2 = sdf.parse(b2.getPubDate());
                    if (date1 != null && date2 != null) {
                        return date2.compareTo(date1); // Descending
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing date for sorting: " + e.getMessage());
                }
                return 0;
            }
        });
        return beritaList;
    }


    /**
     * Memperbarui status favorit sebuah berita.
     * @param articleId ID artikel.
     * @param isFavorite true jika favorit, false jika tidak.
     * @return Jumlah baris yang terpengaruh.
     */
    public int updateFavoriteStatus(String articleId, boolean isFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_IS_FAVORITE, isFavorite ? 1 : 0);

        int rowsAffected = db.update(Constants.TABLE_BERITA, values, Constants.COLUMN_ARTICLE_ID + " = ?",
                new String[]{articleId});
        db.close();
        return rowsAffected;
    }

    /**
     * Memperbarui rangkuman sebuah berita.
     * @param articleId ID artikel.
     * @param rangkuman Teks rangkuman.
     * @return Jumlah baris yang terpengaruh.
     */
    public int updateRangkuman(String articleId, String rangkuman) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_RANGKUMAN, rangkuman);

        int rowsAffected = db.update(Constants.TABLE_BERITA, values, Constants.COLUMN_ARTICLE_ID + " = ?",
                new String[]{articleId});
        db.close();
        return rowsAffected;
    }

    /**
     * Menghapus tanda 'is_terkini' dari semua berita.
     * Dipanggil sebelum mengambil berita terkini baru dari API.
     */
    public void clearIsTerkiniFlags() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_IS_TERKINI, 0);
        db.update(Constants.TABLE_BERITA, values, null, null); // Update semua baris
        db.close();
        Log.d(TAG, "Cleared all is_terkini flags.");
    }


    /**
     * Cek apakah berita dengan articleId tertentu sudah ada di database.
     * @param articleId ID artikel.
     * @return true jika ada, false jika tidak.
     */
    public boolean beritaExists(String articleId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + Constants.COLUMN_ARTICLE_ID + " FROM " + Constants.TABLE_BERITA + " WHERE " + Constants.COLUMN_ARTICLE_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{articleId});
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        // Tidak perlu db.close() di sini jika dipanggil dari dalam metode lain yang sudah mengelola koneksi
        return exists;
    }

    /**
     * Mendapatkan tanggal publikasi (pubDate) dari berita terkini yang paling baru di database.
     * @return String tanggal pubDate (YYYY-MM-DD HH:mm:ss), atau string kosong jika tidak ada.
     */
    public String getLatestPubDateOfTerkini() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT MAX(" + Constants.COLUMN_PUB_DATE + ") FROM " + Constants.TABLE_BERITA
                + " WHERE " + Constants.COLUMN_IS_TERKINI + " = 1";
        Cursor cursor = db.rawQuery(query, null);
        String latestPubDate = "";
        if (cursor.moveToFirst()) {
            latestPubDate = cursor.getString(0); // MAX(pubDate)
            if (latestPubDate == null) latestPubDate = "";
            cursor.close();
        }
        db.close();
        return latestPubDate;
    }


    // Helper untuk mengubah Cursor menjadi objek Berita
    private Berita cursorToBerita(Cursor cursor) {
        Berita berita = new Berita();
        // Gunakan getColumnIndex agar lebih aman jika urutan kolom berubah
        berita.setArticleId(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_ARTICLE_ID)));
        berita.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_TITLE)));
        berita.setLink(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_LINK)));
        berita.setPubDate(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_PUB_DATE)));
        berita.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_IMAGE_URL)));
        berita.setSourceName(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_SOURCE_NAME)));
        berita.setSourceIcon(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_SOURCE_ICON)));
        berita.setLanguage(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_LANGUAGE)));
        berita.setCategoryFromString(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_CATEGORY))); // Ambil sebagai string
        berita.setFavorite(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.COLUMN_IS_FAVORITE)) == 1);
        berita.setRangkuman(cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_RANGKUMAN)));
        berita.setTerkini(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.COLUMN_IS_TERKINI)) == 1);
        return berita;
    }
}