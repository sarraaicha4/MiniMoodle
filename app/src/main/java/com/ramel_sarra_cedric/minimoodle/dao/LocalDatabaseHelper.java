package com.ramel_sarra_cedric.minimoodle.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ramel_sarra_cedric.minimoodle.modeles.Course;

public class LocalDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "minimoodle_local.db";
    private static final int DATABASE_VERSION = 1;

    public static class QuizResult {
        public final int score;
        public final int total;

        public QuizResult(int score, int total) {
            this.score = score;
            this.total = total;
        }
    }

    public LocalDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Historique de consultation: répond à l'exigence SQLite du PDF.
        db.execSQL("CREATE TABLE consultation_history ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id TEXT NOT NULL, "
                + "course_id TEXT NOT NULL, "
                + "consulted_at INTEGER NOT NULL)");

        // Cache léger des cours pour garder des infos utiles localement.
        db.execSQL("CREATE TABLE course_cache ("
                + "course_id TEXT PRIMARY KEY, "
                + "code TEXT, "
                + "title TEXT, "
                + "teacher TEXT, "
                + "session TEXT, "
                + "cached_at INTEGER NOT NULL)");

        // Statut local d'un travail, utile si JSON Server n'est pas disponible.
        db.execSQL("CREATE TABLE assignment_status ("
                + "assignment_id TEXT PRIMARY KEY, "
                + "user_id TEXT NOT NULL, "
                + "status TEXT NOT NULL, "
                + "updated_at INTEGER NOT NULL)");

        // Résultats de quiz gardés localement.
        db.execSQL("CREATE TABLE quiz_results ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id TEXT NOT NULL, "
                + "quiz_id TEXT NOT NULL, "
                + "score INTEGER NOT NULL, "
                + "total INTEGER NOT NULL, "
                + "completed_at INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS consultation_history");
        db.execSQL("DROP TABLE IF EXISTS course_cache");
        db.execSQL("DROP TABLE IF EXISTS assignment_status");
        db.execSQL("DROP TABLE IF EXISTS quiz_results");
        onCreate(db);
    }

    public void recordCourseConsultation(String userId, String courseId) {
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("course_id", courseId);
        values.put("consulted_at", System.currentTimeMillis());
        getWritableDatabase().insert("consultation_history", null, values);
    }

    public void cacheCourse(Course course) {
        ContentValues values = new ContentValues();
        values.put("course_id", course.id);
        values.put("code", course.code);
        values.put("title", course.title);
        values.put("teacher", course.teacher);
        values.put("session", course.session);
        values.put("cached_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("course_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void saveAssignmentStatus(String userId, String assignmentId, String status) {
        ContentValues values = new ContentValues();
        values.put("assignment_id", assignmentId);
        values.put("user_id", userId);
        values.put("status", status);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("assignment_status", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getAssignmentStatus(String userId, String assignmentId) {
        Cursor cursor = getReadableDatabase().query(
                "assignment_status",
                new String[]{"status"},
                "user_id = ? AND assignment_id = ?",
                new String[]{userId, assignmentId},
                null,
                null,
                null
        );

        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public void saveQuizResult(String userId, String quizId, int score, int total) {
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("quiz_id", quizId);
        values.put("score", score);
        values.put("total", total);
        values.put("completed_at", System.currentTimeMillis());
        getWritableDatabase().insert("quiz_results", null, values);
    }

    public QuizResult getLatestQuizResult(String userId, String quizId) {
        Cursor cursor = getReadableDatabase().query(
                "quiz_results",
                new String[]{"score", "total"},
                "user_id = ? AND quiz_id = ?",
                new String[]{userId, quizId},
                null,
                null,
                "completed_at DESC",
                "1"
        );

        try {
            if (cursor.moveToFirst()) {
                return new QuizResult(cursor.getInt(0), cursor.getInt(1));
            }
            return null;
        } finally {
            cursor.close();
        }
    }
}
