package com.samhith.hospitalappjava;

import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import java.io.IOException;

public class UpdateChecker {
    private static final String TAG = "UpdateChecker";
    private static final String VERSION_URL = "https://raw.githubusercontent.com/samhithbasa/Hospital-App/main/releases/version.json";

    public static void checkForUpdates(final AppCompatActivity activity) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(VERSION_URL)
                .header("Cache-Control", "no-cache") // Ensure we always get the fresh config from GitHub
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to check for updates: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Update check failed: Server returned " + response.code());
                    response.close();
                    return;
                }

                if (response.body() == null) {
                    Log.e(TAG, "Update check failed: Response body is null");
                    return;
                }

                try {
                    String jsonStr = response.body().string();
                    JSONObject json = new JSONObject(jsonStr);

                    int latestVersionCode = json.getInt("latestVersionCode");
                    String latestVersionName = json.getString("latestVersionName");
                    String apkUrl = json.getString("apkUrl");
                    String releaseNotes = json.optString("releaseNotes", "");
                    boolean forceUpdate = json.optBoolean("forceUpdate", false);

                    int currentVersionCode = BuildConfig.VERSION_CODE;

                    if (latestVersionCode > currentVersionCode) {
                        activity.runOnUiThread(() -> {
                            if (!activity.isFinishing() && !activity.isDestroyed()) {
                                AppUpdateDialog dialog = AppUpdateDialog.newInstance(
                                        latestVersionName,
                                        apkUrl,
                                        releaseNotes,
                                        forceUpdate
                                );
                                dialog.show(activity.getSupportFragmentManager(), "app_update_dialog");
                            }
                        });
                    } else {
                        Log.d(TAG, "App is up to date. Local: " + currentVersionCode + ", Latest: " + latestVersionCode);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing update version response: " + e.getMessage(), e);
                } finally {
                    response.close();
                }
            }
        });
    }
}
