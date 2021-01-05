package com.darren.download;

import android.content.Context;
import android.util.Log;

import com.darren.download.db.DefaultDownloadController;
import com.darren.download.db.DownloadDBController;
import com.darren.download.exception.DownloadException;
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
    private ConcurrentHashMap<Integer, DownloadInfo> downloadInfoList;
    private DownloadDBController downloadDBController;
    private ThreadFactory threadFactory;
    private InitListener initListener;
    private DownloadListener downloadListener;
    private ConcurrentHashMap<Integer, DownloadTask> downloadTaskMap;
    private boolean isReady = false;

    public interface InitListener {
        void onReady();
    }

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
    public void init(Context context, DownloadConfig config, DownloadListener downloadListener, InitListener listener) {
        this.context = context;
        if (null == config) {
            this.config = new DownloadConfig.Builder().build();
        } else {
            this.config = config;
        }
        this.initListener = listener;
        this.downloadListener = downloadListener;

        threadFactory = new ThreadFactory() {
            private AtomicInteger atomicInteger = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "DownloadManagerImpl_"+atomicInteger.incrementAndGet());
            }
        };
        executorService = Executors.newFixedThreadPool(this.config.getAllDownloadThreadNum(), threadFactory);
        downloadTaskMap = new ConcurrentHashMap<>();
        initDbData();
    }

    private void initDbData() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                downloadDBController = new DefaultDownloadController(context);
                downloadInfoList = (ConcurrentHashMap<Integer, DownloadInfo>) downloadDBController.getAllDownloading();

                //TODO start download
                prepareNextTask(true);

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

            DownloadTask task = new DownloadTask(executorService, downloadInfo, config, downloadListener, this);
            downloadTaskMap.put(downloadInfo.getTaskId(), task);
            downloadInfo.setStatus(DownloadStatus.STATUS_PREPARE_DOWNLOAD);
            downloadInfoList.put(downloadInfo.getTaskId(), downloadInfo);
            task.start();
            if (null != downloadListener) {
                downloadListener.onStart(downloadInfo);
            }
            return true;
        }
    }

    private void removeDownloadTask(int taskId) {
        DownloadTask task = downloadTaskMap.get(taskId);
        if (null != task) {
            task.stop();
        }
        downloadTaskMap.remove(taskId);
    }

    private void prepareNextTask(boolean resume) {
        Set<Integer> downloadInfoIdList = downloadInfoList.keySet();
        for (Integer id : downloadInfoIdList) {
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
        checkReady();
        prepareDownload(downloadInfo);
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
                if (null != downloadListener) {
                    downloadListener.onPaused(downloadInfo);
                }
            }
            prepareNextTask(false);
        }
    }

    @Override
    public void resume(DownloadInfo downloadInfo) {
        checkReady();

        removeDownloadTask(downloadInfo.getTaskId());
        prepareDownload(downloadInfo);
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
        if (null != downloadListener) {
            downloadListener.onRemoved(downloadInfo);
        }
    }

    private void stopAll() {
        checkReady();

        Iterator entrys = downloadTaskMap.entrySet().iterator();
        while (entrys.hasNext()) {
            Map.Entry entry = (Map.Entry) entrys.next();
            DownloadInfo downloadInfo = (DownloadInfo) entry.getValue();
            removeDownloadTask(downloadInfo.getTaskId());
            downloadInfo.setStatus(DownloadStatus.STATUS_PAUSED);
            if (null != downloadListener) {
                downloadListener.onPaused(downloadInfo);
            }
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
    public void onSuccess(DownloadInfo downloadInfo) {
        if (null != downloadListener) {
            downloadListener.onDownloadSuccess(downloadInfo);
        }

        if (null != downloadTaskMap && downloadTaskMap.contains(downloadInfo.getTaskId())) {
            downloadTaskMap.remove(downloadInfo);
        }

        if (null != downloadInfoList && downloadInfoList.containsKey(downloadInfo.getTaskId())) {
            downloadInfoList.remove(downloadInfo.getTaskId());
        }
        prepareNextTask(true);
    }

    @Override
    public void onFailed(DownloadInfo downloadInfo, DownloadException exception) {
        if (null != downloadListener) {
            downloadListener.onDownloadFailed(downloadInfo, exception);
        }
        downloadInfo.setStatus(DownloadStatus.STATUS_ERROR);
        prepareNextTask(true);
    }

}
