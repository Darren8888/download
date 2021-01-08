package com.darren.download;

import java.util.List;

public interface DownloadConsumerInterface {
    void add(DownloadInfo downloadInfo);
    void addList(List<DownloadInfo> downloadInfos);
    void pauseDownloadTask(DownloadInfo downloadInfo);
    void resumeDownloadTask(DownloadInfo downloadInfo);
    void stop();
}
