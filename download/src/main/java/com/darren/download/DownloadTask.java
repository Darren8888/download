package com.darren.download;

import com.darren.download.exception.DownloadException;
import com.darren.download.file.FileInfo;
import com.darren.download.file.GetFileInfoTask;
import com.darren.download.log.LogUtils;
import com.darren.download.thread.DownloadThreadListener;
import com.darren.download.thread.DownloadThreadRunnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class DownloadTask implements DownloadThreadListener {

    private ExecutorService executorService;
    private DownloadInfo downloadInfo;
    private DownloadConfig downloadConfig;
    private List<Future<Object>> futureList;
    private DownloadListener downloadListener;
    private DownloadTaskListener downloadTaskListener;

    public DownloadTask(ExecutorService executorService, DownloadInfo downloadInfo, DownloadConfig config, DownloadListener listener, DownloadTaskListener taskListener) {
        this.executorService = executorService;
        this.downloadInfo = downloadInfo;
        this.downloadConfig = config;
        this.downloadListener = listener;
        this.downloadTaskListener = taskListener;

        this.futureList = new ArrayList<>();
    }

    public void stop() {
        for (int i=0; i<futureList.size(); ++i) {
            if (!futureList.get(i).isDone()
                && !futureList.get(i).isCancelled()) {
                futureList.get(i).cancel(true);
            }
        }
    }

    public void start() {
        if (0>=downloadInfo.getSize()) {
            try {
                GetFileInfoTask task = new GetFileInfoTask(downloadInfo);
                Future<FileInfo> future = executorService.submit(task);
                FileInfo fileInfo = future.get();
                downloadInfo.setSupportRanges(fileInfo.getAcceptRanges()?1:0);
                downloadInfo.setSize(fileInfo.getLength());

                LogUtils.logd(DownloadTask.class.getSimpleName(), "url: " + downloadInfo.getUrl() + ", supportRanges: " + fileInfo.getAcceptRanges() + ", size: " + fileInfo.getLength());
            } catch (ExecutionException e) {
                e.printStackTrace();
                if (null != downloadTaskListener) {
                    downloadTaskListener.onFailed(downloadInfo, new DownloadException(DownloadException.CODE_EXCEPTION_IO_ERR, e.getMessage()));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                if (null != downloadTaskListener) {
                    downloadTaskListener.onFailed(downloadInfo, new DownloadException(DownloadException.CODE_EXCEPTION_IO_ERR, e.getMessage()));
                }
            }
        }

        if (0>=downloadInfo.getSize()) {
            if (null != downloadTaskListener) {
                downloadTaskListener.onFailed(downloadInfo, new DownloadException(DownloadException.CODE_EXCEPTION_FILE_NULL, "file(" + downloadInfo.getUrl() + ") is null"));
            }
            throw new DownloadException(DownloadException.CODE_EXCEPTION_FILE_NULL, "file(" + downloadInfo.getUrl() + ") is null");
        } else {
            download();
        }
    }

    private void download() {
        long length = downloadInfo.getSize();
        long start = 0, end=0;
        final int threads;
        final long average;
        if (1 == downloadInfo.getSupportRanges()) {
            threads = downloadConfig.getEachDownloadThreadNum();
            average = length/threads;
        } else {
            threads = 1;
            average = length+1;
        }

        for (int i=0; i<threads; ++i) {
            start = average*i;
            if (i == (threads-1)) {
                end = length;
            } else {
                end = start + average - 1;
            }

            LogUtils.logd(DownloadTask.class.getSimpleName(), "i: " + i + ", start: " + start + ", end: " + end);

            DownloadThreadInfo threadInfo = new DownloadThreadInfo(downloadInfo.getTaskId(), downloadInfo.getSavePath()+"_"+(i+1), downloadInfo.getUrl(), start, end);
            downloadInfo.addDownloadThreadInfo(threadInfo);
            DownloadThreadRunnable runnable = new DownloadThreadRunnable(downloadInfo, threadInfo, downloadConfig, this);
            futureList.add(executorService.submit(runnable));
        }

        downloadInfo.setStatus(DownloadStatus.STATUS_DOWNLOADING);
    }

    @Override
    public void onProgress(String threadId, long progress) {
        long progressA = 0;
        Iterator entrys = downloadInfo.getDownloadThreadInfoList().entrySet().iterator();
        while (entrys.hasNext()) {
            Map.Entry entry = (Map.Entry) entrys.next();
            DownloadThreadInfo threadInfo = (DownloadThreadInfo) entry.getValue();
            progressA += threadInfo.getProgress();
        }
        downloadInfo.setProgress(progressA);
        if (null != downloadListener) {
            downloadListener.updateDownloadInfo(downloadInfo);
        }
        LogUtils.logd(DownloadTask.class.getSimpleName(), "onProgress progress: " + progressA + ", length: " + downloadInfo.getSize());
    }


    @Override
    public void onDownloadSuccess(String threadId) {
        if (downloadInfo.getProgress() == downloadInfo.getSize()) {
            downloadInfo.setStatus(DownloadStatus.STATUS_COMPLETED);
            if (null != downloadTaskListener) {
                downloadTaskListener.onSuccess(downloadInfo);
            }
        }
    }
}
