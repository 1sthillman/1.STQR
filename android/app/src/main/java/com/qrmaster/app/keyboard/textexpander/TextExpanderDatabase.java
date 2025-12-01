package com.qrmaster.app.keyboard.textexpander;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite veritabanı - Text Expander kısayolları
 */
public class TextExpanderDatabase extends SQLiteOpenHelper {
    private static final String TAG = "TextExpanderDB";
    private static final String DB_NAME = "text_expander.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE_SHORTCUTS = "shortcuts";
    private static final String COL_ID = "id";
    private static final String COL_TRIGGER = "trigger";
    private static final String COL_EXPANSION = "expansion";
    private static final String COL_DESCRIPTION = "description";
    private static final String COL_ENABLED = "enabled";
    private static final String COL_USE_COUNT = "use_count";
    private static final String COL_LAST_USED = "last_used";

    private static TextExpanderDatabase instance;

    public static synchronized TextExpanderDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new TextExpanderDatabase(context.getApplicationContext());
        }
        return instance;
    }

    private TextExpanderDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_SHORTCUTS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TRIGGER + " TEXT UNIQUE NOT NULL, " +
                COL_EXPANSION + " TEXT NOT NULL, " +
                COL_DESCRIPTION + " TEXT, " +
                COL_ENABLED + " INTEGER DEFAULT 1, " +
                COL_USE_COUNT + " INTEGER DEFAULT 0, " +
                COL_LAST_USED + " INTEGER DEFAULT 0)";
        db.execSQL(createTable);
        
        // Varsayılan kısayollar ekle
        insertDefault(db, "/mail", "ornek@email.com", "E-posta Adresi");
        insertDefault(db, "/tel", "+90 555 123 4567", "Telefon Numarası");
        insertDefault(db, "/adres", "Örnek Mah. Örnek Sok. No: 1\nİstanbul, Türkiye", "Ev Adresi");
        
        Log.d(TAG, "✅ Veritabanı oluşturuldu");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SHORTCUTS);
        onCreate(db);
    }

    private void insertDefault(SQLiteDatabase db, String trigger, String expansion, String description) {
        ContentValues values = new ContentValues();
        values.put(COL_TRIGGER, trigger);
        values.put(COL_EXPANSION, expansion);
        values.put(COL_DESCRIPTION, description);
        values.put(COL_ENABLED, 1);
        values.put(COL_USE_COUNT, 0);
        values.put(COL_LAST_USED, 0);
        db.insert(TABLE_SHORTCUTS, null, values);
    }

    /**
     * Kısayol ekle
     */
    public long addShortcut(TextShortcut shortcut) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TRIGGER, shortcut.getTrigger());
        values.put(COL_EXPANSION, shortcut.getExpansion());
        values.put(COL_DESCRIPTION, shortcut.getDescription());
        values.put(COL_ENABLED, shortcut.isEnabled() ? 1 : 0);
        values.put(COL_USE_COUNT, shortcut.getUseCount());
        values.put(COL_LAST_USED, shortcut.getLastUsed());
        
        long id = db.insert(TABLE_SHORTCUTS, null, values);
        Log.d(TAG, "➕ Kısayol eklendi: " + shortcut.getTrigger());
        return id;
    }

    /**
     * Kısayol güncelle
     */
    public int updateShortcut(TextShortcut shortcut) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TRIGGER, shortcut.getTrigger());
        values.put(COL_EXPANSION, shortcut.getExpansion());
        values.put(COL_DESCRIPTION, shortcut.getDescription());
        values.put(COL_ENABLED, shortcut.isEnabled() ? 1 : 0);
        values.put(COL_USE_COUNT, shortcut.getUseCount());
        values.put(COL_LAST_USED, shortcut.getLastUsed());
        
        return db.update(TABLE_SHORTCUTS, values, COL_ID + "=?", 
                new String[]{String.valueOf(shortcut.getId())});
    }

    /**
     * Kısayol sil
     */
    public int deleteShortcut(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_SHORTCUTS, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    /**
     * Trigger'a göre kısayol bul
     */
    public TextShortcut findByTrigger(String trigger) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SHORTCUTS, null, 
                COL_TRIGGER + "=? AND " + COL_ENABLED + "=1", 
                new String[]{trigger}, null, null, null);
        
        TextShortcut shortcut = null;
        if (cursor != null && cursor.moveToFirst()) {
            shortcut = cursorToShortcut(cursor);
            cursor.close();
        }
        return shortcut;
    }

    /**
     * Tüm kısayolları getir
     */
    public List<TextShortcut> getAllShortcuts() {
        List<TextShortcut> shortcuts = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SHORTCUTS, null, null, null, null, null, COL_USE_COUNT + " DESC");
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                shortcuts.add(cursorToShortcut(cursor));
            }
            cursor.close();
        }
        return shortcuts;
    }

    /**
     * Kullanım sayısını artır
     */
    public void incrementUsage(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_SHORTCUTS + 
                " SET " + COL_USE_COUNT + " = " + COL_USE_COUNT + " + 1, " +
                COL_LAST_USED + " = " + System.currentTimeMillis() +
                " WHERE " + COL_ID + " = " + id);
    }

    private TextShortcut cursorToShortcut(Cursor cursor) {
        return new TextShortcut(
            cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(COL_TRIGGER)),
            cursor.getString(cursor.getColumnIndexOrThrow(COL_EXPANSION)),
            cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)),
            cursor.getInt(cursor.getColumnIndexOrThrow(COL_ENABLED)) == 1,
            cursor.getInt(cursor.getColumnIndexOrThrow(COL_USE_COUNT)),
            cursor.getLong(cursor.getColumnIndexOrThrow(COL_LAST_USED))
        );
    }
}

