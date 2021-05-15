package com.jpsoft.downloadmanager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.jpsoft.downloadmanager.databinding.ActivityMainBinding;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.util.Objects;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private long downloadId;
    File requestFilePath;
    private String url = "";
    private MediaPlayer mpintro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Catching Download Complete events
        registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        binding.button.setOnClickListener(v -> {
            binding.LayoutError.setVisibility(View.GONE);
            url = Objects.requireNonNull(binding.editText.getText()).toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter valid url", Toast.LENGTH_SHORT).show();
            } else {
                requestWritePermission(url);
                //startDownload(url);
            }
        });
    }

    private void startDownload(String downloadUrl) {
        // File Name
        String nameOfFile = URLUtil.guessFileName(downloadUrl, null,
                MimeTypeMap.getFileExtensionFromUrl(downloadUrl));

//        File file = new File(getExternalFilesDir("QuranAudio"), nameOfFile);
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/QuranAudio/", nameOfFile);

        requestFilePath = file;

        if (requestFilePath.getAbsoluteFile().canRead()) {
            Toast.makeText(this, "File Already Exist", Toast.LENGTH_SHORT).show();
            playSura(requestFilePath.getAbsolutePath());
            return;
        } else if (requestFilePath.isFile()) {
            Toast.makeText(this, "File Already Exist", Toast.LENGTH_SHORT).show();
            playSura(requestFilePath.getAbsolutePath());
            return;
        } else if (requestFilePath.canRead()) {
            Toast.makeText(this, "File Already Exist", Toast.LENGTH_SHORT).show();
            playSura(requestFilePath.getAbsolutePath());
            return;
        }

        try {
            DownloadManager.Request request;

            request = new DownloadManager.Request(Uri.parse(downloadUrl))
                    .setTitle(nameOfFile)
                    .setDescription(nameOfFile)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(file))
//                    .setDestinationInExternalFilesDir(MainActivity.this,Environment.DIRECTORY_MUSIC,"/QuranAudio/"+nameOfFile)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true);


            Context context = this;
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
            downloadId = downloadManager.enqueue(request);
        } catch (Exception e) {
            binding.textViewErrorAlart.setTextColor(getResources().getColor(R.color.red));
            binding.textViewErrorAlart.setText("Error");
            binding.LayoutError.setVisibility(View.VISIBLE);
            binding.textViewError.setText(e.getMessage());
        }
    }

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId == id) {
                binding.textViewErrorAlart.setTextColor(getResources().getColor(R.color.green));
                binding.textViewErrorAlart.setText("Success");
                binding.LayoutError.setVisibility(View.VISIBLE);
                binding.textViewError.setText("Download Completed to \n" + requestFilePath.getAbsolutePath());

                playSura(requestFilePath.getAbsolutePath());
            }
        }
    };

    private void playSura(String path) {
        try {
            mpintro = MediaPlayer.create(MainActivity.this, Uri.parse(path));
            mpintro.setLooping(false);
            mpintro.start();
        } catch (Exception e) {
            binding.textViewErrorAlart.setTextColor(getResources().getColor(R.color.red));
            binding.LayoutError.setVisibility(View.VISIBLE);
            binding.textViewError.setText("Music player exception " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(downloadReceiver);
    }

    private void requestWritePermission(String downloadUrl) {
        Dexter.withContext(this)
                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        // permission is granted
                        startDownload(downloadUrl);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        // check for permanent denial of permission
                        if (response.isPermanentlyDenied()) {
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Need Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", (dialog, which) -> {
            dialog.cancel();
            openSettings();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();

    }

    // navigating user to app settings
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }

}