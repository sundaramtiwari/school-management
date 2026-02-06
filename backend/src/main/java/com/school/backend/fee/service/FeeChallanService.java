package com.school.backend.fee.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.enums.FeeFrequency;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
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

    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentFeeAssignmentRepository assignmentRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final int LATE_FEE_PER_DAY = 50; // ₹50 per day
    private static final int GRACE_PERIOD_DAYS = 7;

    @Transactional(readOnly = true)
    public byte[] generateChallan(Long studentId, String session, Long schoolId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        if (!student.getSchoolId().equals(schoolId)) {
            throw new SecurityException("Access denied: Student does not belong to your school");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        // Get all fee assignments for this student and session
        List<StudentFeeAssignment> assignments = assignmentRepository.findByStudentIdAndSession(studentId, session);

        if (assignments.isEmpty()) {
            throw new IllegalArgumentException("No fee assignments found for student in session " + session);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);

            document.open();

            // Build the challan
            addSchoolHeader(document, school);
            addChallanTitle(document, session);
            addStudentDetails(document, student);
            int totalAmount = addFeeBreakdown(document, assignments);
            addPaymentDetails(document, session, totalAmount);
            addFooter(document, school);

            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate fee challan", e);
        }
    }

    private void addSchoolHeader(Document document, School school) throws DocumentException {
        // School Name (Large, Bold, Centered)
        Font schoolNameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
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

    private void addChallanTitle(Document document, String session) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
        Paragraph title = new Paragraph("FEE PAYMENT CHALLAN", titleFont);
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

    private int addFeeBreakdown(Document document, List<StudentFeeAssignment> assignments) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
        Font contentFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 3f, 1.5f, 1.5f });

        // Header Row
        PdfPCell headerCell1 = new PdfPCell(new Phrase("Fee Type", headerFont));
        headerCell1.setBackgroundColor(new Color(41, 128, 185)); // Professional blue
        headerCell1.setPadding(8);
        headerCell1.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(headerCell1);

        PdfPCell headerCell2 = new PdfPCell(new Phrase("Frequency", headerFont));
        headerCell2.setBackgroundColor(new Color(41, 128, 185));
        headerCell2.setPadding(8);
        headerCell2.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(headerCell2);

        PdfPCell headerCell3 = new PdfPCell(new Phrase("Amount (₹)", headerFont));
        headerCell3.setBackgroundColor(new Color(41, 128, 185));
        headerCell3.setPadding(8);
        headerCell3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(headerCell3);

        // Fee Rows
        int totalAmount = 0;
        for (StudentFeeAssignment assignment : assignments) {
            FeeStructure structure = feeStructureRepository.findById(assignment.getFeeStructureId())
                    .orElse(null);

            if (structure != null && structure.isActive()) {
                // Fee Type
                PdfPCell cell1 = new PdfPCell(new Phrase(structure.getFeeType().getName(), contentFont));
                cell1.setPadding(6);
                cell1.setBorder(Rectangle.NO_BORDER);
                cell1.setBorderWidthBottom(0.5f);
                cell1.setBorderColorBottom(Color.LIGHT_GRAY);
                table.addCell(cell1);

                // Frequency
                PdfPCell cell2 = new PdfPCell(new Phrase(formatFrequency(structure.getFrequency()), contentFont));
                cell2.setPadding(6);
                cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell2.setBorder(Rectangle.NO_BORDER);
                cell2.setBorderWidthBottom(0.5f);
                cell2.setBorderColorBottom(Color.LIGHT_GRAY);
                table.addCell(cell2);

                // Amount
                PdfPCell cell3 = new PdfPCell(new Phrase(formatIndianRupees(structure.getAmount()), contentFont));
                cell3.setPadding(6);
                cell3.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cell3.setBorder(Rectangle.NO_BORDER);
                cell3.setBorderWidthBottom(0.5f);
                cell3.setBorderColorBottom(Color.LIGHT_GRAY);
                table.addCell(cell3);

                totalAmount += structure.getAmount();
            }
        }

        // Total Row
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total Amount", totalFont));
        totalLabelCell.setColspan(2);
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

    private void addPaymentDetails(Document document, String session, int totalAmount) throws DocumentException {
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

        // Late Fee Info
        Paragraph lateFeePara = new Paragraph();
        lateFeePara.add(new Chunk("Late Fee: ", labelFont));
        lateFeePara.add(new Chunk(
                "₹ " + LATE_FEE_PER_DAY + " per day after due date (Grace period: " + GRACE_PERIOD_DAYS + " days)",
                valueFont));
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
     * Example: ₹ 1,50,000
     */
    private String formatIndianRupees(int amount) {
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

    /**
     * Calculate late fee based on days overdue
     */
    public int calculateLateFee(LocalDate dueDate, int baseFee) {
        LocalDate now = LocalDate.now();

        if (now.isAfter(dueDate.plusDays(GRACE_PERIOD_DAYS))) {
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(dueDate.plusDays(GRACE_PERIOD_DAYS), now);
            int lateFee = (int) (daysOverdue * LATE_FEE_PER_DAY);

            // Cap late fee at 10% of base fee
            int maxLateFee = (int) (baseFee * 0.10);
            return Math.min(lateFee, maxLateFee);
        }

        return 0;
    }
}
