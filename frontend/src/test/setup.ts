import "@testing-library/jest-dom";
import { vi } from "vitest";

// Mock next/navigation
vi.mock("next/navigation", () => ({
    useRouter: () => ({
        push: vi.fn(),
        replace: vi.fn(),
    }),
    usePathname: () => "",
}));

// Mock AuthContext
vi.mock("@/context/AuthContext", () => ({
    useAuth: () => ({
        user: { role: "ADMIN" },
        isLoading: false
    })
}));

// Mock SessionContext
vi.mock("@/context/SessionContext", () => ({
    useSession: () => ({
        currentSession: { id: 1, name: "2024-25" },
        sessions: [],
        isLoading: false
    })
}));
