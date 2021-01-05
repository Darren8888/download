package com.darren.download.db;

import com.darren.download.DownloadInfo;
import com.darren.download.DownloadThreadInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface DownloadDBController {
    void close();
    void update(DownloadInfo downloadInfo);
    void delete(DownloadInfo downloadInfo);
    void update(DownloadThreadInfo downloadTHreadInfo);
    void delete(DownloadThreadInfo downloadTHreadInfo);

    DownloadInfo getDownloadInfoById(int id);
    Map<Integer, DownloadInfo> getAllDownloading();
}
