package com.example.minimoodle.activites;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.minimoodle.R;
import com.example.minimoodle.dao.MoodleDao;

import org.json.JSONArray;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {
    private EditText firstNameInput;
    private EditText lastNameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText phoneInput;
    private EditText photoInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        emailInput = findViewById(R.id.registerEmailInput);
        passwordInput = findViewById(R.id.registerPasswordInput);
        phoneInput = findViewById(R.id.registerPhoneInput);
        photoInput = findViewById(R.id.registerPhotoInput);
        Button registerBtn = findViewById(R.id.registerBtn);
        TextView goLoginText = findViewById(R.id.goLoginText);

        registerBtn.setOnClickListener(view -> registerUser());
        goLoginText.setOnClickListener(view -> finish());
    }

    private void registerUser() {
        String prenom = firstNameInput.getText().toString().trim();
        String nom = lastNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (prenom.isEmpty() || nom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            passwordInput.setError("Prénom, nom, courriel et mot de passe requis");
            return;
        }

        new Thread(() -> {
            try {
                String newId = String.valueOf(System.currentTimeMillis());
                JSONObject newUser = new JSONObject();
                newUser.put("id", newId);
                newUser.put("username", prenom);
                newUser.put("email", email);
                newUser.put("password", password);
                newUser.put("nom", nom);
                newUser.put("prenom", prenom);
                newUser.put("telephone", phoneInput.getText().toString().trim());
                newUser.put("photoUrl", photoInput.getText().toString().trim());
                newUser.put("enrolledCourseIds", new JSONArray().put("1").put("2").put("3"));
                newUser.put("quizResults", new JSONArray());
                newUser.put("completedAssignmentIds", new JSONArray());

                // POST vers JSON Server: c'est ce qui modifie la base de données moodle.json pendant la démo.
                JSONObject savedUser = MoodleDao.postObject("users", newUser);
                runOnUiThread(() -> openDashboard(savedUser.optString("id", newId)));
            } catch (Exception error) {
                runOnUiThread(() -> passwordInput.setError("Démarrez JSON Server pour créer un compte"));
            }
        }).start();
    }

    private void openDashboard(String newUserId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(BaseMoodleActivity.EXTRA_USER_ID, newUserId);
        startActivity(intent);
        finish();
    }
}
