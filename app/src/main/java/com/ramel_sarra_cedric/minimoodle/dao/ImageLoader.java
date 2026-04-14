package com.ramel_sarra_cedric.minimoodle.dao;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.ramel_sarra_cedric.minimoodle.R;

import java.net.HttpURLConnection;
import java.net.URL;

public class ImageLoader {
    public static void loadInto(Activity activity, ImageView imageView, String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            imageView.setImageResource(R.drawable.profile_fallback);
            return;
        }

        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
                connection.setInstanceFollowRedirects(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());
                if (bitmap != null) {
                    activity.runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                } else {
                    activity.runOnUiThread(() -> imageView.setImageResource(R.drawable.profile_fallback));
                }
            } catch (Exception error) {
                activity.runOnUiThread(() -> imageView.setImageResource(R.drawable.profile_fallback));
            }
        }).start();
    }
}
