package com.ramel_sarra_cedric.minimoodle.activites;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizActivity extends BaseMoodleActivity {
    private LinearLayout quizzesContainer;
    private LocalDatabaseHelper localDb;
    private final List<Course> courses = new ArrayList<>();
    private final List<Quiz> quizzes = new ArrayList<>();
    private final Map<String, QuizScore> quizScores = new HashMap<>();

    private static class QuizScore {
        final int score;
        final int total;

        QuizScore(int score, int total) {
            this.score = score;
            this.total = total;
        }

        int percent() {
            return total == 0 ? 0 : Math.round((score * 100f) / total);
        }

        String label() {
            return score + "/" + total;
        }
    }

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
                if (userJson == null) throw new IllegalStateException("Utilisateur introuvable");
                User user = User.fromJson(userJson);

                courses.clear();
                for (int i = 0; i < coursesJson.length(); i++) {
                    Course course = Course.fromJson(coursesJson.optJSONObject(i));
                    if (user.enrolledCourseIds.contains(course.id)) {
                        localDb.cacheCourse(course);
                        courses.add(course);
                    }
                }

                quizScores.clear();
                loadServerQuizScores(userJson);

                quizzes.clear();
                for (int i = 0; i < quizzesJson.length(); i++) {
                    Quiz quiz = Quiz.fromJson(quizzesJson.optJSONObject(i));
                    if (user.enrolledCourseIds.contains(quiz.courseId)) {
                        LocalDatabaseHelper.QuizResult localResult = localDb.getLatestQuizResult(userId, quiz.id);
                        if (localResult != null) {
                            quizScores.put(quiz.id, new QuizScore(localResult.score, localResult.total));
                        }
                        quizzes.add(quiz);
                    }
                }

                runOnUiThread(this::renderQuizzes);
            } catch (Exception error) {
                runOnUiThread(() -> showMessage("Impossible de charger les quiz."));
            }
        }).start();
    }

    private void loadServerQuizScores(JSONObject userJson) {
        JSONArray results = userJson.optJSONArray("quizResults");
        if (results == null) return;
        for (int i = 0; i < results.length(); i++) {
            JSONObject result = results.optJSONObject(i);
            if (result != null) {
                quizScores.put(
                        result.optString("quizId"),
                        new QuizScore(result.optInt("score"), result.optInt("total"))
                );
            }
        }
    }

    private void renderQuizzes() {
        quizzesContainer.removeAllViews();
        if (quizzes.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Aucun quiz pour vos cours.");
            empty.setTextColor(getColor(R.color.text_dark));
            empty.setTextSize(14);
            quizzesContainer.addView(empty);
            return;
        }

        for (Quiz quiz : quizzes) {
            View card = quizCard(quiz);
            card.setOnClickListener(view -> showQuizDialog(quiz));
            quizzesContainer.addView(card);
        }
    }

    private View quizCard(Quiz quiz) {
        QuizScore score = quizScores.get(quiz.id);
        boolean completed = score != null;
        String courseCode = courseCodeFor(quiz.courseId);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_course_card_green);

        // Ligne 1 : nom du quiz + score si complété
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleText = new TextView(this);
        titleText.setText(quiz.title);
        titleText.setTextColor(getColor(R.color.white));
        titleText.setTextSize(15);
        titleText.setTypeface(null, Typeface.BOLD);
        topRow.addView(titleText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (completed) {
            TextView scoreText = new TextView(this);
            scoreText.setText(score.score + "/" + score.total);
            scoreText.setTextColor(getColor(R.color.white));
            scoreText.setTextSize(14);
            scoreText.setTypeface(null, Typeface.BOLD);
            topRow.addView(scoreText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        card.addView(topRow, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Ligne 2 : code du cours • nb questions • durée
        LinearLayout infoRow = new LinearLayout(this);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);

        String infoStr = courseCode + " • " + quiz.questionCount() + " questions";
        if (quiz.durationMinutes > 0) infoStr += " • " + quiz.durationMinutes + " min";

        TextView infoText = new TextView(this);
        infoText.setText(infoStr);
        infoText.setTextColor(getColor(R.color.white));
        infoText.setTextSize(12);
        infoRow.addView(infoText);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        card.addView(infoRow, infoParams);

        // Statut
        TextView statusText = new TextView(this);
        statusText.setText(completed ? "Terminé" : "À faire");
        statusText.setTextColor(getColor(R.color.white));
        statusText.setTextSize(12);
        statusText.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = dp(8);
        card.addView(statusText, statusParams);

        // Bouton seulement si pas complété
        if (!completed) {
            Button actionBtn = new Button(this);
            actionBtn.setText("Commencer le quiz");
            actionBtn.setTextColor(getColor(R.color.white));
            actionBtn.setTextSize(13);
            actionBtn.setTypeface(null, Typeface.BOLD);
            actionBtn.setAllCaps(false);
            actionBtn.setBackgroundResource(R.drawable.bg_button_orange);
            actionBtn.setIncludeFontPadding(false);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(30));
            btnParams.gravity = Gravity.CENTER_HORIZONTAL;
            btnParams.topMargin = dp(2);
            card.addView(actionBtn, btnParams);
            actionBtn.setOnClickListener(v -> showQuizDialog(quiz));
        }

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(cardParams);
        return card;
    }

    private String courseCodeFor(String courseId) {
        for (Course course : courses) {
            if (course.id.equals(courseId)) return course.code;
        }
        return "Cours";
    }

    private void showQuizDialog(Quiz quiz) {
        if (quiz.questions.length() == 0) {
            showMessage("Ce quiz n'a pas de question.");
            return;
        }

        List<JSONObject> visibleQuestions = new ArrayList<>();
        List<RadioGroup> answerGroups = new ArrayList<>();
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(12), dp(18), dp(8));

        TextView instructions = new TextView(this);
        instructions.setText("Cours: " + courseCodeFor(quiz.courseId)
                + "\nQuestions: " + quiz.questionCount()
                + "\n\nRéponds à toutes les questions, puis valide pour voir ton score.");
        instructions.setTextColor(getColor(R.color.text_dark));
        instructions.setTextSize(14);
        instructions.setLineSpacing(3, 1);
        form.addView(instructions);

        for (int i = 0; i < quiz.questions.length(); i++) {
            JSONObject question = quiz.questions.optJSONObject(i);
            JSONArray options = question == null ? null : question.optJSONArray("options");
            if (question == null || options == null || options.length() == 0) continue;

            visibleQuestions.add(question);
            TextView questionTitle = new TextView(this);
            questionTitle.setText("\nQuestion " + visibleQuestions.size() + "\n" + question.optString("question"));
            questionTitle.setTextColor(getColor(R.color.text_dark));
            questionTitle.setTextSize(14);
            questionTitle.setTypeface(null, Typeface.BOLD);
            questionTitle.setLineSpacing(3, 1);
            form.addView(questionTitle);

            RadioGroup group = new RadioGroup(this);
            group.setOrientation(RadioGroup.VERTICAL);
            group.setPadding(0, dp(4), 0, dp(4));

            for (int j = 0; j < options.length(); j++) {
                RadioButton btn = new RadioButton(this);
                btn.setId(View.generateViewId());
                btn.setTag(j);
                btn.setText(options.optString(j));
                btn.setTextColor(getColor(R.color.text_dark));
                btn.setTextSize(13);
                group.addView(btn);
            }

            answerGroups.add(group);
            form.addView(group);
        }

        if (answerGroups.isEmpty()) {
            showMessage("Ce quiz n'a pas de questions utilisables.");
            return;
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(form);

        AlertDialog quizDialog = new AlertDialog.Builder(this)
                .setTitle(quiz.title)
                .setView(scrollView)
                .setPositiveButton("Valider", null)
                .setNegativeButton("Fermer", null)
                .create();

        quizDialog.setOnShowListener(dialog -> {
            Button validateButton = quizDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
            validateButton.setOnClickListener(view -> submitQuiz(quiz, visibleQuestions, answerGroups, quizDialog));
        });
        quizDialog.show();
    }

    private void submitQuiz(Quiz quiz, List<JSONObject> questions, List<RadioGroup> answerGroups, AlertDialog quizDialog) {
        for (RadioGroup group : answerGroups) {
            if (group.getCheckedRadioButtonId() == -1) {
                showMessage("Réponds à toutes les questions avant de valider.");
                return;
            }
        }

        int score = 0;
        StringBuilder corrections = new StringBuilder();

        for (int i = 0; i < questions.size(); i++) {
            JSONObject question = questions.get(i);
            JSONArray options = question.optJSONArray("options");
            RadioGroup group = answerGroups.get(i);
            RadioButton selectedButton = group.findViewById(group.getCheckedRadioButtonId());
            int selectedIndex = (int) selectedButton.getTag();
            int correctIndex = question.optInt("correctOption", -1);
            boolean correct = selectedIndex == correctIndex;

            if (correct) score++;

            corrections.append(i + 1).append(". ")
                    .append(correct ? "Correct" : "Incorrect")
                    .append(" - réponse: ")
                    .append(options != null ? options.optString(correctIndex, "?") : "?");

            String explanation = question.optString("explanation");
            if (!explanation.isEmpty()) corrections.append("\n   ").append(explanation);
            corrections.append("\n\n");
        }

        localDb.saveQuizResult(userId, quiz.id, score, questions.size());
        quizScores.put(quiz.id, new QuizScore(score, questions.size()));
        quiz.status = "Terminé";
        quizDialog.dismiss();
        renderQuizzes();

        int percent = questions.size() == 0 ? 0 : Math.round((score * 100f) / questions.size());
        new AlertDialog.Builder(this)
                .setTitle("Résultat - " + quiz.title)
                .setMessage("Score: " + score + "/" + questions.size() + " (" + percent + "%)\n\n" + corrections)
                .setPositiveButton("OK", null)
                .show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
