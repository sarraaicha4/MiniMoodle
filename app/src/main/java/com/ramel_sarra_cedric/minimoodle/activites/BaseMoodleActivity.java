package com.ramel_sarra_cedric.minimoodle.activites;

import android.content.Intent;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ramel_sarra_cedric.minimoodle.R;

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

    // Centralise la navigation pour que les cinq écrans restent cohérents.
    protected void setupBottomNavigation(String activePage) {
        ImageButton dashboard = findViewById(R.id.navDashboard);
        ImageButton assignments = findViewById(R.id.navAssignments);
        ImageButton grid = findViewById(R.id.navGrid);
        ImageButton courses = findViewById(R.id.navCourses);
        ImageButton profile = findViewById(R.id.navProfile);

        if (dashboard == null) {
            return;
        }

        dashboard.setOnClickListener(view -> openScreen(MainActivity.class));
        assignments.setOnClickListener(view -> openScreen(QuizActivity.class));
        grid.setOnClickListener(view -> openScreen(AssignmentsActivity.class));
        courses.setOnClickListener(view -> openScreen(CoursesActivity.class));
        profile.setOnClickListener(view -> openScreen(ProfileActivity.class));

        tintNavButton(dashboard, "dashboard".equals(activePage));
        tintNavButton(assignments, "quiz".equals(activePage));
        tintNavButton(grid, "assignments".equals(activePage));
        tintNavButton(courses, "courses".equals(activePage));
        tintNavButton(profile, "profile".equals(activePage));
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
}
