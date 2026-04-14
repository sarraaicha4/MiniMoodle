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
import com.ramel_sarra_cedric.minimoodle.modeles.Course;
import com.ramel_sarra_cedric.minimoodle.modeles.Quiz;
import com.ramel_sarra_cedric.minimoodle.modeles.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends BaseMoodleActivity {
    private LinearLayout quizzesContainer;
    private LocalDatabaseHelper localDb;
    private final List<Course> courses = new ArrayList<>();
    private final List<Quiz> quizzes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quizzes);
        readSessionFromIntent();
        setupBottomNavigation("quiz");
        localDb = new LocalDatabaseHelper(this);

        quizzesContainer = findViewById(R.id.quizzesContainer);
        loadQuizzes();
    }

    private void loadQuizzes() {
        new Thread(() -> {
            try {
                JSONArray usersJson = MoodleDao.getArray(this, "users");
                JSONArray coursesJson = MoodleDao.getArray(this, "courses");
                JSONArray quizzesJson = MoodleDao.getArray(this, "quizzes");
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

                quizzes.clear();
                for (int i = 0; i < quizzesJson.length(); i++) {
                    Quiz quiz = Quiz.fromJson(quizzesJson.optJSONObject(i));
                    if (user.enrolledCourseIds.contains(quiz.courseId)) {
                        quizzes.add(quiz);
                    }
                }

                runOnUiThread(this::renderQuizzes);
            } catch (Exception error) {
                runOnUiThread(() -> showMessage("Impossible de charger les quiz."));
            }
        }).start();
    }

    private void renderQuizzes() {
        quizzesContainer.removeAllViews();

        if (quizzes.isEmpty()) {
            quizzesContainer.addView(emptyText("Aucun quiz pour vos cours."));
            return;
        }

        for (Quiz quiz : quizzes) {
            View card = quizCard(quiz);
            card.setOnClickListener(view -> showQuizDialog(quiz));
            quizzesContainer.addView(card);
        }
    }

    private View quizCard(Quiz quiz) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(14, 9, 14, 9);
        card.setBackgroundResource("À faire".equalsIgnoreCase(quiz.status)
                ? R.drawable.bg_card_orange
                : R.drawable.bg_course_card_green);

        TextView leftText = new TextView(this);
        leftText.setText(quiz.title + "\n" + courseCodeFor(quiz.courseId) + " • " + quiz.status);
        leftText.setTextColor(getColor(R.color.white));
        leftText.setTextSize(12);
        leftText.setLineSpacing(2, 1);

        TextView rightText = new TextView(this);
        rightText.setText(resultLabel(quiz));
        rightText.setTextColor(getColor(R.color.white));
        rightText.setTextSize(12);
        rightText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        rightText.setTypeface(null, android.graphics.Typeface.BOLD);

        card.addView(leftText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        card.addView(rightText, new LinearLayout.LayoutParams(56, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                58
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

    private String resultLabel(Quiz quiz) {
        if ("Mini-test 1".equalsIgnoreCase(quiz.title)) {
            return "90%";
        }
        if ("Quiz 1".equalsIgnoreCase(quiz.title)) {
            return "85%";
        }
        return "-";
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
                .setMessage(question.optString("question"))
                .setSingleChoiceItems(choices, -1, (dialog, which) -> selectedChoice[0] = which)
                .setPositiveButton("Valider", (dialog, which) -> {
                    int score = selectedChoice[0] == question.optInt("correctOption") ? 1 : 0;
                    localDb.saveQuizResult(userId, quiz.id, score, 1);
                    showMessage("Score: " + score + "/1");
                })
                .setNegativeButton("Fermer", null)
                .show();
    }
}
