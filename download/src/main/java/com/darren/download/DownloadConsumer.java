package com.darren.download;

import com.darren.download.exception.DownloadException;
import com.darren.download.log.LogUtils;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class DownloadConsumer implements DownloadTaskListener, DownloadConsumerInterface {

    public interface Callback {
        void onStart(DownloadInfo downloadInfo);
        void onStop(DownloadInfo downloadInfo);
        void updateProgress(DownloadInfo downloadInfo);
        void onSuccess(DownloadInfo downloadInfo);
        void onFailed(DownloadInfo downloadInfo, DownloadException exception);
    }

    private LinkedBlockingQueue<DownloadInfo> blockingQueue;
    private ExecutorService executorService;
    private DownloadConfig config;

    private ConcurrentHashMap<String, DownloadTaskInterface> downloadTaskMap;
    private Future<?> future;
    private Callback callback;

    public DownloadConsumer(ExecutorService executorService, DownloadConfig config, LinkedBlockingQueue queue, Callback callback) {
        this.config = config;
        this.executorService = executorService;
        this.blockingQueue = queue;
        this.downloadTaskMap = new ConcurrentHashMap<>();
        this.callback = callback;

        this.run();
    }

    private void run() {
        if (null != executorService) {
            future = executorService.submit(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            deal(blockingQueue.take());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    private void deal(DownloadInfo downloadInfo) {
        if (downloadTaskMap.containsKey(downloadInfo.getTaskId())
            || DownloadStatus.STATUS_COMPLETED == downloadInfo.getStatus()
            || DownloadStatus.STATUS_REMOVE == downloadInfo.getStatus()
        ) {
            return;
        }

        DownloadTaskInterface task = new DownloadTask(executorService, downloadInfo, config, this);
        downloadTaskMap.put(downloadInfo.getTaskId(), task);
        downloadInfo.setStatus(DownloadStatus.STATUS_PREPARE_DOWNLOAD);

        try {
            int status = task.start();

            if (DownloadStatus.STATUS_DOWNLOADING == status) {
                downloadInfo.setStatus(DownloadStatus.STATUS_DOWNLOADING);
                if (null != callback) {
                    callback.onStart(downloadInfo);
                }
            } else if (DownloadStatus.STATUS_ERROR == status) {
                downloadInfo.setStatus(DownloadStatus.STATUS_ERROR);
            }
        } catch (DownloadException e) {
            downloadInfo.setStatus(DownloadStatus.STATUS_ERROR);
        }
    }

    @Override
    public void add(DownloadInfo downloadInfo) {
        if (!blockingQueue.contains(downloadInfo) && !downloadTaskMap.containsKey(downloadInfo.getTaskId())) {
            blockingQueue.offer(downloadInfo);
        }
    }

    @Override
    public void addList(List<DownloadInfo> downloadInfos) {
        for (DownloadInfo downloadInfo : downloadInfos) {
            add(downloadInfo);
        }
    }

    @Override
    public void pauseDownloadTask(DownloadInfo downloadInfo) {
        if (blockingQueue.contains(downloadInfo)) {
            blockingQueue.remove(downloadInfo);
        } else {
            removeDownloadTask(downloadInfo);
        }
        downloadInfo.setStatus(DownloadStatus.STATUS_PAUSED);
    }

    @Override
    public void resumeDownloadTask(DownloadInfo downloadInfo) {
        if (!blockingQueue.contains(downloadInfo) && !downloadTaskMap.containsKey(downloadInfo.getTaskId())) {
            blockingQueue.add(downloadInfo);
            downloadInfo.setStatus(DownloadStatus.STATUS_NONE);
        }
    }

    @Override
    public void stop() {
        for (DownloadTaskInterface taskInterface : downloadTaskMap.values()) {
            taskInterface.stop();
            DownloadInfo info = taskInterface.getDownloadInfo();
            if (null != callback) {
                callback.onStop(info);
            }
        }

        if (!future.isDone() && !future.isCancelled()) {
            future.cancel(true);
        }

        for (DownloadInfo info : blockingQueue) {
            info.setStatus(DownloadStatus.STATUS_PAUSED);
            if (null != callback) {
                callback.onStop(info);
            }
        }
    }

    private void removeDownloadTask(DownloadInfo downloadInfo) {
        if (downloadTaskMap.containsKey(downloadInfo.getTaskId())) {
            DownloadTaskInterface task = downloadTaskMap.get(downloadInfo.getTaskId());
            if (null != task) {
                task.stop();
            }
            downloadTaskMap.remove(downloadInfo.getTaskId());
        }
    }

    @Override
    public synchronized void onUpdateProgress(DownloadInfo downloadInfo) {
        if (null != callback) {
            callback.updateProgress(downloadInfo);
        }
    }

    @Override
    public synchronized void onSuccess(DownloadInfo downloadInfo) {
        downloadInfo.setStatus(DownloadStatus.STATUS_COMPLETED);
        removeDownloadTask(downloadInfo);
        if (null != callback) {
            callback.onSuccess(downloadInfo);
        }
    }

    @Override
    public synchronized void onFailed(DownloadInfo downloadInfo, DownloadException exception) {
        downloadInfo.setStatus(DownloadStatus.STATUS_ERROR);
        removeDownloadTask(downloadInfo);
        if (null != callback) {
            callback.onFailed(downloadInfo, exception);
        }
    }
}
