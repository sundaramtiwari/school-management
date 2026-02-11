"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

export default function FeesLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    const pathname = usePathname();

    const tabs = [
        { name: "Dashboard", path: "/fees" },
        { name: "Structures", path: "/fees/structures" },
        { name: "Collect Fees", path: "/fees/collect" },
        { name: "Defaulters", path: "/fees/defaulters" },
    ];


    return (
        <div className="space-y-6">
            <div className="border-b flex gap-6 text-sm font-medium text-gray-600">
                {tabs.map((tab) => {
                    const isActive = pathname === tab.path;
                    return (
                        <Link
                            key={tab.path}
                            href={tab.path}
                            className={`pb-2 border-b-2 px-1 ${isActive
                                ? "border-blue-600 text-blue-600"
                                : "border-transparent hover:text-gray-900 hover:border-gray-300"
                                }`}
                        >
                            {tab.name}
                        </Link>
                    );
                })}
            </div>

            <div className="min-h-[400px]">
                {children}
            </div>
        </div>
    );
}
