package com.darren.download.file;

import android.text.TextUtils;

import com.darren.download.DownloadConfig;
import com.darren.download.DownloadInfo;
import com.darren.download.exception.DownloadException;
import com.darren.download.thread.ThreadTask;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.Callable;

public class GetFileInfoTask extends ThreadTask<FileInfo> {

    private DownloadInfo downloadInfo;
    private DownloadConfig downloadConfig;

    public GetFileInfoTask(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
        this.downloadConfig = (new DownloadConfig.Builder()).build();
    }

    @Override
    protected FileInfo execute() {
        HttpURLConnection httpURLConnection = null;
        final URL url;
        FileInfo fileInfo = null;

        try {
            url = new URL(downloadInfo.getUrl());
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(downloadConfig.getConnectTimeout());
            httpURLConnection.setReadTimeout(downloadConfig.getReadTimeout());
            httpURLConnection.setRequestMethod(downloadConfig.getMethod());
            httpURLConnection.setRequestProperty("Accept-Encoding", "identity");
            httpURLConnection.setRequestProperty("Range", "bytes=" + 0 + "-");
            final int responseCode = httpURLConnection.getResponseCode();
            if (HttpURLConnection.HTTP_OK == responseCode) {
                fileInfo = parseHttpResponse(httpURLConnection, false);
            } else if (HttpURLConnection.HTTP_PARTIAL == responseCode) {
                fileInfo = parseHttpResponse(httpURLConnection, true);
            } else {
                throw  new DownloadException("unsupported response code: " + responseCode);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return fileInfo;
        }
    }

    private FileInfo parseHttpResponse(HttpURLConnection httpURLConnection, boolean isAcceptRanges) {
        final long length;
        String contentLength = httpURLConnection.getHeaderField("Content-Length");
        if (TextUtils.isEmpty(contentLength) || contentLength.equals("0") || contentLength.equals("-1")) {
           length = httpURLConnection.getContentLength();
        } else {
            length = Long.parseLong(contentLength);
        }

        if (0 >= length) {
            throw new DownloadException("file length <= 0 exception");
        }

        return new FileInfo(length, isAcceptRanges);
    }
}
