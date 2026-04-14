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
import java.util.Iterator;

public class MoodleDao {
    private static final String BASE_URL = "http://10.0.2.2:3000";

    // GET principal: on essaie d'abord JSON Server, puis on lit l'asset local pour garder une démo utilisable.
    public static JSONArray getArray(Context context, String resourceName) throws Exception {
        try {
            String response = request("GET", resourceName, null);
            return sanitizeArray(new JSONArray(response));
        } catch (Exception serverError) {
            JSONObject localDatabase = sanitizeObject(readLocalDatabase(context));
            if (localDatabase.has(resourceName)) {
                return sanitizeArray(localDatabase.getJSONArray(resourceName));
            }
            throw serverError;
        }
    }

    public static JSONObject postObject(String resourceName, JSONObject body) throws Exception {
        return sanitizeObject(new JSONObject(request("POST", resourceName, body)));
    }

    public static JSONObject patchObject(String resourcePath, JSONObject body) throws Exception {
        return sanitizeObject(new JSONObject(request("PATCH", resourcePath, body)));
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

    private static JSONObject sanitizeObject(JSONObject object) throws Exception {
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            object.put(key, sanitizeValue(object.opt(key)));
        }
        return object;
    }

    private static JSONArray sanitizeArray(JSONArray array) throws Exception {
        for (int i = 0; i < array.length(); i++) {
            array.put(i, sanitizeValue(array.opt(i)));
        }
        return array;
    }

    private static Object sanitizeValue(Object value) throws Exception {
        if (value == null || value == JSONObject.NULL) {
            return value;
        }
        if (value instanceof JSONObject) {
            return sanitizeObject((JSONObject) value);
        }
        if (value instanceof JSONArray) {
            return sanitizeArray((JSONArray) value);
        }
        if (value instanceof String) {
            return repairMojibake((String) value);
        }
        return value;
    }

    private static String repairMojibake(String value) {
        if (!looksMisencoded(value)) {
            return value;
        }

        String repaired = new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        return suspiciousCharCount(repaired) < suspiciousCharCount(value) ? repaired : value;
    }

    private static boolean looksMisencoded(String value) {
        return value.contains("Ã")
                || value.contains("Â")
                || value.contains("â")
                || value.contains("�");
    }

    private static int suspiciousCharCount(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == 'Ã' || character == 'Â' || character == 'â' || character == '�') {
                count++;
            }
        }
        return count;
    }
}
