package com.ramel_sarra_cedric.minimoodle.dao;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.ramel_sarra_cedric.minimoodle.R;

import java.net.URL;

public class ImageLoader {
    // Chargement très simple d'une URL de photo, sans librairie externe, pour rester facile à expliquer.
    public static void loadInto(Activity activity, ImageView imageView, String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            imageView.setImageResource(R.drawable.ic_profile_placeholder);
            return;
        }

        new Thread(() -> {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(new URL(imageUrl).openStream());
                activity.runOnUiThread(() -> imageView.setImageBitmap(bitmap));
            } catch (Exception error) {
                activity.runOnUiThread(() -> imageView.setImageResource(R.drawable.ic_profile_placeholder));
            }
        }).start();
    }
}
