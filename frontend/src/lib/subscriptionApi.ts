import { api } from "./api";
import { platformApi } from "./platformApi";

export type UsageWarningLevel = "NONE" | "WARNING" | "CRITICAL";
export type ExpiryWarningLevel = "NONE" | "WARNING_30" | "CRITICAL_7" | "EXPIRED";
export type SubscriptionStatus = "TRIAL" | "ACTIVE" | "PAST_DUE" | "SUSPENDED" | "NO_PLAN";
export type PaymentType = "PAYMENT" | "UPGRADE_PRORATION";
export type SubscriptionEventType = "TRIAL_STARTED" | "ACTIVATED" | "TRIAL_EXTENDED" | "SUBSCRIPTION_EXTENDED" | "UPGRADED" | "DOWNGRADED" | "SUSPENDED" | "REACTIVATED" | "EXPIRED";

export type SubscriptionPaymentDto = {
    id: number;
    subscriptionId: number;
    amount: number;
    type: PaymentType;
    paymentDate: string;
    referenceNumber: string;
    notes?: string;
    recordedByUserId: number;
    createdAt?: string;
};

export type UpgradePlanResponse = {
    subscription: SubscriptionDto;
    prorationPayment: SubscriptionPaymentDto;
    proratedAmount: number;
};

export type PricingPlanDto = {
    id: number;
    name: string;
    description?: string;
    yearlyPrice: number;
    studentCap: number;
    trialDaysDefault: number;
    gracePeriodDaysDefault: number;
    warningThresholdPercent: number;
    criticalThresholdPercent: number;
    active: boolean;
    createdAt?: string;
    updatedAt?: string;
};

export type SubscriptionDto = {
    id: number;
    schoolId: number;
    schoolName?: string;
    pricingPlanId: number;
    pricingPlanName: string;
    status: SubscriptionStatus;
    startDate: string | null;
    trialEndDate: string | null;
    expiryDate: string | null;
    graceEndDate: string | null;
    gracePeriodDays: number;
    version: number;
    createdAt?: string;
    updatedAt?: string;
};

export type AdminSubscriptionUsageDto = {
    subscriptionId: number;
    planName: string;
    subscriptionStatus: SubscriptionStatus;
    studentCap: number;
    activeStudents: number;
    usagePercent: number;
    expiryDate: string | null;
    graceEndDate: string | null;
    daysToExpiry: number;
    expiryWarningLevel: ExpiryWarningLevel;
};

export type SubscriptionAccessStatusDto = {
    subscriptionStatus: SubscriptionStatus | null;
    usagePercent: number;
    usageWarningLevel: UsageWarningLevel;
    daysToExpiry: number;
    expiryWarningLevel: ExpiryWarningLevel;
    schoolActive: boolean;
};

export type SubscriptionEventDto = {
    id: number;
    subscriptionId: number;
    type: SubscriptionEventType;
    daysAdded: number | null;
    previousExpiryDate: string | null;
    newExpiryDate: string | null;
    previousStatus: SubscriptionStatus | null;
    newStatus: SubscriptionStatus | null;
    reason: string | null;
    performedByUserId: number;
    performedBy?: string;
    createdAt: string;
};

// ---------------------------------------------------------
// PLATFORM API — no tenant headers, no X-School-Id
// Used by /platform/* pages
// ---------------------------------------------------------
export const pricingPlanApi = {
    list: () => platformApi.get<PricingPlanDto[]>("/api/pricing-plans"),
    get: (id: number) => platformApi.get<PricingPlanDto>(`/api/pricing-plans/${id}`),
    create: (payload: Partial<PricingPlanDto>) => platformApi.post<PricingPlanDto>("/api/pricing-plans", payload),
    update: (id: number, payload: Partial<PricingPlanDto>) => platformApi.put<PricingPlanDto>(`/api/pricing-plans/${id}`, payload),
    deactivate: (id: number) => platformApi.patch(`/api/pricing-plans/${id}/deactivate`),
    delete: (id: number) => platformApi.delete(`/api/pricing-plans/${id}`),
};

// Platform-scoped subscription management (no school header)
export const platformSubscriptionApi = {
    getBySchool: (schoolId: number) => platformApi.get<SubscriptionDto>(`/api/subscriptions/school/${schoolId}`),
    createTrial: (payload: any) => platformApi.post<SubscriptionDto>("/api/subscriptions/trial", payload),
    activate: (id: number, payload: any) => platformApi.post(`/api/subscriptions/${id}/activate`, payload),
    recordPayment: (id: number, payload: any) => platformApi.post(`/api/subscriptions/${id}/payments`, payload),
    upgrade: (id: number, payload: any) => platformApi.post<UpgradePlanResponse>(`/api/subscriptions/${id}/upgrade`, payload),
    upgradePreview: (id: number, newPlanId: number) => platformApi.get<UpgradePlanResponse>(`/api/subscriptions/${id}/upgrade/preview?newPlanId=${newPlanId}`),
    downgrade: (id: number, payload: any) => platformApi.post<SubscriptionDto>(`/api/subscriptions/${id}/downgrade`, payload),
    extendTrial: (id: number, payload: { additionalDays: number; reason: string }) =>
        platformApi.post(`/api/subscriptions/${id}/extend-trial`, payload),
    extendSubscription: (id: number, payload: { additionalDays: number; reason: string }) =>
        platformApi.post(`/api/subscriptions/${id}/extend`, payload),
    suspend: (id: number, payload: { reason: string }) => platformApi.post(`/api/subscriptions/${id}/suspend`, payload),
    reactivate: (id: number, payload: { reason: string }) => platformApi.post(`/api/subscriptions/${id}/reactivate`, payload),
    getAdminUsage: (schoolId: number) => platformApi.get<AdminSubscriptionUsageDto>(`/api/subscriptions/${schoolId}/admin-usage`),
    getAdminUsageBulk: (schoolIds: number[]) => platformApi.get<Record<number, AdminSubscriptionUsageDto>>(`/api/subscriptions/admin-usage/bulk?schoolIds=${schoolIds.join(",")}`),
    getPaymentHistory: (id: number) => platformApi.get<SubscriptionPaymentDto[]>(`/api/subscriptions/${id}/payments`),
    getEventHistory: (id: number) => platformApi.get<SubscriptionEventDto[]>(`/api/subscriptions/${id}/events`),
};

// ---------------------------------------------------------
// TENANT API — uses api (injects X-School-Id from ClientLayout)
// Used by /subscription (school admin view)
// ---------------------------------------------------------
export const subscriptionApi = {
    // School admin fetches their own subscription — uses X-School-Id header
    getMySubscription: () => api.get<SubscriptionDto>("/api/subscriptions/my"),
    // Access status for suspension check / usage bar
    getAccessStatus: () => api.get<SubscriptionAccessStatusDto>("/api/subscriptions/access-status"),
};

