package com.course.service;

import com.course.model.Course;
import com.course.repository.CourseRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Course query service keeps filtering concerns out of the console UI. */
public class CourseQueryService {
    private final CourseRepository courseRepository;

    public CourseQueryService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<Course> search(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Course> result = new ArrayList<Course>();
        for (Course course : courseRepository.findAll()) {
            String searchable = (course.getCode() + " " + course.getName() + " " + course.getTeacher())
                    .toLowerCase(Locale.ROOT);
            if (normalized.isEmpty() || searchable.contains(normalized)) result.add(course);
        }
        return result;
    }
}
