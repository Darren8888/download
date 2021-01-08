package com.darren.download;

public final class DownloadThreadInfo {
    private String threadId;
    private String downloadInfoId;
    private String url;
    private long start;
    private long end;
    private long progress;

    public DownloadThreadInfo() {

    }

    public DownloadThreadInfo(String downloadInfoId, String threadId, String url, long start, long end) {
        this.downloadInfoId = downloadInfoId;
        this.threadId = threadId;
        this.url = url;
        this.start = start;
        this.end = end;
    }

    public String getDownloadInfoId() {
        return  this.downloadInfoId;
    }
    public void setDownloadInfoId(String downloadInfoId) {
        this.downloadInfoId = downloadInfoId;
    }

    public String getThreadId() {
        return  this.threadId;
    }
    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getUrl() {
        return  this.url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    public long getStart() {
        return  this.start;
    }
    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return  this.end;
    }
    public void setEnd(long end) {
        this.end = end;
    }

    public long getProgress() {
        return  this.progress;
    }
    public void setProgress(long progress) {
        this.progress = progress;
    }
}
