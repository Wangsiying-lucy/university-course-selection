package com.course.repository;

import com.course.model.Student;
import java.util.Optional;

public interface StudentRepository {
    Optional<Student> findById(String id);
}
