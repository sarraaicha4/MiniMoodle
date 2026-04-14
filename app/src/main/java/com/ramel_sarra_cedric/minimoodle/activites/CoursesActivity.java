package com.ramel_sarra_cedric.minimoodle.activites;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.ramel_sarra_cedric.minimoodle.R;
import com.ramel_sarra_cedric.minimoodle.adaptateurs.CourseAdapter;
import com.ramel_sarra_cedric.minimoodle.dao.LocalDatabaseHelper;
import com.ramel_sarra_cedric.minimoodle.dao.MoodleDao;
import com.ramel_sarra_cedric.minimoodle.modeles.Course;
import com.ramel_sarra_cedric.minimoodle.modeles.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CoursesActivity extends BaseMoodleActivity {
    private LinearLayout coursesContainer;
    private EditText searchInput;
    private Button filterAllBtn;
    private Button filterActiveBtn;
    private Button filterFinishedBtn;
    private final List<Course> allCourses = new ArrayList<>();
    private CourseAdapter adapter;
    private LocalDatabaseHelper localDb;
    private String activeFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courses);
        readSessionFromIntent();
        setupBottomNavigation("courses");

        coursesContainer = findViewById(R.id.coursesContainer);
        searchInput = findViewById(R.id.courseSearchInput);
        filterAllBtn = findViewById(R.id.filterAllBtn);
        filterActiveBtn = findViewById(R.id.filterActiveBtn);
        filterFinishedBtn = findViewById(R.id.filterFinishedBtn);
        adapter = new CourseAdapter(this);
        localDb = new LocalDatabaseHelper(this);

        filterAllBtn.setOnClickListener(view -> setFilter("all"));
        filterActiveBtn.setOnClickListener(view -> setFilter("active"));
        filterFinishedBtn.setOnClickListener(view -> setFilter("finished"));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                renderCourses();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        loadCourses();
    }

    private void loadCourses() {
        new Thread(() -> {
            try {
                JSONArray usersJson = MoodleDao.getArray(this, "users");
                JSONArray coursesJson = MoodleDao.getArray(this, "courses");
                JSONObject userJson = MoodleDao.findById(usersJson, userId);
                User user = User.fromJson(userJson);

                allCourses.clear();
                for (int i = 0; i < coursesJson.length(); i++) {
                    Course course = Course.fromJson(coursesJson.optJSONObject(i));
                    if (user.enrolledCourseIds.contains(course.id)) {
                        localDb.cacheCourse(course);
                        allCourses.add(course);
                    }
                }

                runOnUiThread(this::renderCourses);
            } catch (Exception error) {
                runOnUiThread(() -> showMessage("Impossible de charger les cours."));
            }
        }).start();
    }

    private void setFilter(String filter) {
        activeFilter = filter;
        updateFilterButtons();
        renderCourses();
    }

    private void updateFilterButtons() {
        styleFilter(filterAllBtn, "all".equals(activeFilter));
        styleFilter(filterActiveBtn, "active".equals(activeFilter));
        styleFilter(filterFinishedBtn, "finished".equals(activeFilter));
    }

    private void styleFilter(Button button, boolean selected) {
        button.setBackgroundResource(selected ? R.drawable.bg_filter_selected : R.drawable.bg_filter_button);
        button.setTextColor(getColor(selected ? R.color.white : R.color.button_orange));
    }

    private void renderCourses() {
        String query = searchInput.getText().toString().trim().toLowerCase();
        List<Course> visibleCourses = new ArrayList<>();

        // Recherche simple par code ou nom de cours, comme recommandé dans l'énoncé.
        for (Course course : allCourses) {
            boolean matchesText = query.isEmpty()
                    || course.title.toLowerCase().contains(query)
                    || course.code.toLowerCase().contains(query);
            boolean matchesFilter = "all".equals(activeFilter)
                    || ("active".equals(activeFilter) && "active".equalsIgnoreCase(course.status))
                    || ("finished".equals(activeFilter) && "finished".equalsIgnoreCase(course.status));

            if (matchesText && matchesFilter) {
                visibleCourses.add(course);
            }
        }

        adapter.render(coursesContainer, visibleCourses, course -> {
            Intent intent = new Intent(this, CourseDetailActivity.class);
            intent.putExtra(EXTRA_USER_ID, userId);
            intent.putExtra(EXTRA_COURSE_ID, course.id);
            startActivity(intent);
        });
    }
}
