package com.ramel_sarra_cedric.minimoodle.activites;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.ramel_sarra_cedric.minimoodle.R;
import com.ramel_sarra_cedric.minimoodle.dao.ImageLoader;
import com.ramel_sarra_cedric.minimoodle.dao.MoodleDao;
import com.ramel_sarra_cedric.minimoodle.modeles.User;

import org.json.JSONArray;
import org.json.JSONObject;

public class ProfileActivity extends BaseMoodleActivity {
    private ImageView profileImage;
    private TextView fullNameText;
    private EditText firstNameInput;
    private EditText lastNameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private EditText passwordInput;
    private EditText photoInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        readSessionFromIntent();
        setupBottomNavigation("profile");

        profileImage = findViewById(R.id.profileImage);
        fullNameText = findViewById(R.id.profileFullName);
        firstNameInput = findViewById(R.id.profileFirstNameInput);
        lastNameInput = findViewById(R.id.profileLastNameInput);
        emailInput = findViewById(R.id.profileEmailInput);
        phoneInput = findViewById(R.id.profilePhoneInput);
        passwordInput = findViewById(R.id.profilePasswordInput);
        photoInput = findViewById(R.id.profilePhotoInput);
        Button saveBtn = findViewById(R.id.saveProfileBtn);
        Button logoutBtn = findViewById(R.id.logoutBtn);

        saveBtn.setOnClickListener(view -> saveProfile());
        logoutBtn.setOnClickListener(view -> logout());
        loadProfile();
    }

    private void loadProfile() {
        new Thread(() -> {
            try {
                JSONArray users = MoodleDao.getArray(this, "users");
                JSONObject userJson = MoodleDao.findById(users, userId);
                User user = User.fromJson(userJson);
                runOnUiThread(() -> showUser(user));
            } catch (Exception error) {
                runOnUiThread(() -> showMessage("Impossible de charger le compte."));
            }
        }).start();
    }

    private void showUser(User user) {
        fullNameText.setText(user.prenom + " " + user.nom);
        firstNameInput.setText(user.prenom);
        lastNameInput.setText(user.nom);
        emailInput.setText(user.email);
        phoneInput.setText(user.telephone);
        passwordInput.setText(user.password);
        photoInput.setText(user.photoUrl);
        ImageLoader.loadInto(this, profileImage, user.photoUrl);
    }

    private void saveProfile() {
        new Thread(() -> {
            try {
                JSONObject patch = new JSONObject();
                patch.put("prenom", firstNameInput.getText().toString().trim());
                patch.put("nom", lastNameInput.getText().toString().trim());
                patch.put("username", firstNameInput.getText().toString().trim());
                patch.put("email", emailInput.getText().toString().trim());
                patch.put("telephone", phoneInput.getText().toString().trim());
                patch.put("password", passwordInput.getText().toString().trim());
                patch.put("photoUrl", photoInput.getText().toString().trim());

                // PATCH modifie seulement les champs du profil au lieu de réécrire toute la fiche utilisateur.
                MoodleDao.patchObject("users/" + userId, patch);
                runOnUiThread(() -> {
                    ImageLoader.loadInto(this, profileImage, photoInput.getText().toString().trim());
                    showMessage("Compte enregistré");
                });
            } catch (Exception error) {
                runOnUiThread(() -> showMessage("JSON Server requis pour enregistrer."));
            }
        }).start();
    }

    private void logout() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
