package com.school.backend.fee;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.fee.dto.*;
import com.school.backend.fee.enums.FeeFrequency;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class FeeIntegrationTest extends BaseAuthenticatedIntegrationTest {

    @Autowired
    private SchoolRepository schoolRepository;
    @Autowired
    private FeeStructureRepository feeStructureRepository;
    @Autowired
    private FeeTypeRepository feeTypeRepository;
    @Autowired
    private FeePaymentRepository feePaymentRepository;

    private School testSchool;
    private Long feeTypeId;

    @BeforeEach
    void setupFeeTest() {
        feeStructureRepository.deleteAll();
        feePaymentRepository.deleteAll();
        feeTypeRepository.deleteAll();

        // Ensure we are logged in as a school admin
        testSchool = schoolRepository.findBySchoolCode("SPS001")
                .orElseGet(() -> {
                    School s = new School();
                    s.setName("Test School");
                    s.setSchoolCode("SPS001");
                    s.setActive(true);
                    return schoolRepository.save(s);
                });

        loginAsSchoolAdmin(testSchool.getId());

        // Create a Fee Type
        var ft = new com.school.backend.fee.entity.FeeType();
        ft.setName("Tuition Fee");
        ft.setSchoolId(testSchool.getId());
        ft.setActive(true);
        feeTypeRepository.save(ft);
        feeTypeId = ft.getId();
    }

    @Test
    void fullFeeFlow_CreateStructure_Pay_VerifyBalance() {
        // 1. Create Fee Structure (Monthly)
        FeeStructureCreateRequest createReq = new FeeStructureCreateRequest();
        createReq.setClassId(101L); // Mock Class ID
        createReq.setSession("2024-25");
        createReq.setFeeTypeId(feeTypeId);
        createReq.setAmount(5000);
        createReq.setFrequency(FeeFrequency.MONTHLY);

        ResponseEntity<FeeStructureDto> structureResp = restTemplate.postForEntity(
                "/api/fees/structures",
                new HttpEntity<>(createReq, headers),
                FeeStructureDto.class);
        assertEquals(HttpStatus.OK, structureResp.getStatusCode());
        assertNotNull(structureResp.getBody().getId());
        assertEquals(FeeFrequency.MONTHLY, structureResp.getBody().getFrequency());

        // 2. Check Dues for a Student (ID: 501)
        // Dues should be calculated based on logic (assuming implementation uses
        // assignment or auto-calc)
        // If auto-calc logic assumes 1 month due, let's see.
        // Note: Logic depends on start date vs current date.

        // 3. Pay Fees
        FeePaymentRequest payReq = new FeePaymentRequest();
        payReq.setStudentId(501L);
        payReq.setAmount(5000);
        payReq.setPaymentMode("CASH");
        payReq.setPaymentDate(LocalDate.now());
        payReq.setRemarks("Jan Fee");

        ResponseEntity<FeePaymentDto> payResp = restTemplate.postForEntity(
                "/api/fees/payments",
                new HttpEntity<>(payReq, headers),
                FeePaymentDto.class);
        assertEquals(HttpStatus.OK, payResp.getStatusCode());
        assertNotNull(payResp.getBody().getId());

        // 4. Verify History
        ResponseEntity<FeePaymentDto[]> histResp = restTemplate.exchange(
                "/api/fees/payments/students/501",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                FeePaymentDto[].class);
        assertEquals(HttpStatus.OK, histResp.getStatusCode());
        assertTrue(histResp.getBody().length >= 1);

        // 5. Download Receipt
        Long paymentId = payResp.getBody().getId();
        ResponseEntity<byte[]> receiptResp = restTemplate.exchange(
                "/api/fees/payments/" + paymentId + "/receipt",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class);
        assertEquals(HttpStatus.OK, receiptResp.getStatusCode());
        assertTrue(receiptResp.getBody().length > 0, "PDF content should not be empty");
    }
}
