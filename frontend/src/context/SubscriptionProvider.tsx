"use client";

import React, { useEffect, useState, useCallback, useRef } from "react";
import { SubscriptionContext, SubscriptionContextType } from "./SubscriptionContext";
import { subscriptionApi, SubscriptionAccessStatusDto } from "../lib/subscriptionApi";

const REFRESH_INTERVAL = 5 * 60 * 1000; // 5 minutes

export const SubscriptionProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [status, setStatus] = useState<SubscriptionAccessStatusDto>({
        subscriptionStatus: null,
        usagePercent: 0,
        usageWarningLevel: "NONE",
        daysToExpiry: 0,
        expiryWarningLevel: "NONE",
        schoolActive: true,
    });
    const [isLoading, setIsLoading] = useState(true);
    const refreshTimerRef = useRef<NodeJS.Timeout | null>(null);

    const fetchStatus = useCallback(async () => {
        try {
            // Check if user is logged in before fetching
            const token = localStorage.getItem("token");
            if (!token) {
                setIsLoading(false);
                return;
            }

            const response = await subscriptionApi.getAccessStatus();
            setStatus(response.data);
        } catch (error) {
            console.error("Failed to fetch subscription status:", error);
        } finally {
            setIsLoading(false);
        }
    }, []);

    const refreshSubscription = useCallback(async () => {
        setIsLoading(true);
        await fetchStatus();
    }, [fetchStatus]);

    useEffect(() => {
        fetchStatus();

        // Setup polling
        refreshTimerRef.current = setInterval(fetchStatus, REFRESH_INTERVAL);

        return () => {
            if (refreshTimerRef.current) {
                clearInterval(refreshTimerRef.current);
            }
        };
    }, [fetchStatus]);

    // Handle login refresh by listening to storage changes or common patterns
    // For simplicity, we can also expose refreshSubscription and call it after login

    const value: SubscriptionContextType = {
        ...status,
        isLoading,
        refreshSubscription,
    };

    return (
        <SubscriptionContext.Provider value={value}>
            {children}
        </SubscriptionContext.Provider>
    );
};
