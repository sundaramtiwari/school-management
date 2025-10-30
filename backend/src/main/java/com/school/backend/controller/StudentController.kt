package com.school.backend.controller

import com.school.backend.student.model.Student
import com.school.backend.repository.StudentRepository
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/students")
class StudentController(private val studentRepository: StudentRepository) {

    @GetMapping
    fun getAllStudents(): List<Student> = studentRepository.findAll()

    @PostMapping
    fun addStudent(@RequestBody student: Student): Student = studentRepository.save(student)
}
