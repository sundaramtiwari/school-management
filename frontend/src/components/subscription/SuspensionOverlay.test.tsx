import React from "react";
import { render, screen } from "@testing-library/react";
import SuspensionOverlay from "@/components/subscription/SuspensionOverlay";
import { useSubscription } from "@/context/SubscriptionContext";
import { vi, describe, it, expect, beforeEach } from "vitest";

vi.mock("@/context/SubscriptionContext", () => ({
    useSubscription: vi.fn()
}));

describe("SuspensionOverlay", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should not render when status is not SUSPENDED", () => {
        (useSubscription as any).mockReturnValue({
            subscriptionStatus: "ACTIVE"
        });

        const { container } = render(<SuspensionOverlay />);
        expect(container).toBeEmptyDOMElement();
    });

    it("should render when status is SUSPENDED", () => {
        (useSubscription as any).mockReturnValue({
            subscriptionStatus: "SUSPENDED"
        });

        render(<SuspensionOverlay />);
        expect(screen.getByText("Account Suspended")).toBeInTheDocument();
        expect(screen.getByText(/Access to your school's operational features has been suspended/)).toBeInTheDocument();
    });
});
