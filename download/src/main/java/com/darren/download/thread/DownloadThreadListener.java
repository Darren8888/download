package com.darren.download.thread;

import com.darren.download.exception.DownloadException;

public interface DownloadThreadListener {
    void onProgress(String threadId, long progress);
    void onDownloadSuccess(String threadId);
    void onDownloadFailed(String threadId, DownloadException exception);
}
