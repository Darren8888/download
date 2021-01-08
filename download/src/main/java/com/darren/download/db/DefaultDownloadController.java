package com.darren.download.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.ArrayMap;

import com.darren.download.DownloadInfo;
import com.darren.download.DownloadStatus;
import com.darren.download.DownloadThreadInfo;
import com.darren.download.log.LogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultDownloadController implements DownloadDBController {

    private static final String SQL_REPLACE_DOWNLOAD_INFO = String.format(
        "REPLACE INTO %s (_id,supportRanges,forceInstall,createAt,url,path,size,progress,status) VALUES(?,?,?,?,?,?,?,?,?);",
            DefaultDownloadHelper.TABLE_NAME_DOWNLOAD_INFO);

    private static final String SQL_REPLACE_DOWNLOAD_THREAD_INFO = String.format(
        "REPLACE INTO %s (threadId, downloadInfoId, url, start, end, progress) VALUES(?, ?, ?, ?, ?, ?);",
            DefaultDownloadHelper.TABLE_NAME_DOWNLOAD_THREAD_INFO
    );

    private static final String[] DOWNLOAD_INFO_COLUMNS = new String[] {"_id", "supportRanges", "forceInstall",
            "createAt", "url", "path",
            "size", "progress", "status"
    };

    private static final String[] DOWNLOAD_THREAD_INFO_COLUMNS = new String[] {
            "threadId", "downloadInfoId", "url", "start", "end", "progress"
    };

    private final DefaultDownloadHelper dbHelper;
    private final SQLiteDatabase writableDatabase;
    private final SQLiteDatabase readableDatabase;

    public DefaultDownloadController(Context context) {
        this.dbHelper = new DefaultDownloadHelper(context);
        this.writableDatabase = dbHelper.getWritableDatabase();
        this.readableDatabase = dbHelper.getReadableDatabase();
    }

    @Override
    public synchronized void close() {
        if (writableDatabase.isOpen()) {
            writableDatabase.close();
        }
        if (readableDatabase.isOpen()) {
            readableDatabase.close();
        }
    }

    @Override
    public synchronized void update(DownloadInfo downloadInfo) {
        LogUtils.logd("DBController", "" + downloadInfo.getTaskId() + ", " + downloadInfo.getSupportRanges()
            + ", " + downloadInfo.getForceInstall() + ", " + downloadInfo.getCreateAt()
                + ", " + downloadInfo.getUrl() + ", " + downloadInfo.getSavePath()
                + ", " + downloadInfo.getSize() + ", " + downloadInfo.getProgress() + ", " + downloadInfo.getStatus()
        );
        writableDatabase.execSQL(
                SQL_REPLACE_DOWNLOAD_INFO,
                new Object[] {
                        downloadInfo.getTaskId(), downloadInfo.getSupportRanges(),
                        downloadInfo.getForceInstall(), downloadInfo.getCreateAt(),
                        downloadInfo.getUrl(), downloadInfo.getSavePath(),
                        downloadInfo.getSize(), downloadInfo.getProgress(), downloadInfo.getStatus()
                }
        );

        ConcurrentHashMap<String, DownloadThreadInfo> threadInfoMap = (ConcurrentHashMap)downloadInfo.getDownloadThreadInfoList();
        Iterator entrys = threadInfoMap.entrySet().iterator();
        while (entrys.hasNext()) {
            Map.Entry entry = (Map.Entry) entrys.next();
            DownloadThreadInfo downloadThreadInfo = (DownloadThreadInfo) entry.getValue();
            update(downloadThreadInfo);
        }
    }

    @Override
    public synchronized void delete(DownloadInfo downloadInfo) {
        writableDatabase.delete(DefaultDownloadHelper.TABLE_NAME_DOWNLOAD_INFO, "_id=?",
            new String[] {String.valueOf(downloadInfo.getTaskId())}
        );

        writableDatabase.delete(DefaultDownloadHelper.TABLE_NAME_DOWNLOAD_THREAD_INFO, "downloadInfoId=?",
            new String[] {String.valueOf(downloadInfo.getTaskId())}
        );
    }

    @Override
    public synchronized void update(DownloadThreadInfo downloadThreadInfo) {
        writableDatabase.execSQL(SQL_REPLACE_DOWNLOAD_THREAD_INFO,
                new Object[] {
                        downloadThreadInfo.getThreadId(), downloadThreadInfo.getDownloadInfoId(),
                        downloadThreadInfo.getUrl(), downloadThreadInfo.getStart(),
                        downloadThreadInfo.getEnd(), downloadThreadInfo.getProgress()
                }
        );
    }

    @Override
    public synchronized void delete(DownloadThreadInfo downloadThreadInfo) {
        writableDatabase.delete(DefaultDownloadHelper.TABLE_NAME_DOWNLOAD_THREAD_INFO, "threadId=?",
                new String[] {downloadThreadInfo.getThreadId()}
        );
    }

    @Override
    public synchronized DownloadInfo getDownloadInfoById(int id) {
        Cursor cursor = readableDatabase.query(DefaultDownloadHelper.TABLE_NAME_DOWNLOAD_INFO, DOWNLOAD_INFO_COLUMNS, "_id=?",
                new String[] {String.valueOf(id)}, null, null, "createAt desc"
                );

        if (cursor.moveToNext()) {
            DownloadInfo downloadInfo = new DownloadInfo();
            inflateDownloadInfo(cursor, downloadInfo);
            return downloadInfo;
        }
        return null;
    }

    private void inflateDownloadInfo(Cursor cursor, DownloadInfo downloadInfo) {
        downloadInfo.setTaskId(cursor.getString(0));
        downloadInfo.setSupportRanges(cursor.getInt(1));
        downloadInfo.setForceInstall(cursor.getInt(2));
        downloadInfo.setCreateAt(cursor.getLong(3));
        downloadInfo.setUrl(cursor.getString(4));
        downloadInfo.setSavePath(cursor.getString(5));
        downloadInfo.setSize(cursor.getLong(6));
        downloadInfo.setProgress(cursor.getLong(7));
        downloadInfo.setStatus(cursor.getInt(8));
    }

    @Override
    public synchronized Map<String, DownloadInfo> getAllDownloading() {
        Cursor cursor = readableDatabase.query(DefaultDownloadHelper.TABLE_NAME_DOWNLOAD_INFO,
                DOWNLOAD_INFO_COLUMNS, "status != ?",
                new String[] {String.valueOf(DownloadStatus.STATUS_COMPLETED)},
                null, null, "createAt desc"
                );

        Map<String, DownloadInfo> downloadInfoList = new ConcurrentHashMap<>();
        Cursor downloadThreadinfoCursor;
        while (cursor.moveToNext()) {
            LogUtils.logd("DbController", "getAllDownloading");
            DownloadInfo downloadInfo = new DownloadInfo();
            inflateDownloadInfo(cursor, downloadInfo);
            downloadInfoList.put(downloadInfo.getTaskId(), downloadInfo);

            LogUtils.logd("DbController", "getAllDownloading id: " + downloadInfo.getTaskId());

            downloadThreadinfoCursor = readableDatabase.query(DefaultDownloadHelper.TABLE_NAME_DOWNLOAD_THREAD_INFO,
                    DOWNLOAD_THREAD_INFO_COLUMNS, "downloadInfoId = ?",
                    new String[] {String.valueOf(downloadInfo.getTaskId())},
                    null, null, null
                    );

            while (downloadThreadinfoCursor.moveToNext()) {
                DownloadThreadInfo threadInfo = new DownloadThreadInfo();
                inflateDownloadThreadInfo(downloadThreadinfoCursor, threadInfo);
                downloadInfo.addDownloadThreadInfo(threadInfo);

                LogUtils.logd("DbController", "getAllDownloading threadInfo id: " + threadInfo.getThreadId());
            }
        }

        return downloadInfoList;
    }

    private void inflateDownloadThreadInfo(Cursor cursor, DownloadThreadInfo downloadThreadInfo) {
        downloadThreadInfo.setThreadId(cursor.getString(0));
        downloadThreadInfo.setDownloadInfoId(cursor.getString(1));
        downloadThreadInfo.setUrl(cursor.getString(2));
        downloadThreadInfo.setStart(cursor.getLong(3));
        downloadThreadInfo.setEnd(cursor.getLong(4));
        downloadThreadInfo.setProgress(cursor.getLong(5));
    }

}
