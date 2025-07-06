package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "store.db";
    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context ctx){
        if (instance == null){
            instance = new DatabaseHelper(ctx.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_MESSAGES);
        Log.i("DB helper","db created!!");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+MessageEntry.TABLE_NAME);
        onCreate(db);
    }

    @SuppressLint("Range")
    public ArrayList<EmailMessage> getRecords(){
        ArrayList<EmailMessage> emailMessages = new ArrayList<>();
        Cursor cursor = this.getReadableDatabase()
                .query("messages", null, null, null, null, null, "timestamp DESC");
        while (cursor.moveToNext()) {
            Date msg_date = new Date(cursor.getLong(cursor.getColumnIndex(MessageEntry.TIMESTAMP_FIELD)) * 1000L);

            emailMessages.add(new EmailMessage(
                    cursor.getString(cursor.getColumnIndex(MessageEntry.SENDER_FIELD)),
                    cursor.getString(cursor.getColumnIndex(MessageEntry.MESSAGE_FIELD)),
                    cursor.getString(cursor.getColumnIndex(MessageEntry.SUBJECT_FIELD)),
                    new SimpleDateFormat("EEE MMM dd HH:mm yyyy", Locale.ENGLISH).format(msg_date),
                    "text/html",
                    Long.parseLong(cursor.getString(cursor.getColumnIndex("uid")))
            ));
//            Log.i("DB helper","get "+cursor.getString(cursor.getColumnIndex("message")));
        }
        cursor.close();
        return emailMessages;
    }

    private static class MessageEntry implements BaseColumns{
        public static final String TABLE_NAME = "messages";
        public static final String MESSAGE_FIELD = "message";
        public static final String SENDER_FIELD = "sender";
        public static final String UID_FIELD = "uid";
        public static final String TIMESTAMP_FIELD = "timestamp";
        public static final String SUBJECT_FIELD = "subject";
    }

    private static final String CREATE_TABLE_MESSAGES =
            "CREATE TABLE " + MessageEntry.TABLE_NAME + " (" +
                    MessageEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    MessageEntry.TIMESTAMP_FIELD + " INTEGER NOT NULL, " +
                    MessageEntry.UID_FIELD + " TEXT NOT NULL, " +
                    MessageEntry.MESSAGE_FIELD + " TEXT NOT NULL, " +
                    MessageEntry.SUBJECT_FIELD + " TEXT NOT NULL, " +
                    MessageEntry.SENDER_FIELD + " TEXT NOT NULL);";

    public DatabaseHelper(Context ctx){
        super(ctx,DB_NAME,null,1);
    }

    public List<String> getAllUids(){
        ArrayList<String> uidList = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();

        Cursor cursor = sqLiteDatabase.query(
                MessageEntry.TABLE_NAME,
                new String[]{"uid"},
                null,
                null,
                null,
                null,
                null
        );

        if (cursor.moveToFirst()) {
            do {
                int index = cursor.getColumnIndex("uid");
                String uid = cursor.getString(index);
                uidList.add(uid);

            } while (cursor.moveToNext());
        }

        cursor.close();
        return uidList;
    }

    public String getLastRecord(){
        String res = "false";
        Cursor cursor = this.getReadableDatabase().query(
                MessageEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                MessageEntry.TIMESTAMP_FIELD+" DESC",
                "1"
        );
        if (cursor.moveToFirst() ){
            int col_index = cursor.getColumnIndex(MessageEntry.UID_FIELD);
            if (col_index > 0){
                res = cursor.getString(col_index);
            }
        }
        cursor.close();
        return res;
    }

    public void AddMessage(
            String sender,
            String message,
            String uid,
            Date timestamp,
            String subject){
        ContentValues contentValues = new ContentValues();
        contentValues.put(MessageEntry.SENDER_FIELD,sender);
        contentValues.put(MessageEntry.MESSAGE_FIELD,message);
        contentValues.put(MessageEntry.UID_FIELD,uid);
        contentValues.put(MessageEntry.TIMESTAMP_FIELD,timestamp.getTime() / 1000L);
        contentValues.put(MessageEntry.SUBJECT_FIELD,subject);
        this.getWritableDatabase()
                .insert(MessageEntry.TABLE_NAME,null,contentValues);

//        Log.i("DB helper","add message "+subject);
    }
}
