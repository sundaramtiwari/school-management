"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSession } from "@/context/SessionContext";

export default function Breadcrumbs() {
    const pathname = usePathname();
    const paths = pathname.split("/").filter(Boolean);
    const { currentSession, sessions } = useSession();

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

            <div className="relative group">
                <button
                    className={`
                        ${currentSession?.active ? "bg-blue-50 text-blue-700 border-blue-100" : "bg-amber-50 text-amber-700 border-amber-200"}
                        border px-3 py-1.5 rounded-lg text-xs font-bold uppercase flex items-center gap-2 hover:opacity-80 transition-all shadow-sm
                    `}
                    title={currentSession?.active ? "Active global session" : "Currently browsing an old/future session (Not Global Active)"}
                >
                    <span className="flex items-center gap-1.5">
                        {currentSession?.active ? "Session" : "📂 Browsing"}: {currentSession?.name || "Not selected"}
                    </span>
                    <span className="text-[10px]">▼</span>
                </button>

                <div className="absolute right-0 mt-2 w-56 bg-white border border-gray-100 rounded-xl shadow-xl opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all z-[100] p-1 overflow-hidden">
                    <div className="px-3 py-2 text-[10px] font-bold text-gray-400 uppercase border-b border-gray-50 mb-1">
                        Select Academic Session
                    </div>
                    {sessions.map((s) => (
                        <button
                            key={s.id}
                            onClick={() => {
                                localStorage.setItem("sessionId", s.id.toString());
                                window.location.reload();
                            }}
                            className={`
                                w-full text-left px-3 py-2.5 rounded-lg text-sm transition-all flex items-center justify-between group/item
                                ${currentSession?.id === s.id
                                    ? "bg-blue-50 text-blue-700 font-bold"
                                    : "text-gray-600 hover:bg-gray-50 hover:text-gray-900"}
                            `}
                        >
                            <div className="flex flex-col">
                                <span className="font-semibold">{s.name}</span>
                                {s.active && <span className="text-[9px] text-gray-400 font-normal leading-none mt-0.5">(Global Active)</span>}
                            </div>
                            {currentSession?.id === s.id && (
                                <span className="text-blue-500 font-bold">✓</span>
                            )}
                        </button>
                    ))}
                    {sessions.length === 0 && (
                        <div className="px-3 py-4 text-center text-xs text-gray-400 italic">
                            No sessions found
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
