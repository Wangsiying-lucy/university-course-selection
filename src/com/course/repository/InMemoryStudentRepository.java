package com.course.repository;

import com.course.model.Student;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryStudentRepository implements StudentRepository {
    private final Map<String, Student> students = new LinkedHashMap<String, Student>();

    public void save(Student student) {
        students.put(student.getId(), student);
    }

    @Override
    public Optional<Student> findById(String id) {
        return Optional.ofNullable(id == null ? null : students.get(id.trim()));
    }
}
