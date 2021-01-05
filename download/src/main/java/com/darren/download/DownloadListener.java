package com.darren.download;

import com.darren.download.exception.DownloadException;

public interface DownloadListener {
    void onStart(DownloadInfo downloadInfo);
    void onPaused(DownloadInfo downloadInfo);
    void updateDownloadInfo(DownloadInfo downloadInfo);
    void onRemoved(DownloadInfo downloadInfo);
    void onDownloadSuccess(DownloadInfo downloadInfo);
    void onDownloadFailed(DownloadInfo downloadInfo, DownloadException e);
}
