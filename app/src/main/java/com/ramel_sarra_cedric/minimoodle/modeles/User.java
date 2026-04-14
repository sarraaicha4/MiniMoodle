package com.ramel_sarra_cedric.minimoodle.modeles;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class User {
    public String id;
    public String username;
    public String email;
    public String password;
    public String nom;
    public String prenom;
    public String telephone;
    public String photoUrl;
    public List<String> enrolledCourseIds = new ArrayList<>();

    // Convertit un objet JSON du serveur en objet Java facile à utiliser dans les activités.
    public static User fromJson(JSONObject json) {
        User user = new User();
        user.id = json.optString("id");
        user.username = json.optString("username");
        user.email = json.optString("email");
        user.password = json.optString("password");
        user.nom = json.optString("nom");
        user.prenom = json.optString("prenom");
        user.telephone = json.optString("telephone");
        user.photoUrl = json.optString("photoUrl");

        JSONArray courseIds = json.optJSONArray("enrolledCourseIds");
        if (courseIds != null) {
            for (int i = 0; i < courseIds.length(); i++) {
                user.enrolledCourseIds.add(courseIds.optString(i));
            }
        }

        return user;
    }
}
