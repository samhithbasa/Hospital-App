package com.samhith.hospitalappjava;

import android.os.AsyncTask;
import android.util.Log;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

public class EmailService {

    private static final String TAG = "EmailService";
    
    // EmailJS Configuration
    private static final String SERVICE_ID = "service_kkenfj4";
    private static final String TEMPLATE_ID = "template_rdvui38";
    private static final String PUBLIC_KEY = "blxjnrYyq20idSHzg";

    public interface EmailCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public static void sendOtpEmail(String toEmail, String otp, EmailCallback callback) {
        new SendEmailTask(toEmail, otp, callback).execute();
    }

    private static class SendEmailTask extends AsyncTask<Void, Void, String> {
        private String to;
        private String otp;
        private EmailCallback callback;

        SendEmailTask(String to, String otp, EmailCallback callback) {
            this.to = to;
            this.otp = otp;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            OkHttpClient client = new OkHttpClient();
            MediaType JSON = MediaType.get("application/json; charset=utf-8");

            try {
                // EmailJS API Data structure
                JSONObject templateParams = new JSONObject();
                templateParams.put("email", to); // This matches {{email}} in your template
                templateParams.put("otp", otp);     // This matches {{otp}} in your template

                JSONObject payload = new JSONObject();
                payload.put("service_id", SERVICE_ID);
                payload.put("template_id", TEMPLATE_ID);
                payload.put("user_id", PUBLIC_KEY);
                payload.put("template_params", templateParams);

                RequestBody body = RequestBody.create(payload.toString(), JSON);
                Request request = new Request.Builder()
                        .url("https://api.emailjs.com/api/v1.0/email/send")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return "SUCCESS";
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        return "Error: " + response.code() + " - " + errorBody;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to send email via EmailJS", e);
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if ("SUCCESS".equals(result)) {
                callback.onSuccess();
            } else {
                callback.onFailure(result);
            }
        }
    }
}
