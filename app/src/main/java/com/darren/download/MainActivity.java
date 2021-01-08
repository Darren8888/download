package com.darren.download;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import com.darren.download.exception.DownloadException;
import com.darren.download.log.LogUtils;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
    };

    private DownloadManager mDownloadManager;

    private DownloadListener downloadListener = new DownloadListener() {
        @Override
        public void onReady() {
            testDownload();
        }

        @Override
        public void onStart(DownloadInfo downloadInfo) {
//            LogUtils.logd(TAG, "onStart");
        }

        @Override
        public void onPaused(DownloadInfo downloadInfo) {
//            LogUtils.logd(TAG, "onPaused");
        }

        @Override
        public void updateDownloadInfo(DownloadInfo downloadInfo) {
//            LogUtils.logd(TAG, "size: " + downloadInfo.getSize() + ", progress: " + downloadInfo.getProgress());
        }

        @Override
        public void onRemoved(DownloadInfo downloadInfo) {
            LogUtils.logd(TAG, "onRemoved");
        }

        @Override
        public void onDownloadSuccess(DownloadInfo downloadInfo) {
//            LogUtils.logd(TAG, "onDownloadSuccess " + downloadInfo.getUrl() + ", " + downloadInfo.getSavePath());
        }

        @Override
        public void onDownloadFailed(DownloadInfo downloadInfo, DownloadException e) {
//            LogUtils.logd(TAG, "onDownloadFailed " + e);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mDownloadManager.destroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (verifyStoragePermissions(this)) {
            inifDownloadManager();
        }
    }

    public static boolean verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            return false;
        }

        permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            return false;
        }

        permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.INTERNET);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if( REQUEST_EXTERNAL_STORAGE == requestCode) {
            for(int i=0; i<grantResults.length; i++){
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {//没有授权
                    Toast.makeText(this, "动态获取失败", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }

        inifDownloadManager();
    }

    private void inifDownloadManager() {
        if (null == mDownloadManager) {
            mDownloadManager = DownloadManagerImpl.getInstance();
            mDownloadManager.init(this, null, downloadListener);
        }
    }

    private void testDownload() {
        final String path = this.getExternalFilesDir(null).getAbsolutePath();
        DownloadInfo downloadInfo = new DownloadInfo.Builder()
                .setForceInstall(0)
                .setSupportRanges(1)
                .setPackageName("com.lemonread.student")
                .setSavePath(path+ File.separator+"test.apk")
                .setFileMD5("c9e2af9d16eb6af6c7862a7670400b23")
                .setUrl("http://apks.lemonread.com/student-guanwang-release-v242-build50-20181208-2016121544403285065.apk")
//                .setUrl("http://........")
                .build();

        LogUtils.logd("MainActivity", "start download");

        mDownloadManager.start(downloadInfo);
    }

}