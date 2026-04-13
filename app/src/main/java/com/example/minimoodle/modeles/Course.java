package com.example.minimoodle.modeles;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Course {
    public String id;
    public String code;
    public String title;
    public String description;
    public String teacher;
    public String session;
    public String status;
    public String imageUrl;
    public JSONObject schedule;
    public List<String> annonces = new ArrayList<>();
    public List<String> ressources = new ArrayList<>();

    // La méthode garde tout le parsing au même endroit pour éviter de répéter du code dans les activités.
    public static Course fromJson(JSONObject json) {
        Course course = new Course();
        course.id = json.optString("id");
        course.code = json.optString("code");
        course.title = json.optString("title");
        course.description = json.optString("description");
        course.teacher = json.optString("teacher");
        course.session = json.optString("session");
        course.status = json.optString("status", "active");
        course.imageUrl = json.optString("imageUrl");
        course.schedule = json.optJSONObject("schedule");

        JSONArray annoncesArray = json.optJSONArray("annonces");
        if (annoncesArray != null) {
            for (int i = 0; i < annoncesArray.length(); i++) {
                course.annonces.add(annoncesArray.optString(i));
            }
        }

        JSONArray ressourcesArray = json.optJSONArray("ressources");
        if (ressourcesArray != null) {
            for (int i = 0; i < ressourcesArray.length(); i++) {
                course.ressources.add(ressourcesArray.optString(i));
            }
        }

        return course;
    }
}
