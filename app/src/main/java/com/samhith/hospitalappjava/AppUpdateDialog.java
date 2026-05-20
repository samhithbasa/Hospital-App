package com.samhith.hospitalappjava;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AppUpdateDialog extends DialogFragment {

    private static final String ARG_VERSION_NAME = "version_name";
    private static final String ARG_APK_URL = "apk_url";
    private static final String ARG_RELEASE_NOTES = "release_notes";
    private static final String ARG_FORCE_UPDATE = "force_update";

    private String versionName;
    private String apkUrl;
    private String releaseNotes;
    private boolean forceUpdate;

    private LinearLayout layoutUpdateButtons;
    private LinearLayout layoutDownloadProgress;
    private LinearProgressIndicator downloadProgressBar;
    private TextView tvProgressPercentage;

    public static AppUpdateDialog newInstance(String versionName, String apkUrl, String releaseNotes, boolean forceUpdate) {
        AppUpdateDialog fragment = new AppUpdateDialog();
        Bundle args = new Bundle();
        args.putString(ARG_VERSION_NAME, versionName);
        args.putString(ARG_APK_URL, apkUrl);
        args.putString(ARG_RELEASE_NOTES, releaseNotes);
        args.putBoolean(ARG_FORCE_UPDATE, forceUpdate);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            versionName = getArguments().getString(ARG_VERSION_NAME);
            apkUrl = getArguments().getString(ARG_APK_URL);
            releaseNotes = getArguments().getString(ARG_RELEASE_NOTES);
            forceUpdate = getArguments().getBoolean(ARG_FORCE_UPDATE);
        }
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_app_update, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvVersionInfo = view.findViewById(R.id.tvVersionInfo);
        TextView tvReleaseNotes = view.findViewById(R.id.tvReleaseNotes);
        TextView tvReleaseNotesLabel = view.findViewById(R.id.tvReleaseNotesLabel);
        layoutUpdateButtons = view.findViewById(R.id.layoutUpdateButtons);
        layoutDownloadProgress = view.findViewById(R.id.layoutDownloadProgress);
        downloadProgressBar = view.findViewById(R.id.downloadProgressBar);
        tvProgressPercentage = view.findViewById(R.id.tvDownloadProgressPercentage);
        MaterialButton btnLater = view.findViewById(R.id.btnUpdateLater);
        MaterialButton btnUpdate = view.findViewById(R.id.btnUpdateNow);

        tvVersionInfo.setText("Version " + versionName + " is now ready for install.");

        if (releaseNotes != null && !releaseNotes.isEmpty()) {
            tvReleaseNotesLabel.setVisibility(View.VISIBLE);
            tvReleaseNotes.setVisibility(View.VISIBLE);
            tvReleaseNotes.setText(releaseNotes);
        } else {
            tvReleaseNotesLabel.setVisibility(View.GONE);
            tvReleaseNotes.setVisibility(View.GONE);
        }

        if (forceUpdate) {
            btnLater.setVisibility(View.GONE);
            setCancelable(false);
        } else {
            btnLater.setVisibility(View.VISIBLE);
            setCancelable(true);
            btnLater.setOnClickListener(v -> dismiss());
        }

        btnUpdate.setOnClickListener(v -> startApkDownload(apkUrl));
    }

    private void startApkDownload(String url) {
        final Context appContext = requireContext().getApplicationContext();
        layoutUpdateButtons.setVisibility(View.GONE);
        layoutDownloadProgress.setVisibility(View.VISIBLE);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiIfAdded(() -> {
                    Toast.makeText(appContext, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    resetUI();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiIfAdded(() -> {
                        Toast.makeText(appContext, "Download failed: Server error " + response.code(), Toast.LENGTH_LONG).show();
                        resetUI();
                    });
                    response.close();
                    return;
                }

                if (response.body() == null) {
                    runOnUiIfAdded(() -> {
                        Toast.makeText(appContext, "Download failed: Empty response body", Toast.LENGTH_LONG).show();
                        resetUI();
                    });
                    response.close();
                    return;
                }

                File apkFile = new File(appContext.getCacheDir(), "HospitalAppUpdate.apk");

                try (InputStream inputStream = response.body().byteStream();
                     FileOutputStream outputStream = new FileOutputStream(apkFile)) {

                    long contentLength = response.body().contentLength();
                    byte[] buffer = new byte[8192];
                    long totalBytesRead = 0;
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;

                        if (contentLength > 0) {
                            final int progress = (int) ((totalBytesRead * 100) / contentLength);
                            runOnUiIfAdded(() -> {
                                downloadProgressBar.setProgress(progress);
                                tvProgressPercentage.setText("Downloading: " + progress + "%");
                            });
                        }
                    }

                    outputStream.flush();

                    runOnUiIfAdded(() -> {
                        Toast.makeText(appContext, "Download complete. Starting install...", Toast.LENGTH_SHORT).show();
                        launchInstaller(appContext, apkFile);
                        if (!forceUpdate) {
                            dismiss();
                        }
                    });

                } catch (Exception e) {
                    runOnUiIfAdded(() -> {
                        Toast.makeText(appContext, "Error writing APK: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        resetUI();
                    });
                } finally {
                    response.close();
                }
            }
        });
    }

    private void resetUI() {
        layoutDownloadProgress.setVisibility(View.GONE);
        layoutUpdateButtons.setVisibility(View.VISIBLE);
    }

    private void launchInstaller(Context context, File apkFile) {
        try {
            Uri apkUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", apkFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Failed to launch installer: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void runOnUiIfAdded(Runnable action) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (isAdded()) {
                    action.run();
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
    }
}
