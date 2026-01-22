package com.school.backend.testmanagement.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MarksheetQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<Object[]> fetchStudentMarks(Long examId, Long studentId) {

        String jpql = """
                    SELECT es.subjectId,
                           es.maxMarks,
                           sm.marksObtained
                    FROM ExamSubject es
                    LEFT JOIN StudentMark sm
                         ON sm.examSubjectId = es.id
                        AND sm.studentId = :studentId
                    WHERE es.examId = :examId
                """;

        return em.createQuery(jpql)
                .setParameter("examId", examId)
                .setParameter("studentId", studentId)
                .getResultList();
    }
}
