-- Domain Integrity Hardening (Safe Mode)
-- This file is informational only. Do NOT auto-apply in application startup.
-- Run detection queries first, clean data, then apply index in a controlled migration.

-- 1) Detect violations: multiple active enrollments per (student_id, session_id)
SELECT student_id, session_id, COUNT(*) AS active_count
FROM student_enrollments
WHERE active = true
GROUP BY student_id, session_id
HAVING COUNT(*) > 1;

-- Suggested migration (PostgreSQL) after data cleanup:
-- CREATE UNIQUE INDEX CONCURRENTLY ux_student_enrollment_active_unique
--   ON student_enrollments (student_id, session_id)
--   WHERE active = true;

-- 2) Optional check: multiple active funding arrangements per (student_id, session_id)
SELECT student_id, session_id, COUNT(*) AS active_count
FROM student_funding_arrangements
WHERE active = true
GROUP BY student_id, session_id
HAVING COUNT(*) > 1;
