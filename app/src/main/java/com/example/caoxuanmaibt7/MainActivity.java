package com.example.caoxuanmaibt7;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    EditText edtUrl;
    Button btnDownload;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edtUrl = findViewById(R.id.edtUrl);
        btnDownload = findViewById(R.id.btnDownload);

        askPostNotificationsIfNeeded();

        btnDownload.setOnClickListener(v -> {
            String url = edtUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "Nhập URL cần tải", Toast.LENGTH_SHORT).show();
                return;
            }
            String fileName = URLUtil.guessFileName(url, null, null);

            Intent i = new Intent(this, DownloadService.class);
            i.setAction(DownloadService.ACTION_START);
            i.putExtra(DownloadService.EXTRA_URL, url);
            i.putExtra(DownloadService.EXTRA_FILE_NAME, fileName);

            // Android 8+ dùng startForegroundService
            ContextCompat.startForegroundService(this, i);
            Toast.makeText(this, "Bắt đầu tải: " + fileName, Toast.LENGTH_SHORT).show();
        });
    }

    private void askPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }
}
