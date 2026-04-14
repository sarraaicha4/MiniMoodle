package com.ramel_sarra_cedric.minimoodle.activites;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.ramel_sarra_cedric.minimoodle.R;
import com.ramel_sarra_cedric.minimoodle.dao.LocalDatabaseHelper;
import com.ramel_sarra_cedric.minimoodle.dao.MoodleDao;
import com.ramel_sarra_cedric.minimoodle.modeles.Assignment;
import com.ramel_sarra_cedric.minimoodle.modeles.Course;
import com.ramel_sarra_cedric.minimoodle.modeles.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AssignmentsActivity extends BaseMoodleActivity {
    private LinearLayout assignmentsContainer;
    private LocalDatabaseHelper localDb;
    private final List<Course> courses = new ArrayList<>();
    private final List<Assignment> assignments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignments);
        readSessionFromIntent();
        setupBottomNavigation("assignments");
        localDb = new LocalDatabaseHelper(this);

        assignmentsContainer = findViewById(R.id.assignmentsContainer);
        loadAssignments();
    }

    private void loadAssignments() {
        new Thread(() -> {
            try {
                JSONArray usersJson = MoodleDao.getArray(this, "users");
                JSONArray coursesJson = MoodleDao.getArray(this, "courses");
                JSONArray assignmentsJson = MoodleDao.getArray(this, "assignments");
                JSONObject userJson = MoodleDao.findById(usersJson, userId);
                User user = User.fromJson(userJson);

                courses.clear();
                for (int i = 0; i < coursesJson.length(); i++) {
                    Course course = Course.fromJson(coursesJson.optJSONObject(i));
                    if (user.enrolledCourseIds.contains(course.id)) {
                        localDb.cacheCourse(course);
                        courses.add(course);
                    }
                }

                assignments.clear();
                for (int i = 0; i < assignmentsJson.length(); i++) {
                    Assignment assignment = Assignment.fromJson(assignmentsJson.optJSONObject(i));
                    if (user.enrolledCourseIds.contains(assignment.courseId)) {
                        String localStatus = localDb.getAssignmentStatus(userId, assignment.id);
                        if (localStatus != null) {
                            assignment.status = localStatus;
                        }
                        assignments.add(assignment);
                    }
                }

                runOnUiThread(this::renderAssignments);
            } catch (Exception error) {
                runOnUiThread(() -> showMessage("Impossible de charger les travaux."));
            }
        }).start();
    }

    private void renderAssignments() {
        assignmentsContainer.removeAllViews();

        if (assignments.isEmpty()) {
            assignmentsContainer.addView(emptyText("Aucun travail pour vos cours."));
            return;
        }

        for (Assignment assignment : assignments) {
            View card = assignmentCard(assignment);
            card.setOnClickListener(view -> showAssignmentDialog(assignment));
            assignmentsContainer.addView(card);
        }
    }

    private View assignmentCard(Assignment assignment) {
        int background = "En retard".equalsIgnoreCase(assignment.status)
                ? R.drawable.bg_card_orange
                : R.drawable.bg_course_card_green;

        TextView card = new TextView(this);
        card.setText(assignment.title + "        " + assignment.status + "\n"
                + courseCodeFor(assignment.courseId) + "\n"
                + "Date de remise: " + assignment.dueDate + "\n"
                + "Note: " + (assignment.grade == null || assignment.grade.isEmpty() ? "-" : assignment.grade));
        card.setTextColor(getColor(R.color.white));
        card.setTextSize(12);
        card.setLineSpacing(3, 1);
        card.setBackgroundResource(background);
        card.setPadding(14, 12, 14, 12);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        card.setLayoutParams(params);
        return card;
    }

    private TextView emptyText(String message) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextColor(getColor(R.color.text_dark));
        textView.setTextSize(14);
        return textView;
    }

    private String courseCodeFor(String courseId) {
        for (Course course : courses) {
            if (course.id.equals(courseId)) {
                return course.code;
            }
        }
        return "Cours";
    }

    private void showAssignmentDialog(Assignment assignment) {
        new AlertDialog.Builder(this)
                .setTitle(assignment.title)
                .setMessage(assignment.description + "\n\nCours: " + courseCodeFor(assignment.courseId)
                        + "\nConsignes: " + assignment.instructions
                        + "\nDate limite: " + assignment.dueDate
                        + "\nStatut: " + assignment.status
                        + "\nNote: " + (assignment.grade == null || assignment.grade.isEmpty() ? "-" : assignment.grade))
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
                    renderAssignments();
                });
            } catch (Exception error) {
                localDb.saveAssignmentStatus(userId, assignment.id, "Remis");
                assignment.status = "Remis";
                runOnUiThread(() -> {
                    showMessage("Statut gardé localement; JSON Server requis pour synchroniser.");
                    renderAssignments();
                });
            }
        }).start();
    }
}
