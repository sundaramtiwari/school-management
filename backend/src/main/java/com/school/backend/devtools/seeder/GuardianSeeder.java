package com.school.backend.devtools.seeder;

import com.school.backend.core.guardian.entity.Guardian;
import com.school.backend.core.guardian.repository.GuardianRepository;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.entity.StudentGuardian;
import com.school.backend.core.student.repository.StudentGuardianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GuardianSeeder {

    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;

    @Transactional
    public void seed(StudentSeeder.Result studentResult) {
        List<Guardian> guardiansToSave = new ArrayList<>();
        List<GuardianLinkPlan> linkPlans = new ArrayList<>();

        for (Map.Entry<Long, List<Student>> entry : studentResult.studentsBySchool().entrySet()) {
            Long schoolId = entry.getKey();
            List<Student> students = entry.getValue();
            Map<String, Guardian> guardiansByContact = new LinkedHashMap<>();

            String previousPrimaryContact = null;
            for (int i = 0; i < students.size(); i++) {
                Student student = students.get(i);
                boolean singleGuardian = (i % 5 == 0);
                boolean sharedPrimary = (i % 10 == 0) && previousPrimaryContact != null;

                String primaryContact = sharedPrimary
                        ? previousPrimaryContact
                        : buildContact(schoolId, i, 1);

                String secondaryContact = buildContact(schoolId, i, 2);

                Guardian primaryGuardian = guardiansByContact.get(primaryContact);
                if (primaryGuardian == null) {
                    primaryGuardian = Guardian.builder()
                            .name(student.getFirstName() + " " + student.getLastName() + " Parent")
                            .relation("FATHER")
                            .contactNumber(primaryContact)
                            .email("parent" + i + "@guardian.local")
                            .address(student.getAddress())
                            .occupation("Service")
                            .qualification("Graduate")
                            .whatsappEnabled(true)
                            .active(true)
                            .build();
                    primaryGuardian.setSchoolId(schoolId);
                    guardiansByContact.put(primaryContact, primaryGuardian);
                    guardiansToSave.add(primaryGuardian);
                }

                linkPlans.add(new GuardianLinkPlan(schoolId, student.getId(), primaryContact, true));

                if (!singleGuardian) {
                    Guardian secondaryGuardian = guardiansByContact.get(secondaryContact);
                    if (secondaryGuardian == null) {
                        secondaryGuardian = Guardian.builder()
                                .name(student.getFirstName() + " " + student.getLastName() + " Guardian")
                                .relation("MOTHER")
                                .contactNumber(secondaryContact)
                                .email("guardian" + i + "@guardian.local")
                                .address(student.getAddress())
                                .occupation("Teacher")
                                .qualification("Graduate")
                                .whatsappEnabled(true)
                                .active(true)
                                .build();
                        secondaryGuardian.setSchoolId(schoolId);
                        guardiansByContact.put(secondaryContact, secondaryGuardian);
                        guardiansToSave.add(secondaryGuardian);
                    }
                    linkPlans.add(new GuardianLinkPlan(schoolId, student.getId(), secondaryContact, false));
                }

                previousPrimaryContact = primaryContact;
            }
        }

        List<Guardian> savedGuardians = guardianRepository.saveAll(guardiansToSave);
        Map<String, Long> guardianIdBySchoolAndContact = new LinkedHashMap<>();
        for (Guardian guardian : savedGuardians) {
            guardianIdBySchoolAndContact.put(key(guardian.getSchoolId(), guardian.getContactNumber()), guardian.getId());
        }

        List<StudentGuardian> links = new ArrayList<>(linkPlans.size());
        for (GuardianLinkPlan plan : linkPlans) {
            Long guardianId = guardianIdBySchoolAndContact.get(key(plan.schoolId(), plan.contactNumber()));
            StudentGuardian link = StudentGuardian.builder()
                    .studentId(plan.studentId())
                    .guardianId(guardianId)
                    .primaryGuardian(plan.primary())
                    .build();
            link.setSchoolId(plan.schoolId());
            links.add(link);
        }
        BatchSaveUtil.saveInBatches(links, 1_000, studentGuardianRepository::saveAll);
    }

    private String buildContact(Long schoolId, int studentIndex, int guardianOrder) {
        long raw = schoolId * 100000L + studentIndex * 10L + guardianOrder;
        return String.format("9%09d", raw % 1_000_000_000L);
    }

    private String key(Long schoolId, String contactNumber) {
        return schoolId + "::" + contactNumber;
    }

    private record GuardianLinkPlan(
            Long schoolId,
            Long studentId,
            String contactNumber,
            boolean primary
    ) {
    }
}
