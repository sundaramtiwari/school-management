package com.school.backend.fee.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.FontFactory;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.entity.FeePayment;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FeeReceiptService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private final FeePaymentRepository paymentRepository;
    private final SchoolRepository schoolRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public byte[] generateReceipt(Long paymentId) {
        FeePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        School school = schoolRepository.findById(payment.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        Student student = studentRepository.findById(payment.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);

            document.open();

            // 1. School Header
            addSchoolHeader(document, school);

            // 2. Receipt Title
            addReceiptTitle(document, paymentId);

            // 3. Student & Payment Details
            addDetailsTable(document, student, payment);

            // 4. Amount Section (Highlighted)
            addAmountSection(document, payment);

            // 5. Footer
            addFooter(document);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating receipt", e);
        }
    }

    private void addSchoolHeader(Document document, School school) throws DocumentException {
        Font schoolNameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
        Paragraph schoolName = new Paragraph(
                school.getDisplayName() != null ? school.getDisplayName() : school.getName(),
                schoolNameFont);
        schoolName.setAlignment(Element.ALIGN_CENTER);
        document.add(schoolName);

        Font addressFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
        if (school.getAddress() != null) {
            Paragraph address = new Paragraph(
                    school.getAddress() + ", " + school.getCity() + ", " + school.getState() + " - "
                            + school.getPincode(),
                    addressFont);
            address.setAlignment(Element.ALIGN_CENTER);
            document.add(address);
        }

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

        document.add(new Paragraph(" "));
        addSeparatorLine(document);
        document.add(new Paragraph(" "));
    }

    private void addReceiptTitle(Document document, Long receiptId) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
        Paragraph title = new Paragraph("FEE PAYMENT RECEIPT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Font receiptNoFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        Paragraph receiptNo = new Paragraph("Receipt No: " + String.format("REC-%06d", receiptId), receiptNoFont);
        receiptNo.setAlignment(Element.ALIGN_CENTER);
        document.add(receiptNo);

        document.add(new Paragraph(" "));
        addSeparatorLine(document);
        document.add(new Paragraph(" "));
    }

    private void addDetailsTable(Document document, Student student, FeePayment payment) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        // Row 1: Student Name and Admission Number
        addDetailCell(table, "Student Name:", labelFont);
        addDetailCell(table, student.getFirstName() + " " +
                (student.getLastName() != null ? student.getLastName() : ""), valueFont);
        addDetailCell(table, "Admission No:", labelFont);
        addDetailCell(table, student.getAdmissionNumber(), valueFont);

        // Row 2: Class and Date
        addDetailCell(table, "Class:", labelFont);
        String className = student.getCurrentClass() != null
                ? student.getCurrentClass().getName() + "-" + student.getCurrentClass().getSection()
                : "N/A";
        addDetailCell(table, className, valueFont);
        addDetailCell(table, "Payment Date:", labelFont);
        addDetailCell(table, payment.getPaymentDate().format(DATE_FORMATTER), valueFont);

        // Row 3: Payment Mode and Transaction Ref (if applicable)
        addDetailCell(table, "Payment Mode:", labelFont);
        addDetailCell(table, formatPaymentMode(payment.getMode()), valueFont);
        addDetailCell(table, "Reference:", labelFont);
        addDetailCell(table, payment.getTransactionReference() != null
                ? payment.getTransactionReference()
                : "N/A", valueFont);

        document.add(table);
        addSeparatorLine(document);
        document.add(new Paragraph(" "));
    }

    private void addAmountSection(Document document, FeePayment payment) throws DocumentException {
        // Amount Paid (Highlighted Box)
        PdfPTable amountTable = new PdfPTable(1);
        amountTable.setWidthPercentage(100);

        Font amountFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        PdfPCell amountCell = new PdfPCell(new Phrase(
                "Amount Paid: " + formatIndianRupees(payment.getAmountPaid()), amountFont));
        amountCell.setBackgroundColor(new Color(236, 240, 241));
        amountCell.setPadding(12);
        amountCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        amountCell.setBorder(Rectangle.BOX);
        amountCell.setBorderWidth(1.5f);
        amountCell.setBorderColor(new Color(52, 152, 219));
        amountTable.addCell(amountCell);

        document.add(amountTable);
        document.add(new Paragraph(" "));

        // Remarks (if any)
        if (payment.getRemarks() != null && !payment.getRemarks().isEmpty()) {
            Font remarksFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.ITALIC, Color.DARK_GRAY);
            Paragraph remarks = new Paragraph("Remarks: " + payment.getRemarks(), remarksFont);
            document.add(remarks);
            document.add(new Paragraph(" "));
        }
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        // Signature Area
        PdfPTable signatureTable = new PdfPTable(2);
        signatureTable.setWidthPercentage(100);

        Font signFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);

        PdfPCell receivedByCell = new PdfPCell(new Phrase("Received By:\n\n\n_________________", signFont));
        receivedByCell.setBorder(Rectangle.NO_BORDER);
        receivedByCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        signatureTable.addCell(receivedByCell);

        PdfPCell parentSignCell = new PdfPCell(new Phrase("Parent/Guardian Sign:\n\n\n_________________", signFont));
        parentSignCell.setBorder(Rectangle.NO_BORDER);
        parentSignCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        signatureTable.addCell(parentSignCell);

        document.add(signatureTable);
        document.add(new Paragraph(" "));

        addSeparatorLine(document);

        // Computer Generated Note
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.ITALIC, Color.GRAY);
        Paragraph footer = new Paragraph(
                "This is a computer-generated receipt and does not require a signature. " +
                        "For queries, please contact the school office.",
                footerFont);
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

    private String formatIndianRupees(java.math.BigDecimal amount) {
        if (amount == null)
            return "₹ 0.00";
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return formatter.format(amount).replace("₹", "₹ ");
    }

    private String formatPaymentMode(String mode) {
        return switch (mode.toUpperCase()) {
            case "CASH" -> "Cash";
            case "ONLINE", "UPI" -> "Online/UPI";
            case "CHEQUE" -> "Cheque";
            case "CARD" -> "Card";
            default -> mode;
        };
    }
}
