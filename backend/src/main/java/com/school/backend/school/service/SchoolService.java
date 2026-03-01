package com.school.backend.school.service;

import com.school.backend.common.enums.UserRole;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.school.dto.SchoolDto;
import com.school.backend.school.dto.SchoolOnboardingRequest;
import com.school.backend.school.entity.School;
import com.school.backend.school.mapper.SchoolMapper;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.user.entity.User;
import com.school.backend.user.repository.UserRepository;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create school from DTO and return saved DTO
     */
    public SchoolDto create(SchoolDto dto) {
        log.info("Creating school via DTO name={}", dto.getName());
        School entity = SchoolMapper.toEntity(dto);
        // ensure ID is not set by client
        entity.setId(null);
        School saved = schoolRepository.save(entity);
        return SchoolMapper.toDto(saved);
    }

    /**
     * Create School AND Initial Admin User
     */
    @Transactional
    public SchoolDto createSchoolWithAdmin(SchoolOnboardingRequest req) {
        log.info("Starting school onboarding for schoolName={} adminEmail={}", req.getName(), req.getAdminEmail());
        // 1. Check if user already exists
        if (userRepository.existsByEmail(req.getAdminEmail())) {
            log.warn("School onboarding rejected: admin email already exists email={}", req.getAdminEmail());
            throw new IllegalArgumentException("User with email " + req.getAdminEmail() + " already exists");
        }

        // 2. Create School
        School school = new School();
        school.setName(req.getName());
        school.setDisplayName(req.getDisplayName());
        school.setBoard(req.getBoard());
        school.setMedium(req.getMedium());
        school.setCity(req.getCity());
        school.setState(req.getState());
        school.setContactEmail(req.getContactEmail());
        school.setContactNumber(req.getContactNumber());
        school.setAddress(req.getAddress());
        school.setPincode(req.getPincode());
        school.setWebsite(req.getWebsite());
        school.setDescription(req.getDescription());
        school.setAffiliationCode(req.getAffiliationCode());

        // Auto-generate schoolCode if not provided
        if (req.getSchoolCode() == null || req.getSchoolCode().isBlank()) {
            school.setSchoolCode(generateSchoolCode());
        } else {
            school.setSchoolCode(req.getSchoolCode());
        }

        school = schoolRepository.save(school);
        log.info("School created with id={} code={}", school.getId(), school.getSchoolCode());

        // 3. Create Admin User
        User user = new User();
        user.setEmail(req.getAdminEmail());
        user.setFullName(req.getAdminName());
        user.setPasswordHash(passwordEncoder.encode(req.getAdminPassword()));
        user.setRole(UserRole.SCHOOL_ADMIN);
        user.setSchool(school);

        userRepository.save(user);
        log.info("Initial school admin user created for schoolId={} email={}", school.getId(), req.getAdminEmail());

        return SchoolMapper.toDto(school);
    }

    /**
     * Paginated listing returning Page<SchoolDto>
     */
    public Page<SchoolDto> listSchools(Pageable pageable) {
        log.debug("Listing schools for role={}", SecurityUtil.role());
        if (SecurityUtil.role() == UserRole.SCHOOL_ADMIN) {
            Long schoolId = SecurityUtil.schoolId();
            School school = schoolRepository.findById(schoolId)
                    .orElseThrow(() -> new ResourceNotFoundException("School not found for current user"));
            return new org.springframework.data.domain.PageImpl<>(List.of(SchoolMapper.toDto(school)), pageable, 1);
        }
        return schoolRepository.findAll(pageable)
                .map(SchoolMapper::toDto);
    }

    /**
     * Non-paginated listing (kept for compatibility)
     */
    public List<SchoolDto> getAll() {
        return SchoolMapper.toDtos(schoolRepository.findAll());
    }

    public SchoolDto getByCode(String code) {
        log.debug("Fetching school by code={}", code);
        School school = schoolRepository.findBySchoolCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with code: " + code));

        if (SecurityUtil.role() == UserRole.SCHOOL_ADMIN) {
            Long mySchoolId = SecurityUtil.schoolId();
            if (!school.getId().equals(mySchoolId)) {
                log.warn("Access denied for school-by-code lookup. requestedCode={} userSchoolId={}", code, mySchoolId);
                // For security, strictly forbid or just return as if not found/unauthorized
                // Spring Security usually handles 403 if we threw AccessDeniedException
                // but here we are in service layer.
                throw new AccessDeniedException("Access denied to other school");
            }
        }

        return SchoolMapper.toDto(school);
    }

    public SchoolDto getById(Long id) {
        log.debug("Fetching school by id={}", id);
        // Security check for SCHOOL_ADMIN
        if (SecurityUtil.role() == UserRole.SCHOOL_ADMIN) {
            Long mySchoolId = SecurityUtil.schoolId();
            if (!id.equals(mySchoolId)) {
                log.warn("Access denied for school-by-id lookup. requestedId={} userSchoolId={}", id, mySchoolId);
                throw new AccessDeniedException("Access denied to other school");
            }
        }

        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with id: " + id));
        return SchoolMapper.toDto(school);
    }

    /**
     * Partial update: copy only non-null fields from dto to entity (PATCH
     * semantics).
     */
    public SchoolDto updateByCode(String code, SchoolDto dto) {
        log.info("Updating school by code={}", code);
        School entity = schoolRepository.findBySchoolCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with code: " + code));
        SchoolMapper.updateFromDto(dto, entity);
        School saved = schoolRepository.save(entity);
        return SchoolMapper.toDto(saved);
    }

    /**
     * Full replace (PUT semantics) - overwrite entity fields from DTO.
     * This preserves the same database id but replaces fields.
     */
    public SchoolDto replaceByCode(String code, SchoolDto dto) {
        log.info("Replacing school by code={}", code);
        School existing = schoolRepository.findBySchoolCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with code: " + code));

        // Create a new entity from DTO, but preserve DB id and createdAt/other
        // BaseEntity fields
        School replacement = SchoolMapper.toEntity(dto);
        replacement.setId(existing.getId()); // important: keep PK
        // Optionally: copy audit fields from existing if you want to preserve them.
        // Save replacement (this will act as update since id is present)
        School saved = schoolRepository.save(replacement);
        return SchoolMapper.toDto(saved);
    }

    /**
     * Generates a unique school code in format SCH001, SCH002, etc.
     */
    private String generateSchoolCode() {
        long count = schoolRepository.count();
        String code;
        int attempts = 0;
        do {
            code = String.format("SCH%03d", count + 1 + attempts);
            attempts++;
        } while (schoolRepository.findBySchoolCode(code).isPresent() && attempts < 1000);

        if (attempts >= 1000) {
            throw new IllegalStateException("Unable to generate unique school code");
        }
        return code;
    }

}
