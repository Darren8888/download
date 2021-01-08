package com.darren.download.thread;

public interface DownloadThreadListener {
    void onProgress(String threadId, long progress);
    void onDownloadSuccess(String threadId);
    void onDownloadFailed(String threadId);
}
