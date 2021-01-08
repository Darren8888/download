package com.darren.download;

import com.darren.download.exception.DownloadException;
import com.darren.download.file.FileInfo;
import com.darren.download.file.FileMd5;
import com.darren.download.file.GetFileInfoTask;
import com.darren.download.log.LogUtils;
import com.darren.download.thread.DownloadThreadListener;
import com.darren.download.thread.DownloadThreadRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class DownloadTask implements DownloadTaskInterface, DownloadThreadListener {
    private ExecutorService executorService;
    private DownloadInfo downloadInfo;
    private DownloadConfig downloadConfig;
    private HashMap<String, Future<Object>> futureList;
    private DownloadTaskListener downloadTaskListener;

    public DownloadTask(ExecutorService executorService, DownloadInfo downloadInfo, DownloadConfig config, DownloadTaskListener taskListener) {
        this.executorService = executorService;
        this.downloadInfo = downloadInfo;
        this.downloadConfig = config;
        this.downloadTaskListener = taskListener;

        this.futureList = new HashMap<>();
    }

    @Override
    public void stop() {
        Iterator entrys = futureList.entrySet().iterator();
        while (entrys.hasNext()) {
            Map.Entry entry = (Map.Entry) entrys.next();
            Future<Object> future = (Future<Object>) entry.getValue();
            if (!future.isDone()
                    && !future.isCancelled()) {
                future.cancel(true);
            }
        }

        futureList.clear();
    }

    @Override
    public int start() throws DownloadException {
        if (0>=downloadInfo.getSize()) {
            try {
                GetFileInfoTask task = new GetFileInfoTask(downloadInfo);
                Future<FileInfo> future = executorService.submit(task);
                FileInfo fileInfo = future.get();
                if (null != fileInfo) {
                    downloadInfo.setSupportRanges(fileInfo.getAcceptRanges() ? 1 : 0);
                    downloadInfo.setSize(fileInfo.getLength());

                    LogUtils.logd(DownloadTask.class.getSimpleName(), "url: " + downloadInfo.getUrl() + ", supportRanges: " + fileInfo.getAcceptRanges() + ", size: " + fileInfo.getLength());
                } else {
                    if (null != downloadTaskListener) {
                        downloadTaskListener.onFailed(downloadInfo, new DownloadException(DownloadException.CODE_EXCEPTION_FILE_NULL, "file error"));
                    }
                    return DownloadStatus.STATUS_ERROR;
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
                if (null != downloadTaskListener) {
                    downloadTaskListener.onFailed(downloadInfo, new DownloadException(DownloadException.CODE_EXCEPTION_IO_ERR, e.getMessage()));
                }
                return DownloadStatus.STATUS_ERROR;
            } catch (InterruptedException e) {
                e.printStackTrace();
                if (null != downloadTaskListener) {
                    downloadTaskListener.onFailed(downloadInfo, new DownloadException(DownloadException.CODE_EXCEPTION_IO_ERR, e.getMessage()));
                }
                return DownloadStatus.STATUS_ERROR;
            }
        }

        if (hasFileDownload(downloadInfo)) {
            return DownloadStatus.STATUS_COMPLETED;
        }

        if (0>=downloadInfo.getSize()) {
            if (null != downloadTaskListener) {
                downloadTaskListener.onFailed(downloadInfo, new DownloadException(DownloadException.CODE_EXCEPTION_FILE_NULL, "file(" + downloadInfo.getUrl() + ") is null"));
            }
            throw new DownloadException(DownloadException.CODE_EXCEPTION_FILE_NULL, "file(" + downloadInfo.getUrl() + ") is null");
        } else {
            download();
            return DownloadStatus.STATUS_DOWNLOADING;
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

            String threadInfoId = downloadInfo.getSavePath()+"_"+(i+1);
            long progress = 0;
            if (0<downloadInfo.getDownloadThreadInfoList().size()
                && downloadInfo.getDownloadThreadInfoList().containsKey(threadInfoId)) {
                DownloadThreadInfo threadInfo = downloadInfo.getDownloadThreadInfoList().get(threadInfoId);
                progress = threadInfo.getProgress();
                if (null != futureList.get(threadInfoId)) {
                    futureList.get(threadInfoId).cancel(true);
                    futureList.remove(threadInfoId);
                }
            }

            LogUtils.logd(DownloadTask.class.getSimpleName(), "i: " + i + ", start: " + start + ", end: " + end + ", progress: " + progress);

            DownloadThreadInfo threadInfo = new DownloadThreadInfo(downloadInfo.getTaskId(), threadInfoId, downloadInfo.getUrl(), start, end);
            threadInfo.setProgress(progress);
            downloadInfo.addDownloadThreadInfo(threadInfo);
            DownloadThreadRunnable runnable = new DownloadThreadRunnable(downloadInfo, threadInfo, downloadConfig, this);
            futureList.put(threadInfoId, executorService.submit(runnable));
        }
    }

    private boolean hasFileDownload(DownloadInfo downloadInfo) {
        File file1 = new File(downloadInfo.getSavePath());
        if (file1.exists()
            && (FileMd5.getFileMD5(file1).equals(downloadInfo.getFileMD5()))
        ) {
            if (null != downloadTaskListener) {
                downloadTaskListener.onSuccess(downloadInfo);
            }
            return true;
        } else {
            return false;
        }
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
        long oldProgress = downloadInfo.getProgress();
        downloadInfo.setProgress(progressA);
        if (((progressA == downloadInfo.getSize()) || (2.0 <= (progressA-oldProgress)/downloadInfo.getSize())) && (null != downloadTaskListener)) {
            downloadTaskListener.onUpdateProgress(downloadInfo);
        }
//        LogUtils.logd(DownloadTask.class.getSimpleName(), "onProgress progress: " + progressA + ", length: " + downloadInfo.getSize());
    }


    @Override
    public void onDownloadSuccess(String threadId) {
        if (downloadInfo.getProgress() == downloadInfo.getSize()) {
            if (null != downloadTaskListener) {
                downloadTaskListener.onSuccess(downloadInfo);
            }
        }
    }
}
