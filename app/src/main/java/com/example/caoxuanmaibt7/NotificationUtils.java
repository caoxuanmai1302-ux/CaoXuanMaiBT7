package com.example.caoxuanmaibt7;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

public class NotificationUtils {
    public static final String CHANNEL_ID = "download_channel";
    public static final int NOTIF_ID = 1001;

    public static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Foreground download");
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    public static Notification build(Context ctx, String title, int progress, boolean indeterminate) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.notification_download);
        rv.setTextViewText(R.id.txtTitle, title);
        rv.setProgressBar(R.id.prog, 100, progress, indeterminate);

        // PendingIntent cho 3 nút điều khiển
        Intent pause = new Intent("com.example.app.ACTION_PAUSE");
        PendingIntent pPause = PendingIntent.getBroadcast(ctx, 1, pause,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.btnPause, pPause);

        Intent resume = new Intent("com.example.app.ACTION_RESUME");
        PendingIntent pResume = PendingIntent.getBroadcast(ctx, 2, resume,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.btnResume, pResume);

        Intent cancel = new Intent("com.example.app.ACTION_CANCEL");
        PendingIntent pCancel = PendingIntent.getBroadcast(ctx, 3, cancel,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.btnCancel, pCancel);

        return new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setCustomContentView(rv)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
