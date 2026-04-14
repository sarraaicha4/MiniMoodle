package com.ramel_sarra_cedric.minimoodle.activites;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ramel_sarra_cedric.minimoodle.R;
import com.ramel_sarra_cedric.minimoodle.dao.MoodleDao;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

public class BaseMoodleActivity extends AppCompatActivity {
    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_COURSE_ID = "courseId";

    protected String userId = "1";

    protected void readSessionFromIntent() {
        String idFromIntent = getIntent().getStringExtra(EXTRA_USER_ID);
        if (idFromIntent != null && !idFromIntent.isEmpty()) {
            userId = idFromIntent;
        }
    }

    // Centralise la navigation pour que les écrans restent cohérents.
    protected void setupBottomNavigation(String activePage) {
        ImageButton dashboard = findViewById(R.id.navDashboard);
        ImageButton assignments = findViewById(R.id.navAssignments);
        ImageButton courses = findViewById(R.id.navCourses);
        ImageButton profile = findViewById(R.id.navProfile);

        View dashboardDot = findViewById(R.id.navDashboardDot);
        View assignmentsDot = findViewById(R.id.navAssignmentsDot);
        View coursesDot = findViewById(R.id.navCoursesDot);
        View profileDot = findViewById(R.id.navProfileDot);

        if (dashboard == null) {
            return;
        }

        dashboard.setOnClickListener(view -> openScreen(MainActivity.class));
        assignments.setOnClickListener(view -> openScreen(QuizActivity.class));
        courses.setOnClickListener(view -> openScreen(CoursesActivity.class));
        profile.setOnClickListener(view -> openScreen(ProfileActivity.class));

        boolean dashActive = "dashboard".equals(activePage);
        boolean quizActive = "quiz".equals(activePage);
        boolean coursesActive = "courses".equals(activePage);
        boolean profileActive = "profile".equals(activePage);

        tintNavButton(dashboard, dashActive);
        tintNavButton(assignments, quizActive);
        tintNavButton(courses, coursesActive);

        showDot(dashboardDot, dashActive);
        showDot(assignmentsDot, quizActive);
        showDot(coursesDot, coursesActive);
        showDot(profileDot, profileActive);

        loadProfilePhotoInNav(profile, profileActive);
    }

    protected void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void openScreen(Class<?> activityClass) {
        if (getClass().equals(activityClass)) {
            return;
        }

        Intent intent = new Intent(this, activityClass);
        intent.putExtra(EXTRA_USER_ID, userId);
        startActivity(intent);
    }

    private void tintNavButton(ImageButton button, boolean active) {
        if (button != null) {
            int color = getColor(active ? R.color.button_orange : R.color.nav_muted);
            button.setColorFilter(color);
        }
    }

    private void showDot(View dot, boolean active) {
        if (dot != null) {
            dot.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void loadProfilePhotoInNav(ImageButton profileButton, boolean active) {
        new Thread(() -> {
            try {
                JSONArray users = MoodleDao.getArray(this, "users");
                JSONObject userJson = MoodleDao.findById(users, userId);
                String photoUrl = userJson != null ? userJson.optString("photoUrl", "") : "";

                if (photoUrl.isEmpty()) {
                    runOnUiThread(() -> tintNavButton(profileButton, active));
                    return;
                }

                HttpURLConnection connection = (HttpURLConnection) new URL(photoUrl).openConnection();
                connection.setInstanceFollowRedirects(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());

                if (bitmap == null) {
                    runOnUiThread(() -> tintNavButton(profileButton, active));
                    return;
                }

                int borderColor = getColor(active ? R.color.button_orange : R.color.nav_muted);
                Bitmap circular = toCircularWithBorder(bitmap, borderColor, 3);
                runOnUiThread(() -> {
                    profileButton.clearColorFilter();
                    profileButton.setImageBitmap(circular);
                });
            } catch (Exception e) {
                runOnUiThread(() -> tintNavButton(profileButton, active));
            }
        }).start();
    }

    private Bitmap toCircularWithBorder(Bitmap src, int borderColor, int borderDp) {
        int borderPx = Math.round(borderDp * getResources().getDisplayMetrics().density);
        int size = Math.min(src.getWidth(), src.getHeight());

        Bitmap scaled = Bitmap.createScaledBitmap(src, size, size, true);
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // Bordure colorée
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(borderColor);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, borderPaint);

        // Photo découpée en cercle intérieur
        BitmapShader shader = new BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Paint imgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        imgPaint.setShader(shader);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - borderPx, imgPaint);

        return output;
    }
}
