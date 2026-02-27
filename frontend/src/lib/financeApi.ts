import { api } from "./api";

export interface ExpenseHeadData {
    id: number;
    name: string;
    description?: string;
    active: boolean;
}

export interface ExpenseVoucherRequest {
    expenseDate: string; // YYYY-MM-DD
    expenseHeadId: number;
    amount: number;
    paymentMode: "CASH" | "BANK" | "UPI";
    description?: string;
    referenceNumber?: string;
}

export interface FinanceAccountTransferRequest {
    transferDate: string; // YYYY-MM-DD
    amount: number;
    referenceNumber?: string;
    remarks?: string;
}

export interface FinanceAccountTransferData {
    id: number;
    sessionId: number;
    transferDate: string;
    amount: number;
    fromAccount: string;
    toAccount: string;
    referenceNumber?: string;
    remarks?: string;
    createdBy: number;
    createdAt: string;
}

export interface ExpenseVoucherData {
    id: number;
    voucherNumber: string;
    expenseDate: string;
    expenseHeadId: number;
    expenseHeadName: string;
    amount: number;
    paymentMode: "CASH" | "BANK" | "UPI";
    description?: string;
    referenceNumber?: string;
    createdBy: string;
    active: boolean;
}

export interface DailyCashSummary {
    totalFeeCollected: number;
    totalExpense: number;
    netCash: number;
    cashRevenue: number;
    bankRevenue: number;
    cashExpense: number;
    bankExpense: number;
    netBank: number;
    transferOut: number;
    transferIn: number;
    netAmount: number;
    closed: boolean;
}

export interface MonthlyPLData {
    year: number;
    month: number;
    totalRevenue: number;
    totalExpense: number;
    netProfit: number;
    cashRevenue: number;
    bankRevenue: number;
    cashExpense: number;
    bankExpense: number;
    netCash: number;
    netBank: number;
}

export interface SessionPLData {
    sessionId: number;
    sessionName: string;
    totalRevenue: number;
    totalExpense: number;
    netProfit: number;
    cashRevenue: number;
    bankRevenue: number;
    cashExpense: number;
    bankExpense: number;
    netCash: number;
    netBank: number;
}

export interface FeeHeadSummary {
    feeTypeName: string;
    totalPrincipal: number;
    totalLateFee: number;
    totalCollected: number;
}

export const financeApi = {
    // Daily Cash
    getDailyCashSummary: async (date: string) => {
        const response = await api.get<DailyCashSummary>(`/api/dashboard/daily-cash?date=${date}`);
        return response.data;
    },

    getFeeHeadSummary: async (date: string) => {
        const response = await api.get<FeeHeadSummary[]>(`/api/fees/payments/head-summary?date=${date}`);
        return response.data;
    },

    // P&L Reports
    getMonthlyPL: async (year: number, month: number) => {
        const response = await api.get<MonthlyPLData>(`/api/finance/monthly-pl?year=${year}&month=${month}`);
        return response.data;
    },

    getSessionPL: async () => {
        const response = await api.get<SessionPLData>("/api/finance/session-pl");
        return response.data;
    },

    // Export Reports
    exportDailyCash: async (date: string) => {
        const response = await api.get(`/api/finance/export/daily-cash?date=${date}`, { responseType: "blob" });
        return new Blob([response.data], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
    },

    exportMonthlyPL: async (year: number, month: number) => {
        const response = await api.get(`/api/finance/export/monthly-pl?year=${year}&month=${month}`, { responseType: "blob" });
        return new Blob([response.data], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
    },

    exportSessionPL: async () => {
        const response = await api.get("/api/finance/export/session-pl", { responseType: "blob" });
        return new Blob([response.data], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
    },

    exportExpenses: async (date: string) => {
        const response = await api.get(`/api/finance/export/expenses?date=${date}`, { responseType: "blob" });
        return new Blob([response.data], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
    },

    // Expenses
    getExpenseHeads: async () => {
        const response = await api.get<ExpenseHeadData[]>("/api/expenses/heads");
        return response.data;
    },

    createExpense: async (data: ExpenseVoucherRequest) => {
        const response = await api.post<ExpenseVoucherData>("/api/expenses", data);
        return response.data;
    },

    getExpensesByDate: async (date: string) => {
        const response = await api.get<ExpenseVoucherData[]>(`/api/expenses?date=${date}`);
        return response.data;
    },

    // Expense Heads Management
    createExpenseHead: async (data: { name: string; description?: string }) => {
        const response = await api.post<ExpenseHeadData>("/api/expenses/heads", data);
        return response.data;
    },

    toggleExpenseHeadActive: async (id: number) => {
        const response = await api.patch<ExpenseHeadData>(`/api/expenses/heads/${id}/toggle-active`);
        return response.data;
    },

    // Transfers
    createTransfer: async (data: FinanceAccountTransferRequest) => {
        const response = await api.post<FinanceAccountTransferData>("/api/finance/transfers", data);
        return response.data;
    },

    // Day Closing
    closeDay: async (date: string) => {
        await api.post(`/api/finance/day-closing?date=${date}`);
    },

    enableOverride: async (date: string) => {
        await api.patch(`/api/finance/day-closing/${date}/override`);
    }
};
