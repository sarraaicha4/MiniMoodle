package com.ramel_sarra_cedric.minimoodle.activites;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ramel_sarra_cedric.minimoodle.R;
import com.ramel_sarra_cedric.minimoodle.dao.MoodleDao;
import com.ramel_sarra_cedric.minimoodle.modeles.Assignment;
import com.ramel_sarra_cedric.minimoodle.modeles.Course;
import com.ramel_sarra_cedric.minimoodle.modeles.Quiz;
import com.ramel_sarra_cedric.minimoodle.modeles.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseMoodleActivity {
    private LinearLayout dashboardContent;
    private TextView welcomeText;
    private TextView headerPrenom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        readSessionFromIntent();
        setupBottomNavigation("dashboard");

        dashboardContent = findViewById(R.id.dashboardContent);
        welcomeText = findViewById(R.id.welcomeText);
        headerPrenom = findViewById(R.id.headerPrenom);
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
        headerPrenom.setText(user.prenom);

        dashboardContent.removeAllViews();

        int submitted = 0, late = 0, corrected = 0;
        for (Assignment a : assignments) {
            if ("Remis".equalsIgnoreCase(a.status)) submitted++;
            else if ("En retard".equalsIgnoreCase(a.status)) late++;
            else if ("Corrigé".equalsIgnoreCase(a.status)) corrected++;
        }

        long notDone = assignments.stream()
                .filter(a -> !"Remis".equalsIgnoreCase(a.status) && !"Corrigé".equalsIgnoreCase(a.status))
                .filter(a -> !"En retard".equalsIgnoreCase(a.status))
                .count();

        View announcementCard = buildAnnouncementsCard(courses);
        if (announcementCard != null) dashboardContent.addView(announcementCard);
        dashboardContent.addView(buildQuizCard(quizzes));
        dashboardContent.addView(buildAssignmentsCard(assignments, courses, (int) notDone, submitted + corrected, late));

    }

    private View buildQuizCard(List<Quiz> quizzes) {
        long available = quizzes.stream()
                .filter(q -> !"Terminé".equalsIgnoreCase(q.status))
                .count();

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(R.drawable.bg_course_card_green);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, dp(18), 0, dp(10));
        card.setLayoutParams(cardParams);
        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtra(EXTRA_USER_ID, userId);
            startActivity(intent);
        });

        TextView label = new TextView(this);
        label.setText("Quiz  —  " + available + " quiz disponible" + (available > 1 ? "s" : ""));
        label.setTextColor(getColor(R.color.white));
        label.setTextSize(15);
        label.setTypeface(null, Typeface.BOLD);
        card.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        ImageView chevron = new ImageView(this);
        chevron.setImageResource(R.drawable.ic_chevron_right);
        chevron.setColorFilter(getColor(R.color.white));
        card.addView(chevron, new LinearLayout.LayoutParams(dp(24), dp(24)));

        return card;
    }

    private View buildAssignmentsCard(List<Assignment> assignments, List<Course> courses,
                                      int toDo, int remis, int late) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_course_card_green);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);

        // Titre
        TextView title = new TextView(this);
        title.setText("Travaux");
        title.setTextColor(getColor(R.color.white));
        title.setTextSize(15);
        title.setTypeface(null, Typeface.BOLD);
        card.addView(title);

        // Ligne stats : N / N / N
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statsParams.setMargins(0, dp(8), 0, dp(8));
        statsRow.setLayoutParams(statsParams);

        statsRow.addView(statColumn(String.valueOf(toDo), "À faire"));
        statsRow.addView(statSeparator());
        statsRow.addView(statColumn(String.valueOf(remis), "Remis"));
        statsRow.addView(statSeparator());
        statsRow.addView(statColumn(String.valueOf(late), "En retard"));
        card.addView(statsRow);

        // Travaux à rendre bientôt
        List<Assignment> upcoming = new ArrayList<>();
        for (Assignment a : assignments) {
            if (!"Remis".equalsIgnoreCase(a.status) && !"Corrigé".equalsIgnoreCase(a.status)) {
                upcoming.add(a);
            }
        }

        if (!upcoming.isEmpty()) {
            TextView upcomingTitle = new TextView(this);
            upcomingTitle.setText("À rendre bientôt:");
            upcomingTitle.setTextColor(getColor(R.color.white));
            upcomingTitle.setTextSize(13);
            upcomingTitle.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams upParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            upParams.setMargins(0, dp(4), 0, dp(6));
            upcomingTitle.setLayoutParams(upParams);
            card.addView(upcomingTitle);

            for (int i = 0; i < Math.min(upcoming.size(), 3); i++) {
                Assignment a = upcoming.get(i);
                String courseId = a.courseId;

                Button btn = new Button(this);
                btn.setText(a.dueDate + "  –  " + a.title);
                btn.setTextColor(getColor(R.color.white));
                btn.setTextSize(13);
                btn.setTypeface(null, Typeface.BOLD);
                btn.setAllCaps(false);
                btn.setBackgroundResource(R.drawable.bg_button_orange);
                btn.setIncludeFontPadding(false);
                btn.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(34));
                btnParams.setMargins(0, 0, 0, dp(6));
                btn.setLayoutParams(btnParams);
                btn.setOnClickListener(v -> {
                    Intent intent = new Intent(this, CourseDetailActivity.class);
                    intent.putExtra(EXTRA_USER_ID, userId);
                    intent.putExtra(EXTRA_COURSE_ID, courseId);
                    intent.putExtra(EXTRA_TAB, "assignments");
                    startActivity(intent);
                });
                card.addView(btn);
            }
        }

        return card;
    }

    private LinearLayout statColumn(String number, String label) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);

        TextView numTv = new TextView(this);
        numTv.setText(number);
        numTv.setTextColor(getColor(R.color.white));
        numTv.setTextSize(22);
        numTv.setTypeface(null, Typeface.BOLD);
        numTv.setGravity(Gravity.CENTER);
        col.addView(numTv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView labelTv = new TextView(this);
        labelTv.setText(label);
        labelTv.setTextColor(getColor(R.color.white));
        labelTv.setTextSize(11);
        labelTv.setGravity(Gravity.CENTER);
        col.addView(labelTv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        return col;
    }

    private TextView statSeparator() {
        TextView sep = new TextView(this);
        sep.setText("  /  ");
        sep.setTextColor(getColor(R.color.white));
        sep.setTextSize(22);
        sep.setTypeface(null, Typeface.BOLD);
        sep.setGravity(Gravity.CENTER_VERTICAL);
        return sep;
    }

    private View buildAnnouncementsCard(List<Course> courses) {
        List<Course> withAnnouncements = new ArrayList<>();
        for (Course c : courses) {
            if (!c.annonces.isEmpty()) withAnnouncements.add(c);
        }
        if (withAnnouncements.isEmpty()) return null;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_orange);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, dp(18), 0, dp(10));
        card.setLayoutParams(cardParams);

        // Ligne titre (ic_notification + "Annonces (N)")
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(dp(16), dp(10), dp(16), 0);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_notification);
        headerRow.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView titleTv = new TextView(this);
        titleTv.setText("Annonces (" + withAnnouncements.size() + ")");
        titleTv.setTextColor(getColor(R.color.white));
        titleTv.setTextSize(13);
        titleTv.setTypeface(null, Typeface.BOLD);
        titleTv.setPadding(dp(3), 0, 0, 0);
        headerRow.addView(titleTv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(headerRow);

        // Une entrée par cours
        for (int i = 0; i < withAnnouncements.size(); i++) {
            Course course = withAnnouncements.get(i);

            TextView courseTv = new TextView(this);
            courseTv.setText(course.code);
            courseTv.setTextColor(getColor(R.color.white));
            courseTv.setTextSize(11);
            courseTv.setTypeface(null, Typeface.BOLD);
            courseTv.setPadding(dp(16), dp(8), dp(16), 0);
            card.addView(courseTv, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView bodyTv = new TextView(this);
            bodyTv.setText(course.annonces.get(0));
            bodyTv.setTextColor(getColor(R.color.white));
            bodyTv.setTextSize(13);
            bodyTv.setPadding(dp(16), dp(4), dp(16), dp(8));
            card.addView(bodyTv, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            // Séparateur blanc entre les annonces (pas après la dernière)
            if (i < withAnnouncements.size() - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(getColor(R.color.white));
                LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
                divParams.setMargins(dp(16), 0, dp(16), 0);
                card.addView(divider, divParams);
            }
        }

        return card;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
