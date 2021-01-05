package com.darren.download;

import android.content.Context;

public interface DownloadManager {
    void init(Context context, DownloadConfig config, DownloadListener downloadListener, DownloadManagerImpl.InitListener listener);
    void start(DownloadInfo downloadInfo);
    void pause(DownloadInfo downloadInfo);
    void resume(DownloadInfo downloadInfo);
    void remove(DownloadInfo downloadInfo);
    void destroy();
}
