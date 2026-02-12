"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSession } from "@/context/SessionContext";

export default function Breadcrumbs() {
    const pathname = usePathname();
    const paths = pathname.split("/").filter(Boolean);
    const { currentSession } = useSession();

    return (
        <div className="mb-6 flex items-center justify-between gap-3">
            <nav className="flex items-center space-x-2 text-sm text-gray-500 bg-white py-2 px-4 rounded-lg border shadow-sm w-fit">
                <Link href="/" className="hover:text-blue-600 transition-colors">
                    Dashboard
                </Link>
                {paths.map((p, i) => {
                    const href = `/${paths.slice(0, i + 1).join("/")}`;
                    const isLast = i === paths.length - 1;
                    const label = p.charAt(0).toUpperCase() + p.slice(1).replace(/-/g, " ");

                    return (
                        <React.Fragment key={href}>
                            <svg className="w-4 h-4 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
                                <path fillRule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clipRule="evenodd" />
                            </svg>
                            {isLast ? (
                                <span className="font-semibold text-gray-800">{label}</span>
                            ) : (
                                <Link href={href} className="hover:text-blue-600 transition-colors">
                                    {label}
                                </Link>
                            )}
                        </React.Fragment>
                    );
                })}
            </nav>

            <div className="bg-blue-50 text-blue-700 border border-blue-100 px-3 py-1.5 rounded-lg text-xs font-bold uppercase">
                Session: {currentSession?.name || "Not selected"}
            </div>
        </div>
    );
}
