package com.ramel_sarra_cedric.minimoodle.activites;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ramel_sarra_cedric.minimoodle.R;
import com.ramel_sarra_cedric.minimoodle.dao.LocalDatabaseHelper;
import com.ramel_sarra_cedric.minimoodle.dao.MoodleDao;
import com.ramel_sarra_cedric.minimoodle.modeles.Quiz;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class QuizDetailActivity extends BaseMoodleActivity {

    public static final String EXTRA_QUIZ_ID = "quizId";

    private TextView quizTitleTv;
    private TextView quizProgressTv;
    private LinearLayout questionsContainer;
    private Button endBtn;
    private LocalDatabaseHelper localDb;

    private Quiz quiz;
    private final List<JSONObject> questions = new ArrayList<>();
    private int[] selectedAnswers;
    private final List<List<ImageView[]>> answerViews = new ArrayList<>();
    private boolean submitted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_detail);
        readSessionFromIntent();
        setupBottomNavigation("quiz");
        localDb = new LocalDatabaseHelper(this);

        quizTitleTv = findViewById(R.id.quizTitleText);
        quizProgressTv = findViewById(R.id.quizProgressText);
        questionsContainer = findViewById(R.id.dashboardContent);
        endBtn = findViewById(R.id.endBtn);
        endBtn.setOnClickListener(v -> submitQuiz());

        String quizId = getIntent().getStringExtra(EXTRA_QUIZ_ID);
        loadQuiz(quizId);
    }

    private void loadQuiz(String quizId) {
        new Thread(() -> {
            try {
                JSONArray quizzesJson = MoodleDao.getArray(this, "quizzes");
                JSONObject quizJson = MoodleDao.findById(quizzesJson, quizId);
                if (quizJson == null) throw new IllegalStateException("Quiz introuvable");
                quiz = Quiz.fromJson(quizJson);

                questions.clear();
                for (int i = 0; i < quiz.questions.length(); i++) {
                    JSONObject q = quiz.questions.optJSONObject(i);
                    JSONArray opts = q == null ? null : q.optJSONArray("options");
                    if (q != null && opts != null && opts.length() > 0) questions.add(q);
                }

                LocalDatabaseHelper.QuizResult existingResult = localDb.getLatestQuizResult(userId, quiz.id);
                runOnUiThread(() -> renderQuiz(existingResult));
            } catch (Exception e) {
                runOnUiThread(() -> showMessage("Quiz introuvable."));
            }
        }).start();
    }

    private void renderQuiz(LocalDatabaseHelper.QuizResult existingResult) {
        quizTitleTv.setText(quiz.title);
        quizProgressTv.setText("Questions: 0/" + questions.size());

        selectedAnswers = new int[questions.size()];
        for (int i = 0; i < selectedAnswers.length; i++) selectedAnswers[i] = -1;
        answerViews.clear();
        questionsContainer.removeAllViews();

        for (int i = 0; i < questions.size(); i++) {
            questionsContainer.addView(buildQuestionCard(i, questions.get(i)));
        }

        if (existingResult != null) {
            enterReviewMode(existingResult);
        }
    }

    private void enterReviewMode(LocalDatabaseHelper.QuizResult result) {
        submitted = true;
        quizProgressTv.setText("Score: " + result.score + "/" + result.total);

        for (int i = 0; i < questions.size(); i++) {
            int correctIdx = questions.get(i).optInt("correctOption", -1);
            List<ImageView[]> views = answerViews.get(i);
            if (correctIdx >= 0 && correctIdx < views.size()) {
                views.get(correctIdx)[0].setImageResource(R.drawable.ic_radio_checked);
                views.get(correctIdx)[1].setVisibility(View.VISIBLE);
            }
        }

        endBtn.setText("Retour aux quizs");
        endBtn.setOnClickListener(v -> finish());
    }

    private View buildQuestionCard(int questionIdx, JSONObject question) {
        String questionText = question.optString("question");
        JSONArray options = question.optJSONArray("options");

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.HORIZONTAL);
        outer.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams outerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        outerParams.setMargins(0, 0, 0, dp(12));
        outer.setLayoutParams(outerParams);

        // Badge numéro
        LinearLayout badge = new LinearLayout(this);
        badge.setOrientation(LinearLayout.VERTICAL);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundResource(R.drawable.bg_quiz_number);
        badge.setPadding(0, dp(5), 0, dp(5));

        TextView qNumTv = new TextView(this);
        qNumTv.setText("Q" + (questionIdx + 1));
        qNumTv.setTextColor(getColor(R.color.text_dark));
        qNumTv.setTextSize(18);
        qNumTv.setTypeface(null, Typeface.BOLD);
        qNumTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams numParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        numParams.setMargins(0, 0, 0, dp(5));
        badge.addView(qNumTv, numParams);

        TextView qPtsTv = new TextView(this);
        qPtsTv.setText("1 pt");
        qPtsTv.setTextColor(getColor(R.color.text_dark));
        qPtsTv.setTextSize(10);
        qPtsTv.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams ptsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ptsParams.setMargins(0, dp(-5), 0, 0);
        badge.addView(qPtsTv, ptsParams);

        outer.addView(badge, new LinearLayout.LayoutParams(dp(40), LinearLayout.LayoutParams.WRAP_CONTENT));

        // Carte question
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_quiz_question);
        card.setPadding(dp(10), dp(5), dp(10), dp(5));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        cardParams.setMargins(dp(10), 0, 0, 0);
        card.setLayoutParams(cardParams);

        TextView questionTv = new TextView(this);
        questionTv.setText(questionText);
        questionTv.setTextColor(getColor(R.color.white));
        questionTv.setTextSize(15);
        questionTv.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams qTextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        qTextParams.setMargins(0, 0, 0, dp(5));
        card.addView(questionTv, qTextParams);

        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.white));
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(2));
        divParams.setMargins(0, 0, 0, dp(10));
        card.addView(divider, divParams);

        // Lignes de réponses
        List<ImageView[]> questionAnswerViews = new ArrayList<>();
        answerViews.add(questionAnswerViews);

        if (options != null) {
            for (int j = 0; j < options.length(); j++) {
                final int optionIdx = j;

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(4), 0, dp(4));

                ImageView radioIv = new ImageView(this);
                radioIv.setImageResource(R.drawable.ic_radio_unchecked);
                radioIv.setContentDescription("non sélectionné");
                row.addView(radioIv, new LinearLayout.LayoutParams(dp(18), dp(18)));

                TextView optTv = new TextView(this);
                optTv.setText(options.optString(j));
                optTv.setTextColor(getColor(R.color.white));
                optTv.setTextSize(13);
                optTv.setPadding(dp(8), 0, 0, 0);
                row.addView(optTv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                ImageView checkIv = new ImageView(this);
                checkIv.setImageResource(R.drawable.ic_check);
                checkIv.setContentDescription("correct");
                checkIv.setVisibility(View.GONE);
                row.addView(checkIv, new LinearLayout.LayoutParams(dp(18), dp(18)));

                questionAnswerViews.add(new ImageView[]{radioIv, checkIv});
                row.setOnClickListener(v -> {
                    if (!submitted) selectAnswer(questionIdx, optionIdx);
                });

                card.addView(row, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }
        }

        outer.addView(card);
        return outer;
    }

    private void selectAnswer(int questionIdx, int optionIdx) {
        boolean wasAnswered = selectedAnswers[questionIdx] != -1;
        selectedAnswers[questionIdx] = optionIdx;

        List<ImageView[]> questionAnswerViews = answerViews.get(questionIdx);
        for (int i = 0; i < questionAnswerViews.size(); i++) {
            questionAnswerViews.get(i)[0].setImageResource(
                    i == optionIdx ? R.drawable.ic_radio_checked : R.drawable.ic_radio_unchecked);
        }

        if (!wasAnswered) {
            quizProgressTv.setText("Questions: " + countAnswered() + "/" + questions.size());
        }
    }

    private int countAnswered() {
        int count = 0;
        for (int sel : selectedAnswers) if (sel != -1) count++;
        return count;
    }

    private void submitQuiz() {
        if (submitted) return;

        for (int i = 0; i < selectedAnswers.length; i++) {
            if (selectedAnswers[i] == -1) {
                showMessage("Réponds à toutes les questions avant de terminer.");
                return;
            }
        }

        submitted = true;
        int score = 0;

        for (int i = 0; i < questions.size(); i++) {
            int correctIdx = questions.get(i).optInt("correctOption", -1);
            if (selectedAnswers[i] == correctIdx) score++;

            List<ImageView[]> views = answerViews.get(i);
            if (correctIdx >= 0 && correctIdx < views.size()) {
                views.get(correctIdx)[1].setVisibility(View.VISIBLE);
            }
        }

        int finalScore = score;
        quizProgressTv.setText("Score: " + finalScore + "/" + questions.size());
        endBtn.setText("Terminé");
        endBtn.setEnabled(false);

        localDb.saveQuizResult(userId, quiz.id, finalScore, questions.size());

        new Thread(() -> {
            try {
                JSONArray users = MoodleDao.getArray(this, "users");
                JSONObject userJson = MoodleDao.findById(users, userId);
                JSONArray results = userJson != null ? userJson.optJSONArray("quizResults") : null;
                if (results == null) results = new JSONArray();

                JSONObject result = new JSONObject();
                result.put("quizId", quiz.id);
                result.put("score", finalScore);
                result.put("total", questions.size());
                results.put(result);

                JSONObject patch = new JSONObject();
                patch.put("quizResults", results);
                MoodleDao.patchObject("users/" + userId, patch);
            } catch (Exception ignored) {}
        }).start();

        int percent = questions.size() == 0 ? 0 : Math.round((finalScore * 100f) / questions.size());
        showMessage("Score: " + finalScore + "/" + questions.size() + " (" + percent + "%)");
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
