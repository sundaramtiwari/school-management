import { createContext, useContext } from "react";
import { SubscriptionAccessStatusDto } from "../lib/subscriptionApi";

export interface SubscriptionContextType extends SubscriptionAccessStatusDto {
    isLoading: boolean;
    refreshSubscription: () => Promise<void>;
}

export const SubscriptionContext = createContext<SubscriptionContextType | undefined>(undefined);

export function useSubscription() {
    const context = useContext(SubscriptionContext);
    if (context === undefined) {
        throw new Error("useSubscription must be used within a SubscriptionProvider");
    }
    return context;
}
