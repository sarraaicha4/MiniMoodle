package com.ramel_sarra_cedric.minimoodle.modeles;

import org.json.JSONObject;

public class Assignment {
    public String id;
    public String courseId;
    public String title;
    public String description;
    public String dueDate;
    public String instructions;
    public String status;
    public String grade;
    public String comment;
    public int totalPoints;
    public String type;

    public static Assignment fromJson(JSONObject json) {
        Assignment assignment = new Assignment();
        assignment.id = json.optString("id");
        assignment.courseId = json.optString("courseId");
        assignment.title = json.optString("title");
        assignment.description = json.optString("description");
        assignment.dueDate = json.optString("dueDate");
        assignment.instructions = json.optString("instructions");
        assignment.status = json.optString("status", "À faire");
        assignment.grade = json.optString("grade", "");
        assignment.comment = json.optString("comment", "");
        assignment.totalPoints = json.optInt("totalPoints", 20);
        assignment.type = json.optString("type", "text");
        return assignment;
    }
}
