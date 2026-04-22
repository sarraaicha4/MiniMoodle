package com.ramel_sarra_cedric.minimoodle.activites;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
    private View tabHome;
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
        String tabExtra = getIntent().getStringExtra(EXTRA_TAB);
        if (tabExtra != null) activeTab = tabExtra;
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
                        if (localStatus != null) assignment.status = localStatus;
                        assignments.add(assignment);
                    }
                }

                quizzes.clear();
                for (int i = 0; i < quizzesJson.length(); i++) {
                    Quiz quiz = Quiz.fromJson(quizzesJson.optJSONObject(i));
                    if (course.id.equals(quiz.courseId)) quizzes.add(quiz);
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
        styleTabText(tabContent, "content".equals(tabName));
        styleTabText(tabAssignments, "assignments".equals(tabName));
        styleTabText(tabQuiz, "quiz".equals(tabName));

        if (course == null) return;

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

    private void styleTab(View tab, boolean selected) {
        tab.setBackgroundResource(selected ? R.drawable.bg_tab_selected : R.drawable.bg_tab);
    }

    private void styleTabText(TextView tab, boolean selected) {
        tab.setBackgroundResource(selected ? R.drawable.bg_tab_selected : R.drawable.bg_tab);
        tab.setTextColor(getColor(R.color.text_dark));
    }

    // ─── Onglet Accueil ───────────────────────────────────────────────────────

    private void renderHome() {
        detailContainer.addView(simpleText(course.description));
        if (!course.annonces.isEmpty()) detailContainer.addView(announcementCard(course.annonces.get(0)));
        if (course.schedule != null) detailContainer.addView(scheduleCard());
    }

    // ─── Onglet Contenu ───────────────────────────────────────────────────────

    private void renderContent() {
        if (course.ressources.isEmpty()) {
            detailContainer.addView(simpleText("Aucune ressource pour ce cours."));
            return;
        }
        int index = 1;
        for (String resource : course.ressources) {
            detailContainer.addView(contentCard("Cours " + index, resource));
            index++;
        }
    }

    // ─── Onglet Travaux ───────────────────────────────────────────────────────

    private void renderAssignments() {
        if (assignments.isEmpty()) {
            detailContainer.addView(simpleText("Aucun travail pour ce cours."));
            return;
        }
        for (Assignment assignment : assignments) {
            detailContainer.addView(assignmentCard(assignment));
        }
    }

    private View assignmentCard(Assignment assignment) {
        boolean submitted = "Remis".equalsIgnoreCase(assignment.status)
                || "Corrigé".equalsIgnoreCase(assignment.status);
        boolean late = "En retard".equalsIgnoreCase(assignment.status);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(late ? R.drawable.bg_card_orange : R.drawable.bg_course_card_green);
        card.setOnClickListener(v -> showAssignmentDialog(assignment));

        // Ligne titre + note (si corrigé)
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleTv = new TextView(this);
        titleTv.setText(assignment.title);
        titleTv.setTextColor(getColor(R.color.white));
        titleTv.setTextSize(15);
        titleTv.setTypeface(null, Typeface.BOLD);
        topRow.addView(titleTv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView gradeTv = new TextView(this);
        gradeTv.setText((assignment.grade == null || assignment.grade.isEmpty()) ? "-" : assignment.grade);
        gradeTv.setTextColor(getColor(R.color.white));
        gradeTv.setTextSize(14);
        gradeTv.setTypeface(null, Typeface.BOLD);
        topRow.addView(gradeTv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(topRow, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Ligne info
        String infoStr = "Date limite: " + assignment.dueDate + "  •  " + assignment.totalPoints + " pts";
        TextView infoTv = new TextView(this);
        infoTv.setText(infoStr);
        infoTv.setTextColor(getColor(R.color.white));
        infoTv.setTextSize(12);
        card.addView(infoTv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Statut
        TextView statusTv = new TextView(this);
        statusTv.setText(assignment.status);
        statusTv.setTextColor(getColor(R.color.white));
        statusTv.setTextSize(12);
        statusTv.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = dp(8);
        card.addView(statusTv, statusParams);

        // Bouton remettre si pas encore soumis
        if (!submitted) {
            Button actionBtn = new Button(this);
            actionBtn.setText("Remettre le travail");
            actionBtn.setTextColor(getColor(R.color.white));
            actionBtn.setTextSize(13);
            actionBtn.setTypeface(null, Typeface.BOLD);
            actionBtn.setAllCaps(false);
            actionBtn.setBackgroundResource(R.drawable.bg_button_orange);
            actionBtn.setIncludeFontPadding(false);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(30));
            btnParams.gravity = Gravity.CENTER_HORIZONTAL;
            btnParams.topMargin = dp(8);
            card.addView(actionBtn, btnParams);
            actionBtn.setOnClickListener(v -> markAssignmentAsSubmitted(assignment));
        }

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(cardParams);
        return card;
    }

    private void showAssignmentDialog(Assignment assignment) {
        String message = assignment.description
                + "\n\nConsignes: " + assignment.instructions
                + "\nDate limite: " + assignment.dueDate
                + "\nStatut: " + assignment.status
                + (assignment.grade != null && !assignment.grade.isEmpty() ? "\nNote: " + assignment.grade : "")
                + (assignment.comment != null && !assignment.comment.isEmpty() ? "\nCommentaire: " + assignment.comment : "");

        new AlertDialog.Builder(this)
                .setTitle(assignment.title)
                .setMessage(message)
                .setPositiveButton("Fermer", null)
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

    // ─── Onglet Quiz ──────────────────────────────────────────────────────────

    private void renderQuizzes() {
        if (quizzes.isEmpty()) {
            detailContainer.addView(simpleText("Aucun quiz pour ce cours."));
            return;
        }
        for (Quiz quiz : quizzes) {
            View card = quizCard(quiz);
            detailContainer.addView(card);
        }
    }

    private View quizCard(Quiz quiz) {
        LocalDatabaseHelper.QuizResult localResult = localDb.getLatestQuizResult(userId, quiz.id);
        boolean completed = localResult != null;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_course_card_green);

        // Ligne titre + score
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleTv = new TextView(this);
        titleTv.setText(quiz.title);
        titleTv.setTextColor(getColor(R.color.white));
        titleTv.setTextSize(15);
        titleTv.setTypeface(null, Typeface.BOLD);
        topRow.addView(titleTv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (completed) {
            TextView scoreTv = new TextView(this);
            scoreTv.setText(localResult.score + "/" + localResult.total);
            scoreTv.setTextColor(getColor(R.color.white));
            scoreTv.setTextSize(14);
            scoreTv.setTypeface(null, Typeface.BOLD);
            topRow.addView(scoreTv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        card.addView(topRow, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Ligne info
        String infoStr = quiz.questionCount() + " questions";
        if (quiz.durationMinutes > 0) infoStr += " • " + quiz.durationMinutes + " min";

        TextView infoTv = new TextView(this);
        infoTv.setText(infoStr);
        infoTv.setTextColor(getColor(R.color.white));
        infoTv.setTextSize(12);
        card.addView(infoTv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Statut
        TextView statusTv = new TextView(this);
        statusTv.setText(completed ? "Terminé" : "À faire");
        statusTv.setTextColor(getColor(R.color.white));
        statusTv.setTextSize(12);
        statusTv.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = dp(8);
        card.addView(statusTv, statusParams);

        Button actionBtn = new Button(this);
        actionBtn.setText(completed ? "Consulter le quiz" : "Commencer le quiz");
        actionBtn.setTextColor(getColor(R.color.white));
        actionBtn.setTextSize(13);
        actionBtn.setTypeface(null, Typeface.BOLD);
        actionBtn.setAllCaps(false);
        actionBtn.setBackgroundResource(R.drawable.bg_button_orange);
        actionBtn.setIncludeFontPadding(false);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(30));
        btnParams.gravity = Gravity.CENTER_HORIZONTAL;
        btnParams.topMargin = dp(2);
        card.addView(actionBtn, btnParams);
        actionBtn.setOnClickListener(v -> openQuiz(quiz));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(cardParams);
        return card;
    }

    private void openQuiz(Quiz quiz) {
        Intent intent = new Intent(this, QuizDetailActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(QuizDetailActivity.EXTRA_QUIZ_ID, quiz.id);
        startActivity(intent);
    }

    // ─── Composants réutilisables (onglet Accueil / Contenu) ──────────────────

    private TextView simpleText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(getColor(R.color.text_dark));
        textView.setTextSize(13);
        textView.setLineSpacing(4, 1);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(4), 0, dp(4), dp(12));
        textView.setLayoutParams(params);
        return textView;
    }

    private View contentCard(String title, String resourceName) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_course_card_green);
        card.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);

        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextColor(getColor(R.color.white));
        titleTv.setTextSize(14);
        titleTv.setTypeface(null, Typeface.BOLD);
        card.addView(titleTv);

        TextView resourceTv = new TextView(this);
        resourceTv.setText("• " + resourceName);
        resourceTv.setTextColor(getColor(R.color.white));
        resourceTv.setTextSize(13);
        resourceTv.setPaintFlags(resourceTv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        LinearLayout.LayoutParams resParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        resParams.setMargins(0, dp(4), 0, 0);
        resourceTv.setLayoutParams(resParams);
        resourceTv.setOnClickListener(v -> downloadFile(resourceName));
        card.addView(resourceTv);

        return card;
    }

    private void downloadFile(String name) {
        String safeName = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!safeName.endsWith(".txt")) safeName = safeName.replaceAll("\\.[^.]*$", "") + ".txt";
        String finalName = safeName;

        new Thread(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, finalName);
                    values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                    Uri uri = getContentResolver().insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) getContentResolver().openOutputStream(uri).close();
                } else {
                    java.io.File dir = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS);
                    new java.io.File(dir, finalName).createNewFile();
                }
                runOnUiThread(() -> showMessage(finalName + " téléchargé dans Téléchargements"));
            } catch (Exception e) {
                runOnUiThread(() -> showMessage("Impossible de télécharger le fichier."));
            }
        }).start();
    }

    private View announcementCard(String announcement) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_orange);

        // Header row: icon + title
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(dp(16), dp(10), dp(16), 0);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_notification);
        headerRow.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView titleTv = new TextView(this);
        titleTv.setText("Annonces (1)");
        titleTv.setTextColor(getColor(R.color.white));
        titleTv.setTextSize(13);
        titleTv.setTypeface(null, Typeface.BOLD);
        titleTv.setPadding(dp(3), 0, 0, 0);
        headerRow.addView(titleTv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(headerRow);

        // Body
        TextView bodyTv = new TextView(this);
        bodyTv.setText(announcement);
        bodyTv.setTextColor(getColor(R.color.white));
        bodyTv.setTextSize(13);
        bodyTv.setTypeface(null, Typeface.BOLD);
        bodyTv.setPadding(dp(16), dp(6), dp(16), dp(10));
        card.addView(bodyTv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);
        return card;
    }

    private View scheduleCard() {
        String day = course.schedule.optString("day", "");
        String start = course.schedule.optString("start", "-");
        String end = course.schedule.optString("end", "-");

        String[] startTimes = {"-", "-", "-", "-", "-"};
        String[] endTimes   = {"-", "-", "-", "-", "-"};
        int dayIndex = dayIndex(day);
        if (dayIndex >= 0) {
            startTimes[dayIndex] = start;
            endTimes[dayIndex]   = end;
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_white_card);
        card.setPadding(dp(16), dp(12), dp(16), dp(12));

        TextView titleTv = new TextView(this);
        titleTv.setText("Horaire");
        titleTv.setTextColor(getColor(R.color.text_dark));
        titleTv.setTextSize(14);
        titleTv.setTypeface(null, Typeface.BOLD);
        card.addView(titleTv);

        TextView sessionTv = new TextView(this);
        sessionTv.setText("Session: " + course.session);
        sessionTv.setTextColor(getColor(R.color.text_dark));
        sessionTv.setTextSize(13);
        card.addView(sessionTv);

        card.addView(scheduleRow("", new String[]{"L", "M", "M", "J", "V"}, true));

        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.text_dark));
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        divParams.setMargins(0, dp(2), 0, dp(2));
        card.addView(divider, divParams);

        card.addView(scheduleRow("Début", startTimes, false));
        card.addView(scheduleRow("Fin", endTimes, false));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);
        return card;
    }

    private LinearLayout scheduleRow(String label, String[] values, boolean isHeader) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView labelTv = new TextView(this);
        labelTv.setText(label);
        labelTv.setTextColor(getColor(R.color.text_dark));
        labelTv.setTextSize(12);
        if (isHeader) labelTv.setTypeface(null, Typeface.BOLD);
        row.addView(labelTv, new LinearLayout.LayoutParams(dp(48), LinearLayout.LayoutParams.WRAP_CONTENT));

        for (String value : values) {
            TextView tv = new TextView(this);
            tv.setText(value);
            tv.setTextColor(getColor(R.color.text_dark));
            tv.setTextSize(12);
            tv.setGravity(Gravity.CENTER);
            if (isHeader) tv.setTypeface(null, Typeface.BOLD);
            row.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, dp(4), 0, dp(4));
        row.setLayoutParams(rowParams);
        return row;
    }

    private int dayIndex(String day) {
        switch (day.toLowerCase()) {
            case "lundi":    return 0;
            case "mardi":    return 1;
            case "mercredi": return 2;
            case "jeudi":    return 3;
            case "vendredi": return 4;
            default:         return -1;
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
