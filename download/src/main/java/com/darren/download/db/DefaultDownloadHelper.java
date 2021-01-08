package com.darren.download.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.darren.download.DownloadConfig;

public final class DefaultDownloadHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "downloads.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_NAME_DOWNLOAD_INFO = "download_info";
    public static final String TABLE_NAME_DOWNLOAD_THREAD_INFO = "download_thread_info";

    private static final String SQL_CREATE_DOWNLOAD_TABLE = String.format(
            "CREATE TABLE IF NOT EXISTS %s (_id varchar(255) PRIMARY KEY NOT NULL, supportRanges integer NOT NULL, forceInstall integer NOT NULL, createAt long NOT NULL, url varchar(255) NOT NULL," +
                    "path varchar(255) NOT NULL, size long NOT NULL, progress long NOT NULL, status integer NOT NULL, md5 varchar(255) NOT NULL);",
            TABLE_NAME_DOWNLOAD_INFO
    );

    private static final String SQL_CREATE_DOWNLOAD_THREAD_TABLE = String.format(
            "CREATE TABLE IF NOT EXISTS %s (threadId varchar(255) PRIMARY KEY NOT NULL, downloadInfoId varchar(255) NOT NULL," +
                    "url varchar(255) NOT NULL, start long NOT NULL, end long NOT NULL, progress long NOT NULL);",
            TABLE_NAME_DOWNLOAD_THREAD_INFO
    );

    public DefaultDownloadHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_DOWNLOAD_TABLE);
        db.execSQL(SQL_CREATE_DOWNLOAD_THREAD_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
