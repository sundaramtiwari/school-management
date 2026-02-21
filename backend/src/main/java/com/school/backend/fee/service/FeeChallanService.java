package com.school.backend.fee.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Chunk;
import com.lowagie.text.DocumentException;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FeeChallanService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentFeeAssignmentRepository assignmentRepository;
    private final AcademicSessionRepository academicSessionRepository;

    @Transactional(readOnly = true)
    public byte[] generateChallan(Long studentId, Long sessionId, Long schoolId, int months) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        if (!student.getSchoolId().equals(schoolId)) {
            throw new AccessDeniedException("Access denied: Student does not belong to your school");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        // Fetch session name
        com.school.backend.school.entity.AcademicSession academicSession = academicSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic Session not found: " + sessionId));
        String sessionName = academicSession.getName();

        // Get all fee assignments for this student and session
        List<StudentFeeAssignment> assignments = assignmentRepository.findByStudentIdAndSessionId(studentId, sessionId);

        if (assignments.isEmpty()) {
            throw new IllegalArgumentException("No fee assignments found for student in session " + sessionName);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);

            document.open();

            // Build the challan
            addSchoolHeader(document, school);
            addChallanTitle(document, sessionName, months);
            addStudentDetails(document, student);
            java.math.BigDecimal totalAmount = addFeeBreakdown(document, assignments, months);
            addPaymentDetails(document, sessionName, totalAmount);
            addFooter(document, school);

            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate fee challan", e);
        }
    }

    private void addSchoolHeader(Document document, School school) throws DocumentException {
        // School Name (Large, Bold, Centered)
        Font schoolNameFont = FontFactory.getFont(FontFactory.HELVETICA, 18, Font.BOLD, Color.DARK_GRAY);
        Paragraph schoolName = new Paragraph(
                school.getDisplayName() != null ? school.getDisplayName() : school.getName(), schoolNameFont);
        schoolName.setAlignment(Element.ALIGN_CENTER);
        document.add(schoolName);

        // School Address
        Font addressFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        if (school.getAddress() != null) {
            Paragraph address = new Paragraph(school.getAddress() + ", " + school.getCity() + ", " + school.getState(),
                    addressFont);
            address.setAlignment(Element.ALIGN_CENTER);
            document.add(address);
        }

        // Contact Details
        if (school.getContactNumber() != null || school.getContactEmail() != null) {
            String contact = "";
            if (school.getContactNumber() != null)
                contact += "Tel: " + school.getContactNumber();
            if (school.getContactEmail() != null)
                contact += (contact.isEmpty() ? "" : " | ") + "Email: " + school.getContactEmail();

            Paragraph contactPara = new Paragraph(contact, addressFont);
            contactPara.setAlignment(Element.ALIGN_CENTER);
            document.add(contactPara);
        }

        // Affiliation Code (if present)
        if (school.getAffiliationCode() != null && !school.getAffiliationCode().isEmpty()) {
            Paragraph affiliation = new Paragraph("Affiliation Code: " + school.getAffiliationCode(), addressFont);
            affiliation.setAlignment(Element.ALIGN_CENTER);
            document.add(affiliation);
        }

        // Separator Line
        document.add(new Paragraph(" "));
        addSeparatorLine(document);
        document.add(new Paragraph(" "));
    }

    private void addChallanTitle(Document document, String session, int months) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA, 14, Font.BOLD, Color.BLACK);
        String titleStr = "FEE PAYMENT CHALLAN";
        if (months > 1) {
            titleStr += " - ADVANCE (" + months + " MONTHS)";
        }
        Paragraph title = new Paragraph(titleStr, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Font sessionFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.DARK_GRAY);
        Paragraph sessionPara = new Paragraph("Academic Session: " + session, sessionFont);
        sessionPara.setAlignment(Element.ALIGN_CENTER);
        document.add(sessionPara);

        document.add(new Paragraph(" "));
        addSeparatorLine(document);
        document.add(new Paragraph(" "));
    }

    private void addStudentDetails(Document document, Student student) throws DocumentException {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5f);
        table.setSpacingAfter(10f);

        // Row 1: Student Name and Class
        addDetailCell(table, "Student Name:", labelFont);
        addDetailCell(table,
                student.getFirstName() + " " + (student.getLastName() != null ? student.getLastName() : ""), valueFont);
        addDetailCell(table, "Class:", labelFont);
        String className = student.getCurrentClass() != null
                ? student.getCurrentClass().getName() + "-" + student.getCurrentClass().getSection()
                : "N/A";
        addDetailCell(table, className, valueFont);

        // Row 2: Admission No and Roll No
        addDetailCell(table, "Admission No:", labelFont);
        addDetailCell(table, student.getAdmissionNumber(), valueFont);
        addDetailCell(table, "Date:", labelFont);
        addDetailCell(table, LocalDate.now().format(DATE_FORMATTER), valueFont);

        document.add(table);
        addSeparatorLine(document);
        document.add(new Paragraph(" "));
    }

    private java.math.BigDecimal addFeeBreakdown(Document document, List<StudentFeeAssignment> assignments,
            int monthsToPay)
            throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLD, Color.WHITE);
        Font contentFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 3f, 1.2f, 0.8f, 1.5f });

        // Header Row
        PdfPCell headerCell1 = new PdfPCell(new Phrase("Fee Type", headerFont));
        headerCell1.setBackgroundColor(new Color(41, 128, 185));
        headerCell1.setPadding(8);
        table.addCell(headerCell1);

        PdfPCell headerCell2 = new PdfPCell(new Phrase("Frequency", headerFont));
        headerCell2.setBackgroundColor(new Color(41, 128, 185));
        headerCell2.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell2.setPadding(8);
        table.addCell(headerCell2);

        PdfPCell headerCell3 = new PdfPCell(new Phrase("Qty", headerFont));
        headerCell3.setBackgroundColor(new Color(41, 128, 185));
        headerCell3.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell3.setPadding(8);
        table.addCell(headerCell3);

        PdfPCell headerCell4 = new PdfPCell(new Phrase("Subtotal (₹)", headerFont));
        headerCell4.setBackgroundColor(new Color(41, 128, 185));
        headerCell4.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerCell4.setPadding(8);
        table.addCell(headerCell4);

        // Fee Rows
        java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
        for (StudentFeeAssignment assignment : assignments) {
            FeeStructure structure = feeStructureRepository.findById(assignment.getFeeStructureId())
                    .orElse(null);

            if (structure != null && structure.isActive()) {
                int multiplier = 1;
                if (structure.getFrequency() == FeeFrequency.MONTHLY) {
                    multiplier = monthsToPay;
                } else if (structure.getFrequency() == FeeFrequency.QUARTERLY) {
                    multiplier = (int) Math.ceil((double) monthsToPay / 3);
                } else if (structure.getFrequency() == FeeFrequency.HALF_YEARLY) {
                    multiplier = (int) Math.ceil((double) monthsToPay / 6);
                }

                java.math.BigDecimal subtotal = structure.getAmount()
                        .multiply(java.math.BigDecimal.valueOf(multiplier));

                // Fee Type
                table.addCell(createTableCell(structure.getFeeType().getName(), contentFont, Element.ALIGN_LEFT));
                // Frequency
                table.addCell(
                        createTableCell(formatFrequency(structure.getFrequency()), contentFont, Element.ALIGN_CENTER));
                // Qty (Months/Units)
                table.addCell(createTableCell(String.valueOf(multiplier), contentFont, Element.ALIGN_CENTER));
                // Amount
                table.addCell(createTableCell(formatIndianRupees(subtotal), contentFont, Element.ALIGN_RIGHT));

                totalAmount = totalAmount.add(subtotal);
            }
        }

        // Total Row
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLD);
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("Grand Total", totalFont));
        totalLabelCell.setColspan(3);
        totalLabelCell.setPadding(8);
        totalLabelCell.setBackgroundColor(new Color(236, 240, 241));
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalLabelCell);

        PdfPCell totalAmountCell = new PdfPCell(new Phrase(formatIndianRupees(totalAmount), totalFont));
        totalAmountCell.setPadding(8);
        totalAmountCell.setBackgroundColor(new Color(236, 240, 241));
        totalAmountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalAmountCell);

        document.add(table);
        document.add(new Paragraph(" "));

        return totalAmount;
    }

    private PdfPCell createTableCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBorderWidthBottom(0.5f);
        cell.setBorderColorBottom(Color.LIGHT_GRAY);
        return cell;
    }

    private void addPaymentDetails(Document document, String session, java.math.BigDecimal totalAmount)
            throws DocumentException {
        addSeparatorLine(document);
        document.add(new Paragraph(" "));

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font noteFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.ITALIC, Color.DARK_GRAY);

        // Due Date
        LocalDate dueDate = calculateDueDate(session);
        Paragraph dueDatePara = new Paragraph();
        dueDatePara.add(new Chunk("Due Date: ", labelFont));
        dueDatePara.add(new Chunk(dueDate.format(DATE_FORMATTER), valueFont));
        document.add(dueDatePara);

        // Late Fee Info (Generic note as it's policy driven now)
        Paragraph lateFeePara = new Paragraph();
        lateFeePara.add(new Chunk("Late Fee: ", labelFont));
        lateFeePara.add(new Chunk("Applicable as per school policy if paid after due date.", valueFont));
        document.add(lateFeePara);

        document.add(new Paragraph(" "));
        addSeparatorLine(document);
        document.add(new Paragraph(" "));

        // Payment Instructions
        Paragraph instructionsTitle = new Paragraph("Payment Instructions:", labelFont);
        document.add(instructionsTitle);

        Paragraph instruction1 = new Paragraph(
                "• Payment can be made at school office (9:00 AM - 3:00 PM, Monday to Friday)", noteFont);
        Paragraph instruction2 = new Paragraph("• Accepted modes: Cash, Cheque, Demand Draft, UPI", noteFont);
        Paragraph instruction3 = new Paragraph("• Please keep this challan for your records", noteFont);
        Paragraph instruction4 = new Paragraph("• For online payment, contact school office for bank details",
                noteFont);

        document.add(instruction1);
        document.add(instruction2);
        document.add(instruction3);
        document.add(instruction4);
    }

    private void addFooter(Document document, School school) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        addSeparatorLine(document);

        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, Color.GRAY);
        Paragraph footer = new Paragraph("This is a computer-generated challan. For queries, contact " +
                (school.getContactEmail() != null ? school.getContactEmail() : school.getContactNumber()), footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private void addSeparatorLine(Document document) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderWidthBottom(1f);
        cell.setBorderColorBottom(Color.DARK_GRAY);
        cell.setFixedHeight(1f);
        line.addCell(cell);
        document.add(line);
    }

    private void addDetailCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);
        table.addCell(cell);
    }

    /**
     * Format amount in Indian rupee format with ₹ symbol
     */
    private String formatIndianRupees(java.math.BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return formatter.format(amount).replace("₹", "₹ "); // Add space after symbol for readability
    }

    /**
     * Format frequency for display
     */
    private String formatFrequency(FeeFrequency frequency) {
        return switch (frequency) {
            case ONE_TIME -> "One Time";
            case MONTHLY -> "Monthly";
            case QUARTERLY -> "Quarterly";
            case HALF_YEARLY -> "Half-Yearly";
            case ANNUALLY -> "Annual";
        };
    }

    /**
     * Calculate due date based on session and current date
     * Indian schools typically follow April-March academic year
     */
    private LocalDate calculateDueDate(String session) {
        // Parse session "2024-25" -> start year 2024
        int startYear = Integer.parseInt(session.split("-")[0]);

        // Get current month
        LocalDate now = LocalDate.now();
        Month currentMonth = now.getMonth();

        // Due date is 5th of next month for monthly fees
        // For term fees, use standard term dates
        if (now.getMonthValue() >= 4) {
            // Current academic year
            return LocalDate.of(startYear, now.getMonthValue(), 5).plusMonths(1);
        } else {
            // Next calendar year but same academic year
            return LocalDate.of(startYear + 1, now.getMonthValue(), 5).plusMonths(1);
        }
    }

}
