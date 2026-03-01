import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import UpgradeModal from "@/components/subscription/UpgradeModal";
import { pricingPlanApi, subscriptionApi } from "@/lib/subscriptionApi";
import { vi, describe, it, expect, beforeEach } from "vitest";

vi.mock("@/lib/subscriptionApi", () => ({
    pricingPlanApi: {
        list: vi.fn()
    },
    subscriptionApi: {
        upgradePreview: vi.fn(),
        upgrade: vi.fn()
    }
}));

describe("UpgradeModal", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should fetch and show upgrade preview when a plan is selected", async () => {
        const mockPlans = [
            { id: 2, name: "Premium Plan", yearlyPrice: 10000, studentCap: 1000, active: true }
        ];

        const mockPreview = {
            subscription: { pricingPlanName: "Basic Plan" },
            proratedAmount: 5000
        };

        (pricingPlanApi.list as any).mockResolvedValue({ data: mockPlans });
        (subscriptionApi.upgradePreview as any).mockResolvedValue({ data: mockPreview });

        render(
            <UpgradeModal
                subscriptionId={1}
                currentPlanId={1}
                schoolName="Test School"
                onClose={() => { }}
                onSuccess={() => { }}
            />
        );

        // Wait for plans to load
        await waitFor(() => expect(screen.getByText(/Premium Plan/)).toBeInTheDocument());

        // Simulate plan selection
        const select = screen.getByRole("combobox");
        // We trigger the change manually since we don't have fireEvent easily without more boilerplate, 
        // but Testing Library handles it.
        const { fireEvent } = await import("@testing-library/react");
        fireEvent.change(select, { target: { value: "2" } });

        // Check for preview values
        await waitFor(() => {
            expect(screen.getByText("₹5,000")).toBeInTheDocument();
            expect(screen.getByText("Basic Plan")).toBeInTheDocument();
        });
    });
});
