package com.darren.download;

import android.content.Context;
import android.util.Log;

import com.darren.download.db.DefaultDownloadController;
import com.darren.download.db.DownloadDBController;
import com.darren.download.exception.DownloadException;
import com.darren.download.file.FileMd5;
import com.darren.download.log.LogUtils;
import com.darren.download.thread.DownloadThreadListener;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadManagerImpl implements DownloadManager, DownloadTaskListener {

    private static DownloadManager instance;
    private ExecutorService executorService;
    private Context context;
    private DownloadConfig config;
    private ConcurrentHashMap<String, DownloadInfo> downloadInfoList;
    private DownloadDBController downloadDBController;
    private ThreadFactory threadFactory;
    private DownloadListener initListener;
    private ConcurrentHashMap<String, DownloadTaskInterface> downloadTaskMap;
    private boolean isReady = false;

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
            this.config = new DownloadConfig.Builder().build();
        } else {
            this.config = config;
        }
        this.initListener = listener;

        threadFactory = new ThreadFactory() {
            private AtomicInteger atomicInteger = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "DownloadManagerImpl_"+atomicInteger.incrementAndGet());
            }
        };
        executorService = Executors.newFixedThreadPool(Integer.MAX_VALUE, threadFactory);
        downloadTaskMap = new ConcurrentHashMap<>();
        initDbData();
    }

    private void initDbData() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                downloadDBController = new DefaultDownloadController(context);
                downloadInfoList = (ConcurrentHashMap<String, DownloadInfo>) downloadDBController.getAllDownloading();
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

    private boolean prepareDownload(DownloadInfo downloadInfo) {
        LogUtils.logd("DownloadManagerImpl", "downloadInfo "
                + ", " + downloadInfo.getTaskId()
                + ", getStatus: " + downloadInfo.getStatus()
                + ", getProgress: " + downloadInfo.getProgress()
                + ", getUrl: " + downloadInfo.getUrl()
                + ", info getFileMD5: " + downloadInfo.getFileMD5()
                + ", downloadInfo getFileMD5: " + downloadInfo.getFileMD5()
        );

        if (downloadTaskMap.contains(downloadInfo.getTaskId())) {
            return true;
        }

        if (downloadTaskMap.size() >= config.getAllDownloadThreadNum()/config.getEachDownloadThreadNum()) {
            downloadInfo.setStatus(DownloadStatus.STATUS_WAIT);
            downloadInfoList.put(downloadInfo.getTaskId(), downloadInfo);
            return false;
        } else {
            File file = new File(downloadInfo.getSavePath());
            LogUtils.logd(DownloadManagerImpl.class.getSimpleName(), "file parent: " + file.getParent());
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }

            DownloadTaskInterface task = new DownloadTask(executorService, downloadInfo, config, this);
            downloadTaskMap.put(downloadInfo.getTaskId(), task);
            downloadInfo.setStatus(DownloadStatus.STATUS_PREPARE_DOWNLOAD);
            downloadInfoList.put(downloadInfo.getTaskId(), downloadInfo);
            LogUtils.logd("DownloadManagerImpl", "add prepareDownload");
            try {
                int status = task.start();
                if (DownloadStatus.STATUS_DOWNLOADING == status) {
                    downloadInfo.setStatus(DownloadStatus.STATUS_DOWNLOADING);
                    if (null != initListener) {
                        initListener.onStart(downloadInfo);
                    }
                } else if (DownloadStatus.STATUS_ERROR == status) {
                    downloadInfo.setStatus(DownloadStatus.STATUS_ERROR);
                    return false;
                }
            } catch (DownloadException e) {
                downloadInfo.setStatus(DownloadStatus.STATUS_ERROR);
                if (null != initListener) {
                    initListener.onDownloadFailed(downloadInfo, e);
                }
            }

            return true;
        }
    }

    private void removeDownloadTask(String taskId) {
        DownloadTaskInterface task = downloadTaskMap.get(taskId);
        if (null != task) {
            task.stop();
        }
        downloadTaskMap.remove(taskId);
    }

    private void prepareNextTask(boolean resume) {
        Set<String> downloadInfoIdList = downloadInfoList.keySet();
        for (String id : downloadInfoIdList) {
            DownloadInfo downloadInfo = downloadInfoList.get(id);
            if (DownloadStatus.STATUS_WAIT == downloadInfo.getStatus()
                || ((resume) && (DownloadStatus.STATUS_PAUSED == downloadInfo.getStatus()))
            ) {
                prepareDownload(downloadInfo);
            }
        }
    }

    @Override
    public void start(DownloadInfo downloadInfo) {
        LogUtils.logd("DownloadManagerImpl", "start " + downloadInfo.getUrl()
                + ", " + downloadInfo.getProgress()
                + ", " + downloadInfo.getSavePath());
        checkReady();

//        if (!downloadTaskMap.contains(downloadInfo.getTaskId())) {
            LogUtils.logd("DownloadManagerImpl", "start add prepareDownload task" + downloadInfo.getTaskId());
            prepareDownload(downloadInfo);
//        }
    }

    @Override
    public void pause(DownloadInfo downloadInfo) {
        checkReady();

        if (downloadTaskMap.contains(downloadInfo.getTaskId())) {
            removeDownloadTask(downloadInfo.getTaskId());
            if (DownloadStatus.STATUS_COMPLETED != downloadInfo.getStatus()
                && DownloadStatus.STATUS_REMOVE != downloadInfo.getStatus()
            ) {
                downloadInfo.setStatus(DownloadStatus.STATUS_PAUSED);
                if (null != initListener) {
                    initListener.onPaused(downloadInfo);
                }
                downloadDBController.update(downloadInfo);
            }
            prepareNextTask(false);
        }
    }

    @Override
    public void resume(DownloadInfo downloadInfo) {
        checkReady();

        removeDownloadTask(downloadInfo.getTaskId());
        prepareDownload(downloadInfo);
        downloadDBController.update(downloadInfo);
    }

    @Override
    public void remove(DownloadInfo downloadInfo) {
        checkReady();

        downloadInfo.setStatus(DownloadStatus.STATUS_REMOVE);
        removeDownloadTask(downloadInfo.getTaskId());

        downloadDBController.delete(downloadInfo);
        downloadInfoList.remove(downloadInfo.getTaskId());
        File file = new File(downloadInfo.getSavePath());
        if (file.exists()) {
            file.delete();
        }
        if (null != initListener) {
            initListener.onRemoved(downloadInfo);
        }
    }

    private void stopAll() {
        checkReady();

        Iterator entrys = downloadInfoList.entrySet().iterator();
        while (entrys.hasNext()) {
            Map.Entry entry = (Map.Entry) entrys.next();
            DownloadInfo downloadInfo = (DownloadInfo) entry.getValue();
            removeDownloadTask(downloadInfo.getTaskId());
            downloadInfo.setStatus(DownloadStatus.STATUS_PAUSED);
            if (null != initListener) {
                initListener.onPaused(downloadInfo);
            }

            downloadDBController.update(downloadInfo);
        }
    }

    @Override
    public void destroy() {
        this.stopAll();
        executorService.shutdown();
        threadFactory = null;
        downloadDBController.close();
        downloadDBController = null;
        downloadTaskMap.clear();
        downloadDBController = null;
        downloadInfoList.clear();
        downloadInfoList = null;

        initListener = null;
        context = null;
        instance = null;
    }

    @Override
    public void onUpdateProgress(DownloadInfo downloadInfo) {
//        downloadDBController.update(downloadInfo);
        if (null != initListener) {
            initListener.updateDownloadInfo(downloadInfo);
        }
    }

    @Override
    public synchronized void onSuccess(DownloadInfo downloadInfo) {
        downloadInfo.setStatus(DownloadStatus.STATUS_COMPLETED);

        if (null != initListener) {
            initListener.onDownloadSuccess(downloadInfo);
        }

        downloadDBController.delete(downloadInfo);

        removeDownloadTask(downloadInfo.getTaskId());

//        if (null != downloadInfoList && downloadInfoList.containsKey(downloadInfo.getTaskId())) {
//            downloadInfoList.remove(downloadInfo.getTaskId());
//        }
        prepareNextTask(true);
    }

    @Override
    public synchronized void onFailed(DownloadInfo downloadInfo, DownloadException exception) {
        if (null != initListener) {
            initListener.onDownloadFailed(downloadInfo, exception);
        }

        removeDownloadTask(downloadInfo.getTaskId());

        downloadInfo.setStatus(DownloadStatus.STATUS_ERROR);
        downloadDBController.update(downloadInfo);
        prepareNextTask(true);
    }

}
