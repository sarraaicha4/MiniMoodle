package com.ramel_sarra_cedric.minimoodle.activites;

import android.content.DialogInterface;
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
            return score + "/" + total + " (" + percent() + "%)";
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
                if (userJson == null) {
                    throw new IllegalStateException("Utilisateur introuvable");
                }
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
        if (results == null) {
            return;
        }

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
            quizzesContainer.addView(emptyText("Aucun quiz pour vos cours."));
            return;
        }

        TextView helpText = emptyText("Sélectionne un quiz pour répondre aux questions et enregistrer ton résultat.");
        helpText.setPadding(0, 0, 0, dp(12));
        quizzesContainer.addView(helpText);

        for (Quiz quiz : quizzes) {
            View card = quizCard(quiz);
            card.setOnClickListener(view -> showQuizDialog(quiz));
            quizzesContainer.addView(card);
        }
    }

    private View quizCard(Quiz quiz) {
        QuizScore score = quizScores.get(quiz.id);
        boolean completed = score != null;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        card.setBackgroundResource(completed ? R.drawable.bg_course_card_green : R.drawable.bg_card_orange);

        TextView leftText = new TextView(this);
        leftText.setText(quiz.title + "\n"
                + courseCodeFor(quiz.courseId) + " • " + quiz.questionCount() + " questions"
                + durationLabel(quiz));
        leftText.setTextColor(getColor(R.color.white));
        leftText.setTextSize(12);
        leftText.setLineSpacing(2, 1);

        TextView rightText = new TextView(this);
        rightText.setText(completed ? "Terminé\n" + score.label() : statusLabel(quiz) + "\nOuvrir");
        rightText.setTextColor(getColor(R.color.white));
        rightText.setTextSize(11);
        rightText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        rightText.setTypeface(null, Typeface.BOLD);

        card.addView(leftText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(rightText, new LinearLayout.LayoutParams(dp(96), LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(12));
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

    private String durationLabel(Quiz quiz) {
        if (quiz.durationMinutes <= 0) {
            return "";
        }
        return " • " + quiz.durationMinutes + " min";
    }

    private String statusLabel(Quiz quiz) {
        if ("À faire".equalsIgnoreCase(quiz.status)) {
            return "Non commencé";
        }
        return quiz.status;
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
                + "\nDurée: " + (quiz.durationMinutes <= 0 ? "non limitée" : quiz.durationMinutes + " minutes")
                + "\nQuestions: " + quiz.questionCount()
                + "\n\nRéponds à toutes les questions, puis valide pour voir ton score.");
        instructions.setTextColor(getColor(R.color.text_dark));
        instructions.setTextSize(14);
        instructions.setLineSpacing(3, 1);
        form.addView(instructions);

        for (int i = 0; i < quiz.questions.length(); i++) {
            JSONObject question = quiz.questions.optJSONObject(i);
            JSONArray options = question == null ? null : question.optJSONArray("options");
            if (question == null || options == null || options.length() == 0) {
                continue;
            }

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

            for (int optionIndex = 0; optionIndex < options.length(); optionIndex++) {
                RadioButton optionButton = new RadioButton(this);
                optionButton.setId(View.generateViewId());
                optionButton.setTag(optionIndex);
                optionButton.setText(options.optString(optionIndex));
                optionButton.setTextColor(getColor(R.color.text_dark));
                optionButton.setTextSize(13);
                group.addView(optionButton);
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
            Button validateButton = quizDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            validateButton.setOnClickListener(view -> submitQuiz(quiz, visibleQuestions, answerGroups, quizDialog));
        });
        quizDialog.show();
    }

    private void submitQuiz(
            Quiz quiz,
            List<JSONObject> questions,
            List<RadioGroup> answerGroups,
            AlertDialog quizDialog
    ) {
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

            if (correct) {
                score++;
            }

            corrections.append(i + 1)
                    .append(". ")
                    .append(correct ? "Correct" : "Incorrect")
                    .append(" - réponse: ")
                    .append(options.optString(correctIndex, "non définie"));

            String explanation = question.optString("explanation");
            if (!explanation.isEmpty()) {
                corrections.append("\n   ").append(explanation);
            }
            corrections.append("\n\n");
        }

        localDb.saveQuizResult(userId, quiz.id, score, questions.size());
        quizScores.put(quiz.id, new QuizScore(score, questions.size()));
        quiz.status = "Terminé";
        quizDialog.dismiss();
        renderQuizzes();
        showResultDialog(quiz, score, questions.size(), corrections.toString());
    }

    private void showResultDialog(Quiz quiz, int score, int total, String corrections) {
        int percent = total == 0 ? 0 : Math.round((score * 100f) / total);
        new AlertDialog.Builder(this)
                .setTitle("Résultat - " + quiz.title)
                .setMessage("Score: " + score + "/" + total + " (" + percent + "%)\n\n" + corrections)
                .setPositiveButton("OK", null)
                .show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
