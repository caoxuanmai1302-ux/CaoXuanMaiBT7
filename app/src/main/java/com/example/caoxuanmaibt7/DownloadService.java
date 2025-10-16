package com.example.caoxuanmaibt7;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadService extends Service {
    public static final String ACTION_START  = "ACTION_START";
    public static final String ACTION_PAUSE  = "ACTION_PAUSE";
    public static final String ACTION_RESUME = "ACTION_RESUME";
    public static final String ACTION_CANCEL = "ACTION_CANCEL";

    public static final String EXTRA_URL = "EXTRA_URL";
    public static final String EXTRA_FILE_NAME = "EXTRA_FILE_NAME";

    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean isCanceled = new AtomicBoolean(false);

    private String urlStr;
    private String fileName;
    private File outFile;
    private long downloadedBytes = 0;
    private long totalBytes = -1;

    private Thread worker;

    @Nullable @Override public IBinder onBind(Intent intent) { return new Binder(); }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) return START_NOT_STICKY;
        String action = intent.getAction();

        switch (action) {
            case ACTION_START:
                urlStr = intent.getStringExtra(EXTRA_URL);
                fileName = intent.getStringExtra(EXTRA_FILE_NAME);
                isPaused.set(false);
                isCanceled.set(false);
                startForegroundFlow();
                startDownload(false);
                break;

            case ACTION_RESUME:
                isPaused.set(false);
                startDownload(true);
                break;

            case ACTION_PAUSE:
                isPaused.set(true);
                break;

            case ACTION_CANCEL:
                isCanceled.set(true);
                stopSelf();
                break;
        }
        return START_NOT_STICKY;
    }

    private void startForegroundFlow() {
        NotificationUtils.createChannel(this);
        Notification n = NotificationUtils.build(this, "Đang tải: " + fileName, 0, true);
        startForeground(NotificationUtils.NOTIF_ID, n); // gọi sớm (≤5s)
    }

    private void updateNotification(int progress, boolean indeterminate) {
        Notification n = NotificationUtils.build(this, "Đang tải: " + fileName, progress, indeterminate);
        startForeground(NotificationUtils.NOTIF_ID, n); // cập nhật ổn định trên nhiều API
    }

    private void startDownload(boolean resume) {
        if (worker != null && worker.isAlive()) return;

        worker = new Thread(() -> {
            try {
                // Thư mục internal storage: .../files/downloads
                if (outFile == null) {
                    File dir = new File(getFilesDir(), "downloads");
                    if (!dir.exists()) dir.mkdirs();
                    outFile = new File(dir, fileName);
                }
                if (!resume) downloadedBytes = 0;

                RandomAccessFile raf = new RandomAccessFile(outFile, "rw");
                if (resume) {
                    downloadedBytes = outFile.length();
                    raf.seek(downloadedBytes);
                }

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                if (downloadedBytes > 0) {
                    conn.setRequestProperty("Range", "bytes=" + downloadedBytes + "-");
                }
                conn.connect();

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_PARTIAL) {
                    long contentLen = conn.getContentLengthLong();
                    if (code == HttpURLConnection.HTTP_OK) {
                        totalBytes = contentLen;
                    } else if (code == HttpURLConnection.HTTP_PARTIAL) {
                        String cr = conn.getHeaderField("Content-Range"); // "bytes X-Y/TOTAL"
                        long total = -1;
                        if (cr != null && cr.contains("/")) {
                            try { total = Long.parseLong(cr.substring(cr.lastIndexOf('/') + 1)); }
                            catch (Exception ignored) {}
                        }
                        totalBytes = (total > 0) ? total : downloadedBytes + contentLen;
                    }

                    InputStream in = new BufferedInputStream(conn.getInputStream());
                    byte[] buf = new byte[8192];
                    long lastUi = System.currentTimeMillis();
                    int len;

                    while ((len = in.read(buf)) != -1) {
                        if (isCanceled.get()) {
                            in.close();
                            raf.close();
                            outFile.delete();
                            stopSelf();
                            return;
                        }
                        while (isPaused.get()) {
                            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                        }
                        raf.write(buf, 0, len);
                        downloadedBytes += len;

                        long now = System.currentTimeMillis();
                        if (now - lastUi > 500) {
                            int progress = (totalBytes > 0) ? (int)((downloadedBytes * 100) / totalBytes) : 0;
                            updateNotification(progress, totalBytes <= 0);
                            lastUi = now;
                        }
                    }
                    in.close();
                    raf.close();

                    updateNotification(100, false);
                    stopForeground(true);
                    stopSelf();
                } else {
                    stopForeground(true);
                    stopSelf();
                }
            } catch (Exception e) {
                Log.e("DownloadService", "Lỗi: " + e.getMessage(), e);
                stopForeground(true);
                stopSelf();
            }
        });
        worker.start();
    }

    @Override public void onDestroy() {
        super.onDestroy();
        isCanceled.set(true);
    }
}
