package com.school.backend.fee.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.fee.entity.FeePayment;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class FeeReceiptService {

    private final FeePaymentRepository paymentRepository;
    private final SchoolRepository schoolRepository;

    @Transactional(readOnly = true)
    public byte[] generateReceipt(Long paymentId) {
        FeePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        School school = schoolRepository.findById(payment.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);

            document.open();

            // 1. Header (School Name)
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph schoolName = new Paragraph(school.getDisplayName(), headerFont);
            schoolName.setAlignment(Element.ALIGN_CENTER);
            document.add(schoolName);

            Paragraph address = new Paragraph(school.getCity() + ", " + school.getState());
            address.setAlignment(Element.ALIGN_CENTER);
            address.setSpacingAfter(20);
            document.add(address);

            // 2. Receipt Title
            Paragraph title = new Paragraph("FEE RECEIPT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // 3. Details Table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(10);

            addCell(table, "Receipt No:", String.valueOf(payment.getId()));
            addCell(table, "Date:", payment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")));
            addCell(table, "Student ID:", String.valueOf(payment.getStudentId())); // In real app, fetch Student Name
            addCell(table, "Payment Mode:", payment.getMode());

            document.add(table);

            // 4. Amount
            Paragraph amount = new Paragraph("Amount Paid: INR " + payment.getAmountPaid(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            amount.setAlignment(Element.ALIGN_RIGHT);
            document.add(amount);

            Paragraph remarks = new Paragraph(
                    "Remarks: " + (payment.getRemarks() != null ? payment.getRemarks() : "-"));
            document.add(remarks);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating receipt", e);
        }
    }

    private void addCell(PdfPTable table, String label, String value) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase(value));
        c2.setBorder(Rectangle.NO_BORDER);
        table.addCell(c1);
        table.addCell(c2);
    }
}
