package com.darren.download;

public class DownloadStatus {
    public static final int STATUS_NONE = 0;
    public static final int STATUS_PREPARE_DOWNLOAD = 1;
    public static final int STATUS_DOWNLOADING = 2;
    public static final int STATUS_WAIT = 3;
    public static final int STATUS_PAUSED = 4;
    public static final int STATUS_COMPLETED = 5;
    public static final int STATUS_ERROR = 6;
    public static final int STATUS_REMOVE = 7;
    public static final int STATUS_RETRY = 8;
}
