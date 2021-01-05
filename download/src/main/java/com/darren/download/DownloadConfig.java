package com.darren.download;

import android.os.Build;

public class DownloadConfig {
    private int connectTimeout = 10000;
    private int readTimeout = 10000;
    private int allDownloadThreadNum = 9;
    private int eachDownloadThreadNum = 3;
    private int retryCount = 3;
    private String method = "GET";

    public int getConnectTimeout() {
        return this.connectTimeout;
    }
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return this.readTimeout;
    }
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getAllDownloadThreadNum() {
        return this.allDownloadThreadNum;
    }
    public void setAllDownloadThreadNum(int allDownloadThreadNum) {
        this.allDownloadThreadNum = allDownloadThreadNum;
    }

    public int getEachDownloadThreadNum() {
        return this.eachDownloadThreadNum;
    }
    public void setEachDownloadThreadNum(int eachDownloadThreadNum) {
        if (eachDownloadThreadNum>allDownloadThreadNum) {
            throw new IllegalArgumentException("allDownloadThreadNum must bigger than eachDownloadThreadNum");
        }
        this.eachDownloadThreadNum = eachDownloadThreadNum;
    }

    public int getRetryCount() {
        return this.retryCount;
    }
    public void setRetryCount(int getRetryCount) {
        this.retryCount = getRetryCount;
    }

    public String getMethod() {
        return this.method;
    }
    public void setMethod(String method) {
        this.method = method;
    }

    public static final class Builder {

        private int connectTimeout = -1;
        private int readTimeout = -1;
        private int retryCount = -1;
        private int allDownloadThreadNum = -1;
        private int eachDownloadThreadNum = -1;
        private String method = null;

        public Builder setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder setRetryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder setAllDownloadTreadNum(int allDownloadThreadNum) {
            this.allDownloadThreadNum = allDownloadThreadNum;
            return this;
        }

        public Builder setEachDownloadThreadNum(int eachDownloadThreadNum) {
            if (eachDownloadThreadNum>allDownloadThreadNum) {
                throw new IllegalArgumentException("allDownloadThreadNum must bigger than eachDownloadThreadNum");
            }
            this.eachDownloadThreadNum = eachDownloadThreadNum;
            return this;
        }

        public Builder setMethod(String method) {
            this.method = method;
            return this;
        }

        public DownloadConfig build() {
            DownloadConfig downloadConfig = new DownloadConfig();

            if (-1 != connectTimeout) {
                downloadConfig.setConnectTimeout(connectTimeout);
            }

            if (-1 != readTimeout) {
                downloadConfig.setReadTimeout(readTimeout);
            }

            if (-1 != retryCount) {
                downloadConfig.setRetryCount(retryCount);
            }

            if (-1 != allDownloadThreadNum) {
                downloadConfig.setAllDownloadThreadNum(allDownloadThreadNum);
            }

            if (-1 != eachDownloadThreadNum) {
                downloadConfig.setEachDownloadThreadNum(eachDownloadThreadNum);
            }

            if (null != method) {
                downloadConfig.setMethod(method);
            }

            return downloadConfig;
        }
    }
}
