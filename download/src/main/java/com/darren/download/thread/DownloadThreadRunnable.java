package com.darren.download.thread;

import android.util.Log;

import com.darren.download.DownloadConfig;
import com.darren.download.DownloadInfo;
import com.darren.download.DownloadThreadInfo;
import com.darren.download.log.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class DownloadThreadRunnable extends ThreadTask<Object> {
    private final DownloadThreadListener listener;
    private final DownloadConfig config;
    private final DownloadInfo downloadInfo;
    private final DownloadThreadInfo downloadThreadInfo;

    public DownloadThreadRunnable(DownloadInfo downloadInfo, DownloadThreadInfo downloadThreadInfo, DownloadConfig config, DownloadThreadListener listener) {
        this.downloadInfo = downloadInfo;
        this.downloadThreadInfo = downloadThreadInfo;
        this.config = config;
        this.listener = listener;
    }

    @Override
    protected Object execute() {
        runDownload();
        return new Object();
    }

    private void runDownload() {
        HttpURLConnection httpURLConnection = null;
        try {
            final URL url = new URL(downloadInfo.getUrl());
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(config.getConnectTimeout());
            httpURLConnection.setReadTimeout(config.getReadTimeout());
            httpURLConnection.setRequestMethod(config.getMethod());

            LogUtils.logd(DownloadThreadRunnable.class.getSimpleName(), "runDownload getStart(): " + downloadThreadInfo.getStart()
                + ", getProgress(): " + downloadThreadInfo.getProgress()
            );

            long lastStart = downloadThreadInfo.getStart()+downloadThreadInfo.getProgress();
            if (0 != downloadInfo.getSupportRanges()) {
                httpURLConnection.setRequestProperty("Range", "bytes=" + lastStart + "-" + downloadThreadInfo.getEnd());
            }

            final int responseCode = httpURLConnection.getResponseCode();

            LogUtils.logd(DownloadThreadRunnable.class.getSimpleName(), "DownloadThreadRunnable responseCode: " + responseCode);

            if (HttpURLConnection.HTTP_PARTIAL == responseCode
                || HttpURLConnection.HTTP_OK == responseCode
            ) {
                InputStream inputStream = httpURLConnection.getInputStream();
                RandomAccessFile file = new RandomAccessFile(downloadInfo.getSavePath(), "rwd");
                file.seek(lastStart);

                final byte[] buffer = new byte[4*1024];
                int length = -1, offset = 0;
                while (true) {
                    hasPause();
                    length = inputStream.read(buffer);
                    if (-1 == length) {
                        break;
                    }
                    file.write(buffer, 0, length);
                    offset += length;
                    downloadThreadInfo.setProgress(lastStart+offset-downloadThreadInfo.getStart());

                    synchronized (listener) {
                        listener.onProgress(downloadThreadInfo.getThreadId(), downloadThreadInfo.getProgress());
                    }
                }

                inputStream.close();
                file.close();
                listener.onDownloadSuccess(downloadThreadInfo.getThreadId());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != httpURLConnection) {
                httpURLConnection.disconnect();
            }
        }
    }

    private void hasPause() {
//        if (downloadInfo.isPause()) {
//            throw new DownloadException(DownloadException.CODE_EXCEPTION_PAUSE, "thread: " + downloadThreadInfo.getThreadId() + " has paused");
//        }
    }
}
