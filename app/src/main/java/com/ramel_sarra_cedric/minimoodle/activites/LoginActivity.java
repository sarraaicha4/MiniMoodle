package com.ramel_sarra_cedric.minimoodle.activites;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ramel_sarra_cedric.minimoodle.R;
import com.ramel_sarra_cedric.minimoodle.dao.MoodleDao;

import org.json.JSONArray;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button loginBtn;
    TextView goRegisterText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginBtn = findViewById(R.id.loginBtn);
        goRegisterText = findViewById(R.id.goRegisterText);

        loginBtn.setOnClickListener(v -> login());
        goRegisterText.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void login() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            passwordInput.setError("Courriel et mot de passe requis");
            return;
        }

        new Thread(() -> {
            try {
                JSONArray users = MoodleDao.getArray(this, "users");
                JSONObject matchedUser = null;

                // Validation demandée dans le PDF: on compare les champs avec les données du serveur JSON.
                for (int i = 0; i < users.length(); i++) {
                    JSONObject user = users.getJSONObject(i);
                    if (email.equals(user.optString("email")) && password.equals(user.optString("password"))) {
                        matchedUser = user;
                        break;
                    }
                }

                JSONObject finalMatchedUser = matchedUser;
                runOnUiThread(() -> {
                    if (finalMatchedUser == null) {
                        passwordInput.setError("Courriel ou mot de passe incorrect");
                        return;
                    }

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra(BaseMoodleActivity.EXTRA_USER_ID, finalMatchedUser.optString("id"));
                    startActivity(intent);
                    finish();
                });
            } catch (Exception error) {
                runOnUiThread(() -> passwordInput.setError("Impossible de lire moodle.json"));
            }
        }).start();
    }
}
