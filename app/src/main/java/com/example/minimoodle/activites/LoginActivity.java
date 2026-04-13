package com.example.minimoodle.activites;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.minimoodle.R;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginBtn = findViewById(R.id.loginBtn);

        loginBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString();
            String password = passwordInput.getText().toString();

            new Thread(() -> {
                try {
                    URL url = new URL("http://10.0.2.2:3000/users");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );

                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    JSONArray users = new JSONArray(response.toString());

                    boolean found = false;

                    for (int i = 0; i < users.length(); i++) {
                        JSONObject user = users.getJSONObject(i);

                        String apiEmail = user.getString("email");
                        String apiPassword = user.getString("password");

                        if (email.equals(apiEmail) && password.equals(apiPassword)) {
                            found = true;
                            break;
                        }
                    }

                    boolean finalFound = found;

                    runOnUiThread(() -> {
                        if (finalFound) {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                        } else {
                            passwordInput.setError("Email ou mot de passe incorrect");
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }
}
