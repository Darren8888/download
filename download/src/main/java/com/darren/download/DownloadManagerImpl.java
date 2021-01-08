package com.darren.download;

import android.content.Context;

import com.darren.download.db.DefaultDownloadController;
import com.darren.download.db.DownloadDBController;
import com.darren.download.exception.DownloadException;
import com.darren.download.log.LogUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadManagerImpl implements DownloadManager, DownloadConsumer.Callback {

    private static DownloadManager instance;
    private ExecutorService executorService;
    private Context context;
    private DownloadConfig downloadConfig;
    private DownloadDBController downloadDBController;
    private ThreadFactory threadFactory;
    private DownloadListener initListener;
    private boolean isReady = false;
    private DownloadConsumerInterface downloadConsumer;

    private LinkedBlockingQueue<DownloadInfo> blockingQueue;

    private DownloadManagerImpl() {

    }

    public static DownloadManager getInstance() {
        if (null == instance) {
            synchronized (DownloadManagerImpl.class) {
                if (null == instance) {
                    instance = new DownloadManagerImpl();
                }
            }
        }

        return instance;
    }

    @Override
    public void init(Context context, DownloadConfig config, DownloadListener listener) {
        this.context = context;
        if (null == config) {
            this.downloadConfig = new DownloadConfig.Builder().build();
        } else {
            this.downloadConfig = config;
        }
        this.initListener = listener;

        threadFactory = new ThreadFactory() {
            private AtomicInteger atomicInteger = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "DownloadManagerImpl_"+atomicInteger.incrementAndGet());
            }
        };

        this.blockingQueue = new LinkedBlockingQueue<>(50);

//        executorService = Executors.newCachedThreadPool(threadFactory);
        executorService = Executors.newFixedThreadPool(Integer.MAX_VALUE, threadFactory);

        downloadConsumer = new DownloadConsumer(executorService, downloadConfig, blockingQueue, this);

        initDbData();
    }

    private void initDbData() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                downloadDBController = new DefaultDownloadController(context);
                List<DownloadInfo> downloadInfoList = downloadDBController.getAllDownloading();

                LogUtils.logd("DownloadManagerImpl", "initDbData size: " + downloadInfoList.size());
                for (DownloadInfo downloadInfo : downloadInfoList) {
                    LogUtils.logd("DownloadManagerImpl", "initDbData "
                        + ", url: " + downloadInfo.getUrl()
                        + ", status: " + downloadInfo.getStatus()
                        + ", size: " + downloadInfo.getSize()
                        + ", progress: " + downloadInfo.getProgress()
                    );

                    if (DownloadStatus.STATUS_COMPLETED != downloadInfo.getStatus()) {
                        downloadConsumer.add(downloadInfo);
                    }
                }

                if (null != initListener) {
                    isReady = true;
                    initListener.onReady();
                }
            }
        });
    }

    private void checkReady() {
        if (!isReady) {
            throw new DownloadException(DownloadException.CODE_EXCEPTION_INIT_FAILED, "DownloadManager is not ready");
        }
    }

    @Override
    public void start(DownloadInfo downloadInfo) {
        checkReady();

        downloadInfo.setStatus(DownloadStatus.STATUS_NONE);
        downloadConsumer.add(downloadInfo);
    }

    @Override
    public void pause(DownloadInfo downloadInfo) {
        checkReady();
        downloadConsumer.pauseDownloadTask(downloadInfo);
    }

    @Override
    public void resume(DownloadInfo downloadInfo) {
        checkReady();

        downloadConsumer.resumeDownloadTask(downloadInfo);
    }

    @Override
    public void remove(DownloadInfo downloadInfo) {
        checkReady();

        downloadInfo.setStatus(DownloadStatus.STATUS_REMOVE);
        downloadConsumer.pauseDownloadTask(downloadInfo);

        LogUtils.logd("DownloadManagerImpl", "remove delete: " + downloadInfo.getSavePath());

        File file = new File(downloadInfo.getSavePath());
        if (file.exists()) {
            file.delete();
        }
        downloadDBController.delete(downloadInfo);

        if (null != initListener) {
            initListener.onRemoved(downloadInfo);
        }
    }

    private void stopAll() {
        LogUtils.logd("DownloadManagerImpl", "stopAll");
        checkReady();
        downloadConsumer.stop();
    }

    @Override
    public void destroy() {
        this.stopAll();

        executorService.shutdown();
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }

        threadFactory = null;
        downloadDBController.close();
        downloadDBController = null;

        initListener = null;
        context = null;
        instance = null;
    }

    @Override
    public void onStart(DownloadInfo downloadInfo) {
        if (null != initListener) {
            initListener.onStart(downloadInfo);
        }
    }

    @Override
    public void onStop(DownloadInfo downloadInfo) {

        LogUtils.logd("DownloadManagerImpl", "onStop url: " + downloadInfo.getUrl()
                + ", size: " + downloadInfo.getSize()
                + ", progress: " + downloadInfo.getProgress()
                + ", status: " + downloadInfo.getStatus()
        );

        downloadDBController.update(downloadInfo);

        if (null != initListener) {
            initListener.onPaused(downloadInfo);
        }
    }

    @Override
    public void updateProgress(DownloadInfo downloadInfo) {
        if (null != initListener) {
            initListener.updateDownloadInfo(downloadInfo);
        }
    }

    @Override
    public void onSuccess(DownloadInfo downloadInfo) {
        if (null != initListener) {
            initListener.onDownloadSuccess(downloadInfo);
        }

        downloadDBController.update(downloadInfo);
    }

    @Override
    public void onFailed(DownloadInfo downloadInfo, DownloadException exception) {
        if (null != initListener) {
            initListener.onDownloadFailed(downloadInfo, exception);
        }
        downloadDBController.update(downloadInfo);
    }
}
