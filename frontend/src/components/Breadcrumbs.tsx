"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

export default function Breadcrumbs() {
    const pathname = usePathname();
    const paths = pathname.split("/").filter(Boolean);

    if (paths.length === 0) return null;

    return (
        <nav className="flex items-center space-x-2 text-sm text-gray-500 mb-6 bg-white py-2 px-4 rounded-lg border shadow-sm">
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
    );
}

import React from "react";
