package com.ramel_sarra_cedric.minimoodle.modeles;

import org.json.JSONArray;
import org.json.JSONObject;

public class Quiz {
    public String id;
    public String courseId;
    public String title;
    public String status;
    public int durationMinutes;
    public JSONArray questions;

    public static Quiz fromJson(JSONObject json) {
        Quiz quiz = new Quiz();
        quiz.id = json.optString("id");
        quiz.courseId = json.optString("courseId");
        quiz.title = json.optString("title");
        quiz.status = json.optString("status", "Non commencé");
        quiz.durationMinutes = json.optInt("durationMinutes", 0);
        quiz.questions = json.optJSONArray("questions");
        if (quiz.questions == null) {
            quiz.questions = new JSONArray();
        }
        return quiz;
    }

    public int questionCount() {
        return questions.length();
    }
}
