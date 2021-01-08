package com.darren.download;

import com.darren.download.exception.DownloadException;

public interface DownloadTaskListener {
    void onUpdateProgress(DownloadInfo downloadInfo);
    void onSuccess(DownloadInfo downloadInfo);
    void onFailed(DownloadInfo downloadInfo, DownloadException exception);
}
