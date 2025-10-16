package com.example.caoxuanmaibt7;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DownloadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Intent svc = new Intent(context, DownloadService.class);
        if ("com.example.app.ACTION_PAUSE".equals(action)) {
            svc.setAction(DownloadService.ACTION_PAUSE);
        } else if ("com.example.app.ACTION_RESUME".equals(action)) {
            svc.setAction(DownloadService.ACTION_RESUME);
        } else if ("com.example.app.ACTION_CANCEL".equals(action)) {
            svc.setAction(DownloadService.ACTION_CANCEL);
        } else {
            return;
        }
        context.startService(svc);
    }
}
