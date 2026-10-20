package com.course.repository;

import com.course.model.Course;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class InMemoryCourseRepository implements CourseRepository {
    private final Map<String, Course> courses = new LinkedHashMap<String, Course>();

    public void save(Course course) {
        courses.put(course.getCode(), course);
    }

    @Override
    public Optional<Course> findByCode(String code) {
        if (code == null) return Optional.empty();
        return Optional.ofNullable(courses.get(code.trim().toUpperCase(Locale.ROOT)));
    }

    @Override
    public List<Course> findAll() {
        return Collections.unmodifiableList(new ArrayList<Course>(courses.values()));
    }
}
