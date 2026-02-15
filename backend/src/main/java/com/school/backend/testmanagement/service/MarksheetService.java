package com.school.backend.testmanagement.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.guardian.entity.Guardian;
import com.school.backend.core.guardian.repository.GuardianRepository;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.entity.StudentGuardian;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentGuardianRepository;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.testmanagement.dto.MarksheetDto;
import com.school.backend.testmanagement.entity.Exam;
import com.school.backend.testmanagement.entity.GradePolicy;
import com.school.backend.testmanagement.repository.ExamRepository;
import com.school.backend.testmanagement.repository.GradePolicyRepository;
import com.school.backend.testmanagement.repository.MarksheetQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarksheetService {

    private final MarksheetQueryRepository queryRepo;
    private final ExamRepository examRepository;
    private final GradePolicyRepository gradeRepo;
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final GuardianRepository guardianRepository;
    private final AcademicSessionRepository sessionRepository;

    @Transactional(readOnly = true)
    public byte[] generatePdf(Long examId, Long studentId) {
        MarksheetDto data = generate(examId, studentId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        School school = schoolRepository.findById(exam.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        AcademicSession session = sessionRepository.findById(exam.getSessionId())
                .orElse(AcademicSession.builder().name("").build());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Header
            addSchoolHeader(document, school);

            // 2. Marksheet Title
            addTitle(document, exam, session.getName());

            // 3. Student Details
            addStudentDetails(document, student, exam);

            // 4. Marks Table
            addMarksTable(document, data);

            // 5. Result Summary
            addResultSummary(document, data);

            // 6. Footer (Signatures)
            addFooter(document);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate marksheet PDF", e);
        }
    }

    private void addSchoolHeader(Document document, School school) throws DocumentException {
        Font schoolNameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph schoolName = new Paragraph(
                school.getDisplayName() != null ? school.getDisplayName() : school.getName(), schoolNameFont);
        schoolName.setAlignment(Element.ALIGN_CENTER);
        document.add(schoolName);

        Font addressFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        String addr = String.format("%s, %s, %s", school.getAddress(), school.getCity(), school.getState());
        Paragraph address = new Paragraph(addr, addressFont);
        address.setAlignment(Element.ALIGN_CENTER);
        document.add(address);

        if (school.getAffiliationCode() != null) {
            Paragraph affLine = new Paragraph("Affiliation No: " + school.getAffiliationCode(), addressFont);
            affLine.setAlignment(Element.ALIGN_CENTER);
            document.add(affLine);
        }

        document.add(new Paragraph(" "));
    }

    private void addTitle(Document document, Exam exam, String sessionName) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Paragraph title = new Paragraph("REPORT CARD / MARKSHEET", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph examName = new Paragraph(exam.getName() + " - " + sessionName,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11));
        examName.setAlignment(Element.ALIGN_CENTER);
        document.add(examName);

        document.add(new Paragraph(" "));
    }

    private void addStudentDetails(Document document, Student student, Exam exam) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        // Fetch roll number from enrollment
        Optional<StudentEnrollment> enrollment = enrollmentRepository
                .findByStudentIdAndSessionId(student.getId(), exam.getSessionId()).stream().findFirst();
        String rollNo = enrollment.map(e -> e.getRollNumber() != null ? e.getRollNumber().toString() : "N/A")
                .orElse("N/A");
        String section = enrollment.map(StudentEnrollment::getSection).orElse("A");

        // Fetch father's name (Primary Guardian)
        String fatherName = "N/A";
        List<StudentGuardian> studentGuardians = studentGuardianRepository.findByStudentId(student.getId());
        for (StudentGuardian sg : studentGuardians) {
            if (sg.isPrimaryGuardian()) {
                Guardian g = guardianRepository.findById(sg.getGuardianId()).orElse(null);
                if (g != null) {
                    fatherName = g.getName();
                    break;
                }
            }
        }

        String className = student.getCurrentClass() != null ? student.getCurrentClass().getName() + " - " + section
                : "N/A";

        addStudentDetailRow(table, "Student Name:",
                student.getFirstName() + " " + (student.getLastName() != null ? student.getLastName() : ""), labelFont,
                valueFont);
        addStudentDetailRow(table, "Admission No:", student.getAdmissionNumber(), labelFont, valueFont);
        addStudentDetailRow(table, "Father's Name:", fatherName, labelFont, valueFont);
        addStudentDetailRow(table, "Roll No:", rollNo, labelFont, valueFont);
        addStudentDetailRow(table, "Class:", className, labelFont, valueFont);
        addStudentDetailRow(table, "Date of Birth:", student.getDob() != null ? student.getDob().toString() : "N/A",
                labelFont, valueFont);

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addStudentDetailRow(PdfPTable table, String label, String value, Font lFont, Font vFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, lFont));
        c1.setBorder(Rectangle.NO_BORDER);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, vFont));
        c2.setBorder(Rectangle.NO_BORDER);
        table.addCell(c2);
    }

    private void addMarksTable(Document document, MarksheetDto data) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 3f, 1f, 1f, 1f });

        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Color headerColor = new Color(52, 73, 94);

        addTableCell(table, "Subject", headFont, headerColor, Element.ALIGN_CENTER);
        addTableCell(table, "Max Marks", headFont, headerColor, Element.ALIGN_CENTER);
        addTableCell(table, "Marks Obtained", headFont, headerColor, Element.ALIGN_CENTER);
        addTableCell(table, "Grade", headFont, headerColor, Element.ALIGN_CENTER);

        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        for (MarksheetDto.SubjectMark sm : data.getSubjects()) {
            addTableCell(table, sm.getSubjectName(), cellFont, null, Element.ALIGN_LEFT);
            addTableCell(table, sm.getMaxMarks().toString(), cellFont, null, Element.ALIGN_CENTER);
            addTableCell(table, sm.getMarksObtained().toString(), cellFont, null, Element.ALIGN_CENTER);

            double p = (sm.getMarksObtained() * 100.0) / sm.getMaxMarks();
            addTableCell(table, calcGrade(p), cellFont, null, Element.ALIGN_CENTER);
        }

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addTableCell(PdfPTable table, String text, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        if (bg != null)
            cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addResultSummary(Document document, MarksheetDto data) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(40);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        table.addCell(new Phrase("Grand Total:", bold));
        table.addCell(new Phrase(data.getTotalMarks() + " / " + data.getMaxMarks()));

        table.addCell(new Phrase("Percentage:", bold));
        table.addCell(new Phrase(data.getPercentage() + "%"));

        table.addCell(new Phrase("Grade:", bold));
        table.addCell(new Phrase(data.getGrade()));

        table.addCell(new Phrase("Result:", bold));
        table.addCell(new Phrase(data.isPassed() ? "PASS" : "FAIL",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, data.isPassed() ? Color.GREEN : Color.RED)));

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        Font signFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        PdfPCell c1 = new PdfPCell(new Phrase("Class Teacher's Signature", signFont));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase("Principal's Signature", signFont));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c2);

        document.add(table);
    }

    @Transactional(readOnly = true)
    public MarksheetDto generate(Long examId, Long studentId) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));

        if (exam.getStatus() == com.school.backend.common.enums.ExamStatus.DRAFT) {
            throw new com.school.backend.common.exception.BusinessException(
                    "Marksheet cannot be generated for DRAFT exams.");
        }

        List<Object[]> rows = queryRepo.fetchStudentMarks(examId, studentId);

        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("No subjects found for exam");
        }

        int total = 0;
        int max = 0;

        List<MarksheetDto.SubjectMark> subjects = new ArrayList<>();

        for (Object[] r : rows) {

            Long subjectId = (Long) r[0];
            String subjectName = (String) r[1];
            Integer maxMarks = (Integer) r[2];
            Integer obtained = (Integer) r[3];

            if (obtained == null) {
                obtained = 0;
            }

            total += obtained;
            max += maxMarks;

            MarksheetDto.SubjectMark sm = new MarksheetDto.SubjectMark();

            sm.setSubjectId(subjectId);
            sm.setSubjectName(subjectName);
            sm.setMaxMarks(maxMarks);
            sm.setMarksObtained(obtained);

            subjects.add(sm);
        }

        double percent = max == 0 ? 0 : (total * 100.0) / max;

        MarksheetDto dto = new MarksheetDto();

        dto.setStudentId(studentId);
        dto.setExamId(examId);

        dto.setTotalMarks(total);
        dto.setMaxMarks(max);

        dto.setPercentage(round(percent));

        dto.setPassed(percent >= 33.0);

        dto.setGrade(resolveGrade(exam.getSchoolId(), percent));

        dto.setSubjects(subjects);

        return dto;
    }

    private String resolveGrade(Long schoolId, double percent) {

        List<GradePolicy> policies = gradeRepo.findBySchoolIdOrderByMinPercentDesc(schoolId);

        for (GradePolicy gp : policies) {

            if (percent >= gp.getMinPercent()
                    && percent <= gp.getMaxPercent()) {

                return gp.getGrade();
            }
        }

        return calcGrade(percent); // fallback
    }

    private String calcGrade(double p) {

        if (p >= 90)
            return "A+";
        if (p >= 80)
            return "A";
        if (p >= 70)
            return "B+";
        if (p >= 60)
            return "B";
        if (p >= 50)
            return "C";
        if (p >= 33)
            return "D";

        return "F";
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
