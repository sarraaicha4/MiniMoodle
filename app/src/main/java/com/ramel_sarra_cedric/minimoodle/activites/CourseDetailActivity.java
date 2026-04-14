package com.ramel_sarra_cedric.minimoodle.activites;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.ramel_sarra_cedric.minimoodle.R;
import com.ramel_sarra_cedric.minimoodle.dao.LocalDatabaseHelper;
import com.ramel_sarra_cedric.minimoodle.dao.MoodleDao;
import com.ramel_sarra_cedric.minimoodle.modeles.Assignment;
import com.ramel_sarra_cedric.minimoodle.modeles.Course;
import com.ramel_sarra_cedric.minimoodle.modeles.Quiz;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CourseDetailActivity extends BaseMoodleActivity {
    private String courseId;
    private TextView titleText;
    private TextView codeText;
    private TextView teacherText;
    private LinearLayout detailContainer;
    private TextView tabHome;
    private TextView tabContent;
    private TextView tabAssignments;
    private TextView tabQuiz;
    private Course course;
    private LocalDatabaseHelper localDb;
    private final List<Assignment> assignments = new ArrayList<>();
    private final List<Quiz> quizzes = new ArrayList<>();
    private String activeTab = "home";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);
        readSessionFromIntent();
        setupBottomNavigation("courses");
        localDb = new LocalDatabaseHelper(this);

        courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        codeText = findViewById(R.id.courseCodeText);
        titleText = findViewById(R.id.courseTitleText);
        teacherText = findViewById(R.id.courseTeacherText);
        detailContainer = findViewById(R.id.detailContainer);
        tabHome = findViewById(R.id.tabHome);
        tabContent = findViewById(R.id.tabContent);
        tabAssignments = findViewById(R.id.tabAssignments);
        tabQuiz = findViewById(R.id.tabQuiz);

        findViewById(R.id.courseBackButton).setOnClickListener(view -> finish());
        tabHome.setOnClickListener(view -> selectTab("home"));
        tabContent.setOnClickListener(view -> selectTab("content"));
        tabAssignments.setOnClickListener(view -> selectTab("assignments"));
        tabQuiz.setOnClickListener(view -> selectTab("quiz"));

        loadCourse();
    }

    private void loadCourse() {
        new Thread(() -> {
            try {
                JSONArray coursesJson = MoodleDao.getArray(this, "courses");
                JSONArray assignmentsJson = MoodleDao.getArray(this, "assignments");
                JSONArray quizzesJson = MoodleDao.getArray(this, "quizzes");

                JSONObject courseJson = MoodleDao.findById(coursesJson, courseId);
                course = Course.fromJson(courseJson);
                localDb.cacheCourse(course);
                localDb.recordCourseConsultation(userId, course.id);

                assignments.clear();
                for (int i = 0; i < assignmentsJson.length(); i++) {
                    Assignment assignment = Assignment.fromJson(assignmentsJson.optJSONObject(i));
                    if (course.id.equals(assignment.courseId)) {
                        String localStatus = localDb.getAssignmentStatus(userId, assignment.id);
                        if (localStatus != null) {
                            assignment.status = localStatus;
                        }
                        assignments.add(assignment);
                    }
                }

                quizzes.clear();
                for (int i = 0; i < quizzesJson.length(); i++) {
                    Quiz quiz = Quiz.fromJson(quizzesJson.optJSONObject(i));
                    if (course.id.equals(quiz.courseId)) {
                        quizzes.add(quiz);
                    }
                }

                runOnUiThread(() -> {
                    codeText.setText(course.code);
                    titleText.setText(course.title);
                    teacherText.setText(course.teacher);
                    selectTab(activeTab);
                });
            } catch (Exception error) {
                runOnUiThread(() -> titleText.setText("Cours introuvable"));
            }
        }).start();
    }

    private void selectTab(String tabName) {
        activeTab = tabName;
        styleTab(tabHome, "home".equals(tabName));
        styleTab(tabContent, "content".equals(tabName));
        styleTab(tabAssignments, "assignments".equals(tabName));
        styleTab(tabQuiz, "quiz".equals(tabName));

        if (course == null) {
            return;
        }

        detailContainer.removeAllViews();
        if ("content".equals(tabName)) {
            renderContent();
        } else if ("assignments".equals(tabName)) {
            renderAssignments();
        } else if ("quiz".equals(tabName)) {
            renderQuizzes();
        } else {
            renderHome();
        }
    }

    private void styleTab(TextView tab, boolean selected) {
        tab.setBackgroundResource(selected ? R.drawable.bg_tab_selected : R.drawable.bg_tab);
        tab.setTextColor(getColor(R.color.text_dark));
    }

    private void renderHome() {
        detailContainer.addView(simpleText(course.description));

        if (!course.annonces.isEmpty()) {
            detailContainer.addView(announcementCard(course.annonces.get(0)));
        }

        if (course.schedule != null) {
            detailContainer.addView(scheduleCard());
        }
    }

    private void renderContent() {
        if (course.ressources.isEmpty()) {
            detailContainer.addView(simpleText("Aucune ressource pour ce cours."));
            return;
        }

        int index = 1;
        for (String resource : course.ressources) {
            detailContainer.addView(card("Cours " + index, resource, R.drawable.bg_course_card_green));
            index++;
        }
    }

    private void renderAssignments() {
        if (assignments.isEmpty()) {
            detailContainer.addView(simpleText("Aucun travail pour ce cours."));
            return;
        }

        for (Assignment assignment : assignments) {
            int color = "En retard".equalsIgnoreCase(assignment.status)
                    ? R.drawable.bg_card_orange
                    : R.drawable.bg_course_card_green;
            View card = card(assignment.title,
                    "Date de remise: " + assignment.dueDate + "\n"
                            + "Statut: " + assignment.status + "\n"
                            + "Note: " + (assignment.grade == null || assignment.grade.isEmpty() ? "-" : assignment.grade),
                    color);
            card.setOnClickListener(view -> showAssignmentDialog(assignment));
            detailContainer.addView(card);
        }
    }

    private void renderQuizzes() {
        if (quizzes.isEmpty()) {
            detailContainer.addView(simpleText("Aucun quiz pour ce cours."));
            return;
        }

        for (Quiz quiz : quizzes) {
            View card = quizCard(quiz);
            card.setOnClickListener(view -> showQuizDialog(quiz));
            detailContainer.addView(card);
        }
    }

    private TextView simpleText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(getColor(R.color.text_dark));
        textView.setTextSize(12);
        textView.setLineSpacing(4, 1);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(4, 0, 4, 12);
        textView.setLayoutParams(params);
        return textView;
    }

    private View card(String title, String body, int backgroundRes) {
        TextView card = new TextView(this);
        card.setText(title + "\n" + body);
        card.setTextColor(getColor(R.color.white));
        card.setTextSize(13);
        card.setLineSpacing(3, 1);
        card.setBackgroundResource(backgroundRes);
        card.setPadding(16, 14, 16, 14);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        card.setLayoutParams(params);
        return card;
    }

    private View announcementCard(String announcement) {
        TextView card = new TextView(this);
        card.setText("Annonces (1)\n" + announcement);
        card.setTextColor(getColor(R.color.white));
        card.setTextSize(12);
        card.setTypeface(null, android.graphics.Typeface.BOLD);
        card.setBackgroundResource(R.drawable.bg_card_orange);
        card.setPadding(16, 10, 16, 10);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        card.setLayoutParams(params);
        return card;
    }

    private View scheduleCard() {
        TextView card = new TextView(this);
        String text = "Horaire\n"
                + "Session: " + course.session + "\n\n"
                + "        L        M        M        J        V\n"
                + "Début   " + course.schedule.optString("start") + "   -        -        -        -\n"
                + "Fin     " + course.schedule.optString("end") + "   -        -        -        -";
        card.setText(text);
        card.setTextColor(getColor(R.color.white));
        card.setTextSize(11);
        card.setLineSpacing(3, 1);
        card.setBackgroundResource(R.drawable.bg_course_card_green);
        card.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        card.setLayoutParams(params);
        return card;
    }

    private View quizCard(Quiz quiz) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(14, 8, 14, 8);
        card.setBackgroundResource("À faire".equalsIgnoreCase(quiz.status)
                ? R.drawable.bg_card_orange
                : R.drawable.bg_course_card_green);

        TextView leftText = new TextView(this);
        leftText.setText(quiz.title + "\n" + quiz.status);
        leftText.setTextColor(getColor(R.color.white));
        leftText.setTextSize(11);
        leftText.setLineSpacing(2, 1);

        TextView rightText = new TextView(this);
        rightText.setText(quizResultLabel(quiz));
        rightText.setTextColor(getColor(R.color.white));
        rightText.setTextSize(11);
        rightText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        rightText.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(54, LinearLayout.LayoutParams.WRAP_CONTENT);
        card.addView(leftText, leftParams);
        card.addView(rightText, rightParams);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50
        );
        cardParams.setMargins(0, 0, 0, 10);
        card.setLayoutParams(cardParams);
        return card;
    }

    private String quizResultLabel(Quiz quiz) {
        if ("Mini-test 1".equalsIgnoreCase(quiz.title)) {
            return "90%";
        }
        if ("Quiz 1".equalsIgnoreCase(quiz.title)) {
            return "85%";
        }
        return "-";
    }

    private void showAssignmentDialog(Assignment assignment) {
        new AlertDialog.Builder(this)
                .setTitle(assignment.title)
                .setMessage(assignment.description + "\n\nConsignes: " + assignment.instructions
                        + "\nDate limite: " + assignment.dueDate
                        + "\nStatut: " + assignment.status)
                .setPositiveButton("Marquer comme remis", (dialog, which) -> markAssignmentAsSubmitted(assignment))
                .setNegativeButton("Fermer", null)
                .show();
    }

    private void markAssignmentAsSubmitted(Assignment assignment) {
        new Thread(() -> {
            try {
                JSONObject patch = new JSONObject();
                patch.put("status", "Remis");
                MoodleDao.patchObject("assignments/" + assignment.id, patch);
                localDb.saveAssignmentStatus(userId, assignment.id, "Remis");
                assignment.status = "Remis";
                runOnUiThread(() -> {
                    showMessage("Travail marqué comme remis");
                    detailContainer.removeAllViews();
                    renderAssignments();
                });
            } catch (Exception error) {
                localDb.saveAssignmentStatus(userId, assignment.id, "Remis");
                assignment.status = "Remis";
                runOnUiThread(() -> {
                    showMessage("Statut gardé localement; JSON Server requis pour synchroniser.");
                    detailContainer.removeAllViews();
                    renderAssignments();
                });
            }
        }).start();
    }

    private void showQuizDialog(Quiz quiz) {
        if (quiz.questions.length() == 0) {
            showMessage("Ce quiz n'a pas de question.");
            return;
        }

        JSONObject question = quiz.questions.optJSONObject(0);
        JSONArray options = question.optJSONArray("options");
        String[] choices = new String[options.length()];
        for (int i = 0; i < options.length(); i++) {
            choices[i] = options.optString(i);
        }

        int[] selectedChoice = {-1};
        new AlertDialog.Builder(this)
                .setTitle(quiz.title)
                .setSingleChoiceItems(choices, -1, (dialog, which) -> selectedChoice[0] = which)
                .setPositiveButton("Valider", (dialog, which) -> {
                    int score = selectedChoice[0] == question.optInt("correctOption") ? 1 : 0;
                    showMessage("Score: " + score + "/1");
                    localDb.saveQuizResult(userId, quiz.id, score, 1);
                    saveQuizResult(quiz, score);
                })
                .setNegativeButton("Fermer", null)
                .show();
    }

    private void saveQuizResult(Quiz quiz, int score) {
        new Thread(() -> {
            try {
                JSONArray users = MoodleDao.getArray(this, "users");
                JSONObject user = MoodleDao.findById(users, userId);
                JSONArray results = user.optJSONArray("quizResults");
                if (results == null) {
                    results = new JSONArray();
                }

                JSONObject result = new JSONObject();
                result.put("quizId", quiz.id);
                result.put("score", score);
                result.put("total", 1);
                results.put(result);

                JSONObject patch = new JSONObject();
                patch.put("quizResults", results);
                MoodleDao.patchObject("users/" + userId, patch);
            } catch (Exception ignored) {
                // Le score reste visible même si JSON Server n'est pas lancé; l'enregistrement est seulement ignoré.
            }
        }).start();
    }
}
