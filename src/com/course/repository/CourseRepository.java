package com.course.repository;

import com.course.model.Course;
import java.util.List;
import java.util.Optional;

public interface CourseRepository {
    Optional<Course> findByCode(String code);
    List<Course> findAll();
}
