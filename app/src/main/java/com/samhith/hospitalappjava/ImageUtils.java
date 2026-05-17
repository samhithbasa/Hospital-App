package com.samhith.hospitalappjava;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageUtils {

    private static final String TAG = "ImageUtils";
    public static final String BASE64_PREFIX = "base64:";

    /**
     * Encodes an image from a Uri to a compressed Base64 string.
     * Scales the image down to save space in Firestore.
     */
    public static String encodeImageToBase64(Context context, Uri sourceUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) {
                return null;
            }

            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (originalBitmap == null) {
                return null;
            }

            // Scale down the bitmap to max 200x200 to save space
            int maxWidth = 200;
            int maxHeight = 200;
            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();

            if (width > maxWidth || height > maxHeight) {
                float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);
                width = Math.round(ratio * width);
                height = Math.round(ratio * height);
                originalBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true);
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // Compress as JPEG with 70% quality
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
            return BASE64_PREFIX + encoded;

        } catch (Exception e) {
            Log.e(TAG, "Failed to encode image to Base64", e);
            return null;
        }
    }

    /**
     * Decodes a Base64 string back into a Bitmap.
     */
    public static Bitmap decodeBase64ToBitmap(String base64Str) {
        try {
            if (base64Str == null) return null;
            
            if (base64Str.startsWith(BASE64_PREFIX)) {
                base64Str = base64Str.substring(BASE64_PREFIX.length());
            }

            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode Base64 image", e);
            return null;
        }
    }
}
