package com.example.minimoodle.activites;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.minimoodle.R;
import com.example.minimoodle.dao.MoodleDao;
import com.example.minimoodle.modeles.Assignment;
import com.example.minimoodle.modeles.Course;
import com.example.minimoodle.modeles.Quiz;
import com.example.minimoodle.modeles.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseMoodleActivity {
    private LinearLayout dashboardContent;
    private TextView welcomeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        readSessionFromIntent();
        setupBottomNavigation("dashboard");

        dashboardContent = findViewById(R.id.dashboardContent);
        welcomeText = findViewById(R.id.welcomeText);
        loadDashboard();
    }

    private void loadDashboard() {
        new Thread(() -> {
            try {
                JSONArray usersJson = MoodleDao.getArray(this, "users");
                JSONArray coursesJson = MoodleDao.getArray(this, "courses");
                JSONArray assignmentsJson = MoodleDao.getArray(this, "assignments");
                JSONArray quizzesJson = MoodleDao.getArray(this, "quizzes");

                JSONObject userJson = MoodleDao.findById(usersJson, userId);
                if (userJson == null && usersJson.length() > 0) {
                    userJson = usersJson.getJSONObject(0);
                    userId = userJson.optString("id", userId);
                }

                User user = User.fromJson(userJson);
                List<Course> courses = parseCourses(coursesJson, user);
                List<Assignment> assignments = parseAssignments(assignmentsJson, user);
                List<Quiz> quizzes = parseQuizzes(quizzesJson, user);

                runOnUiThread(() -> renderDashboard(user, courses, assignments, quizzes));
            } catch (Exception error) {
                runOnUiThread(() -> welcomeText.setText("Impossible de charger le tableau de bord."));
            }
        }).start();
    }

    private List<Course> parseCourses(JSONArray coursesJson, User user) {
        List<Course> courses = new ArrayList<>();
        for (int i = 0; i < coursesJson.length(); i++) {
            Course course = Course.fromJson(coursesJson.optJSONObject(i));
            if (user.enrolledCourseIds.contains(course.id)) {
                courses.add(course);
            }
        }
        return courses;
    }

    private List<Assignment> parseAssignments(JSONArray assignmentsJson, User user) {
        List<Assignment> assignments = new ArrayList<>();
        for (int i = 0; i < assignmentsJson.length(); i++) {
            Assignment assignment = Assignment.fromJson(assignmentsJson.optJSONObject(i));
            if (user.enrolledCourseIds.contains(assignment.courseId)) {
                assignments.add(assignment);
            }
        }
        return assignments;
    }

    private List<Quiz> parseQuizzes(JSONArray quizzesJson, User user) {
        List<Quiz> quizzes = new ArrayList<>();
        for (int i = 0; i < quizzesJson.length(); i++) {
            Quiz quiz = Quiz.fromJson(quizzesJson.optJSONObject(i));
            if (user.enrolledCourseIds.contains(quiz.courseId)) {
                quizzes.add(quiz);
            }
        }
        return quizzes;
    }

    private void renderDashboard(User user, List<Course> courses, List<Assignment> assignments, List<Quiz> quizzes) {
        dashboardContent.removeAllViews();
        welcomeText.setText("Bonjour " + user.prenom);
        dashboardContent.addView(welcomeText);

        int submitted = 0;
        int late = 0;
        int corrected = 0;
        for (Assignment assignment : assignments) {
            if ("Remis".equalsIgnoreCase(assignment.status)) {
                submitted++;
            } else if ("En retard".equalsIgnoreCase(assignment.status)) {
                late++;
            } else if ("Corrigé".equalsIgnoreCase(assignment.status)) {
                corrected++;
            }
        }

        dashboardContent.addView(metricCard("Synthèse",
                courses.size() + " cours inscrits\n"
                        + assignments.size() + " travaux suivis\n"
                        + submitted + " remis, " + late + " en retard, " + corrected + " corrigé(s)",
                R.drawable.bg_course_card_green));

        dashboardContent.addView(sectionTitle("Annonces récentes"));
        int announcementsAdded = 0;
        for (Course course : courses) {
            if (!course.annonces.isEmpty()) {
                dashboardContent.addView(metricCard(course.code, course.annonces.get(0), R.drawable.bg_card_orange));
                announcementsAdded++;
            }
            if (announcementsAdded == 2) {
                break;
            }
        }

        dashboardContent.addView(sectionTitle("Travaux à surveiller"));
        for (int i = 0; i < Math.min(assignments.size(), 3); i++) {
            Assignment assignment = assignments.get(i);
            dashboardContent.addView(metricCard(assignment.title,
                    "Date de remise: " + assignment.dueDate + "\nStatut: " + assignment.status,
                    "En retard".equalsIgnoreCase(assignment.status) ? R.drawable.bg_card_orange : R.drawable.bg_course_card_green));
        }

        dashboardContent.addView(sectionTitle("Quiz disponibles"));
        for (int i = 0; i < Math.min(quizzes.size(), 3); i++) {
            Quiz quiz = quizzes.get(i);
            dashboardContent.addView(metricCard(quiz.title, quiz.status, R.drawable.bg_course_card_green));
        }
    }

    private TextView sectionTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(getColor(R.color.text_dark));
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 18, 0, 8);
        title.setLayoutParams(params);
        return title;
    }

    private View metricCard(String title, String body, int backgroundRes) {
        TextView card = new TextView(this);
        card.setText(title + "\n" + body);
        card.setTextColor(getColor(R.color.white));
        card.setTextSize(13);
        card.setLineSpacing(3, 1);
        card.setBackgroundResource(backgroundRes);
        card.setPadding(18, 14, 18, 14);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 10);
        card.setLayoutParams(params);
        return card;
    }
}
