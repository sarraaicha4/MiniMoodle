package com.example.minimoodle.adaptateurs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.minimoodle.R;
import com.example.minimoodle.modeles.Course;

import java.util.List;

public class CourseAdapter {
    public interface OnCourseClickListener {
        void onCourseClick(Course course);
    }

    private final LayoutInflater inflater;

    public CourseAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    // Rend les cartes à l'intérieur du LinearLayout, ce qui donne le style exact des maquettes sans RecyclerView complexe.
    public void render(LinearLayout container, List<Course> courses, OnCourseClickListener listener) {
        container.removeAllViews();

        for (Course course : courses) {
            View card = inflater.inflate(R.layout.item_course_card, container, false);
            ((TextView) card.findViewById(R.id.courseTitle)).setText(course.title);
            ((TextView) card.findViewById(R.id.courseCode)).setText("# " + course.code);
            ((TextView) card.findViewById(R.id.courseTeacher)).setText("Enseignant: " + course.teacher);
            ((TextView) card.findViewById(R.id.courseSession)).setText("Session: " + course.session);
            card.setOnClickListener(view -> listener.onCourseClick(course));
            container.addView(card);
        }
    }
}
