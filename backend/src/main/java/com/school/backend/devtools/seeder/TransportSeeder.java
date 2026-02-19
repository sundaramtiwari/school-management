package com.school.backend.devtools.seeder;

import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.fee.enums.FeeFrequency;
import com.school.backend.transport.entity.PickupPoint;
import com.school.backend.transport.entity.TransportEnrollment;
import com.school.backend.transport.entity.TransportRoute;
import com.school.backend.transport.repository.PickupPointRepository;
import com.school.backend.transport.repository.TransportEnrollmentRepository;
import com.school.backend.transport.repository.TransportRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TransportSeeder {

    private final TransportRouteRepository transportRouteRepository;
    private final PickupPointRepository pickupPointRepository;
    private final TransportEnrollmentRepository transportEnrollmentRepository;

    @Transactional
    public Result seed(Random random, SessionSeeder.Result sessionResult, StudentSeeder.Result studentResult) {
        List<TransportRoute> routesToSave = new ArrayList<>();
        for (Long schoolId : studentResult.studentsBySchool().keySet()) {
            int routeCount = 5 + random.nextInt(4);
            for (int i = 1; i <= routeCount; i++) {
                routesToSave.add(
                        TransportRoute.builder()
                                .schoolId(schoolId)
                                .name("Route " + i)
                                .description("Seeded route " + i)
                                .capacity(120)
                                .currentStrength(0)
                                .active(true)
                                .build()
                );
            }
        }
        List<TransportRoute> routes = transportRouteRepository.saveAll(routesToSave);

        Map<Long, List<TransportRoute>> routesBySchool = new LinkedHashMap<>();
        for (TransportRoute route : routes) {
            routesBySchool.computeIfAbsent(route.getSchoolId(), k -> new ArrayList<>()).add(route);
        }

        List<PickupPoint> pointsToSave = new ArrayList<>(routes.size() * 2);
        for (TransportRoute route : routes) {
            for (int i = 1; i <= 2; i++) {
                pointsToSave.add(
                        PickupPoint.builder()
                                .schoolId(route.getSchoolId())
                                .route(route)
                                .name(route.getName() + " - Stop " + i)
                                .amount(BigDecimal.valueOf(900 + (i * 150L)).setScale(2, RoundingMode.HALF_UP))
                                .frequency(FeeFrequency.MONTHLY)
                                .build()
                );
            }
        }
        List<PickupPoint> pickupPoints = pickupPointRepository.saveAll(pointsToSave);

        Map<Long, List<PickupPoint>> pickupPointsBySchool = new LinkedHashMap<>();
        for (PickupPoint pickupPoint : pickupPoints) {
            pickupPointsBySchool.computeIfAbsent(pickupPoint.getSchoolId(), k -> new ArrayList<>()).add(pickupPoint);
        }

        List<TransportEnrollment> enrollmentsToSave = new ArrayList<>();
        Map<Long, Long> transportSessionByStudentId = new LinkedHashMap<>();
        Map<Long, Integer> strengthByRoute = new LinkedHashMap<>();
        Map<Long, Set<Long>> sessionsByStudent = studentResult.allEnrollments().stream()
                .collect(Collectors.groupingBy(
                        StudentEnrollment::getStudentId,
                        LinkedHashMap::new,
                        Collectors.mapping(StudentEnrollment::getSessionId, Collectors.toSet())
                ));

        for (Map.Entry<Long, List<Student>> entry : studentResult.studentsBySchool().entrySet()) {
            Long schoolId = entry.getKey();
            List<Student> students = entry.getValue();
            int targetTransportCount = (int) Math.round(students.size() * 0.30);
            List<PickupPoint> schoolPoints = pickupPointsBySchool.getOrDefault(schoolId, List.of());
            SessionSeeder.SessionTriplet sessions = sessionResult.sessionsBySchool().get(schoolId);

            int assigned = 0;
            for (int i = 0; i < students.size() && assigned < targetTransportCount; i++) {
                Student student = students.get(i);
                if (i % 10 >= 3) {
                    continue;
                }

                Long preferredSessionId = hasEnrollment(sessionsByStudent, student.getId(), sessions.active().getId())
                        ? sessions.active().getId()
                        : sessions.completed().getId();

                PickupPoint pickupPoint = schoolPoints.get(assigned % schoolPoints.size());
                enrollmentsToSave.add(
                        TransportEnrollment.builder()
                                .schoolId(schoolId)
                                .studentId(student.getId())
                                .pickupPoint(pickupPoint)
                                .sessionId(preferredSessionId)
                                .active(true)
                                .build()
                );
                transportSessionByStudentId.put(student.getId(), preferredSessionId);
                strengthByRoute.merge(pickupPoint.getRoute().getId(), 1, Integer::sum);
                assigned++;
            }
        }

        for (TransportRoute route : routes) {
            route.setCurrentStrength(strengthByRoute.getOrDefault(route.getId(), 0));
        }
        transportRouteRepository.saveAll(routes);
        BatchSaveUtil.saveInBatches(enrollmentsToSave, 1_000, transportEnrollmentRepository::saveAll);

        return new Result(transportSessionByStudentId);
    }

    private boolean hasEnrollment(Map<Long, Set<Long>> sessionsByStudent, Long studentId, Long sessionId) {
        Set<Long> sessions = sessionsByStudent.get(studentId);
        return sessions != null && sessions.contains(sessionId);
    }

    public record Result(Map<Long, Long> transportSessionByStudentId) {
    }
}
