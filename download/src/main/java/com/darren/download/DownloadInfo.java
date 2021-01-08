package com.darren.download;

import android.os.Environment;
import android.text.TextUtils;

import com.darren.download.exception.DownloadException;
import com.darren.download.log.LogUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DownloadInfo {
    private String packageName;
    private int supportRanges;
    private long createAt;
    private int forceInstall;
    private String url;
    private String savePath;
    private long size;
    private long progress;
    private String fileMD5;
    private int retryCount;
    private volatile int status;
    private long updateTime;

    private Map<String, DownloadThreadInfo> downloadThreadInfoList;

    public DownloadInfo() {
        createAt = System.currentTimeMillis();
        updateTime = createAt;
    }

    public long getUpdateTime() {
        return this.updateTime;
    }
    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public String getTaskId() {
        return this.packageName;
    }
    public void setTaskId(String packageName) {
        this.packageName = packageName;
    }

    public int getSupportRanges() {
        return this.supportRanges;
    }
    public void setSupportRanges(int supportRanges) {
        this.supportRanges = supportRanges;
    }

    public long getCreateAt() {
        return this.createAt;
    }
    public void setCreateAt(long createAt) {
        this.progress = createAt;
    }

    public int getForceInstall() {
        return this.forceInstall;
    }
    public void setForceInstall(int forceInstall) {
        this.forceInstall = forceInstall;
    }

    public String getUrl() {
        return this.url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    public String getSavePath() {
        return this.savePath;
    }
    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public long getSize() {
        return this.size;
    }
    public void setSize(long size) {
        this.size = size;
    }

    public synchronized long getProgress() {
        return this.progress;
    }
    public synchronized void setProgress(long progress) {
        this.progress = progress;
    }

    public String getFileMD5() {
        return this.fileMD5;
    }
    public void setFileMD5(String fileMD5) {
        this.fileMD5 = fileMD5;
    }

    public int getRetryCount() {
        return this.retryCount;
    }
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public synchronized int getStatus() {
        return this.status;
    }
    public synchronized void setStatus(int status) {
        this.status = status;
    }

    private void initDownloadThreadInfoList() {
        if (null == this.downloadThreadInfoList) {
            this.downloadThreadInfoList = new ConcurrentHashMap<>();
        }
    }
    public Map<String, DownloadThreadInfo> getDownloadThreadInfoList() {
        initDownloadThreadInfoList();
        return this.downloadThreadInfoList;
    }
    public void addDownloadThreadInfo(DownloadThreadInfo info) {
        initDownloadThreadInfoList();
        this.downloadThreadInfoList.put(info.getThreadId(), info);
    }

    public boolean isPause() {
        return (DownloadStatus.STATUS_COMPLETED == this.status)
                || (DownloadStatus.STATUS_ERROR == this.status)
                || (DownloadStatus.STATUS_PAUSED == this.status);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if ((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }

        DownloadInfo downloadInfo = (DownloadInfo) obj;
        return this.packageName.equals(downloadInfo.getTaskId());
    }

//    @Override
//    public int hashCode() {
//        return this.taskId;
//    }

    public static final class Builder {
        /*
         * 1为支持分段下载； 0为不能分段下载
         */
        private int supportRanges = 0;
        /*
         * 1为强制安装； 0为不强制安装
         */
        private int forceInstall = 0;
        private String packageName;
        private String url;
        private String savePath;
        private String fileMD5;

        public Builder setSupportRanges(int supportRanges) {
            this.supportRanges = supportRanges;
            return this;
        }

        public Builder setForceInstall(int forceInstall) {
            this.forceInstall = forceInstall;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setSavePath(String savePath) {
            this.savePath = savePath;
            return this;
        }

        public Builder setFileMD5(String fileMD5) {
            this.fileMD5 = fileMD5;
            return this;
        }

        public Builder setPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public DownloadInfo build() {
            if (TextUtils.isEmpty(this.url)) {
                LogUtils.logd("DownloadInfo", "url can't be null");
                throw new DownloadException(DownloadException.CODE_EXCEPTION_URL_NULL, "url can't be null");
            }

            if (TextUtils.isEmpty(this.packageName)) {
                LogUtils.logd("DownloadInfo", "packageName can't be null");
                throw new DownloadException(DownloadException.CODE_EXCEPTION_INIT_FAILED, "packageName can't be null");
            }

            DownloadInfo downloadInfo = new DownloadInfo();

            downloadInfo.setSupportRanges(this.supportRanges);
            downloadInfo.setForceInstall(this.forceInstall);
            downloadInfo.setUrl(this.url);
            downloadInfo.setTaskId(this.packageName);

            if (TextUtils.isEmpty(this.savePath)) {
                downloadInfo.setSavePath(Environment.getDownloadCacheDirectory().getAbsolutePath());
            } else {
                downloadInfo.setSavePath(this.savePath);
            }

            downloadInfo.setFileMD5(this.fileMD5);

            return downloadInfo;
        }

    }


}
