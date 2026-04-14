package com.ramel_sarra_cedric.minimoodle.dao;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MoodleDao {
    private static final String BASE_URL = "http://10.0.2.2:3000";

    // GET principal: on essaie d'abord JSON Server, puis on lit l'asset local pour garder une démo utilisable.
    public static JSONArray getArray(Context context, String resourceName) throws Exception {
        try {
            String response = request("GET", resourceName, null);
            return new JSONArray(response);
        } catch (Exception serverError) {
            JSONObject localDatabase = readLocalDatabase(context);
            if (localDatabase.has(resourceName)) {
                return localDatabase.getJSONArray(resourceName);
            }
            throw serverError;
        }
    }

    public static JSONObject postObject(String resourceName, JSONObject body) throws Exception {
        return new JSONObject(request("POST", resourceName, body));
    }

    public static JSONObject patchObject(String resourcePath, JSONObject body) throws Exception {
        return new JSONObject(request("PATCH", resourcePath, body));
    }

    public static JSONObject findById(JSONArray array, String id) {
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item != null && id.equals(item.optString("id"))) {
                return item;
            }
        }
        return null;
    }

    private static JSONObject readLocalDatabase(Context context) throws Exception {
        InputStream inputStream = context.getAssets().open("moodle.json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        reader.close();
        return new JSONObject(builder.toString());
    }

    private static String request(String method, String resourcePath, JSONObject body) throws Exception {
        URL url = new URL(BASE_URL + "/" + resourcePath);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(3500);
        connection.setReadTimeout(3500);
        connection.setRequestProperty("Accept", "application/json");

        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(body.toString().getBytes(StandardCharsets.UTF_8));
            outputStream.close();
        }

        int statusCode = connection.getResponseCode();
        InputStream stream = statusCode >= 200 && statusCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        connection.disconnect();

        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException("Erreur serveur JSON: " + statusCode + " " + response);
        }

        return response.toString();
    }
}
