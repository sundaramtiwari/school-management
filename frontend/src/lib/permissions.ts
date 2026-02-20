export function normalizeRole(role?: string | null): string {
  return role?.toUpperCase() || "";
}

export function isPlatformAdminRole(role?: string | null): boolean {
  const normalizedRole = normalizeRole(role);
  return normalizedRole === "SUPER_ADMIN" || normalizedRole === "PLATFORM_ADMIN";
}

export function canAddStudent(role?: string | null): boolean {
  const normalizedRole = normalizeRole(role);
  return normalizedRole === "SCHOOL_ADMIN" || normalizedRole === "SUPER_ADMIN" || normalizedRole === "PLATFORM_ADMIN";
}

export function canPromoteStudents(role?: string | null): boolean {
  return canAddStudent(role);
}

export function canEditStudent(role?: string | null): boolean {
  const normalizedRole = normalizeRole(role);
  return normalizedRole === "SUPER_ADMIN" || normalizedRole === "ACCOUNTANT";
}

export function canCollectFees(role?: string | null): boolean {
  const normalizedRole = normalizeRole(role);
  return normalizedRole === "ACCOUNTANT" || normalizedRole === "SCHOOL_ADMIN" || normalizedRole === "SUPER_ADMIN";
}

export function canManageFees(role?: string | null): boolean {
  return canCollectFees(role);
}

export function canMutateFinance(role?: string | null): boolean {
  return canCollectFees(role);
}

export function canCommitAttendance(role?: string | null): boolean {
  const normalizedRole = normalizeRole(role);
  return normalizedRole === "SCHOOL_ADMIN" || normalizedRole === "TEACHER" || isPlatformAdminRole(normalizedRole);
}

export function canModifyAttendance(role?: string | null): boolean {
  const normalizedRole = normalizeRole(role);
  return normalizedRole === "SCHOOL_ADMIN" || isPlatformAdminRole(normalizedRole);
}
