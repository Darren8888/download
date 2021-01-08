package com.darren.download;

import com.darren.download.exception.DownloadException;

public interface DownloadTaskInterface {
    int start() throws DownloadException;
    void stop();
    DownloadInfo getDownloadInfo();
}
