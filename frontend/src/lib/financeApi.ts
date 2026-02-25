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
}

export interface FeeHeadSummary {
    feeHeadName: string;
    totalPrincipal: number;
    totalLateFee: number;
    totalCollection: number;
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
    }
};
