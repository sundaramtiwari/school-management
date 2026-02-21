"use client";

import { useCallback, useEffect, useState } from "react";
import { transportApi } from "@/lib/transportApi";
import { useToast } from "@/components/ui/Toast";
import Modal from "@/components/ui/Modal";
import { Skeleton, TableSkeleton } from "@/components/ui/Skeleton";
import { useAuth } from "@/context/AuthContext";

type Route = {
    id: number;
    name: string;
    description: string;
    capacity: number;
    currentStrength: number;
};

type PickupPoint = {
    id: number;
    name: string;
    amount: number;
    frequency: string;
};

const FREQUENCIES = ["MONTHLY", "QUARTERLY", "HALF_YEARLY", "ANNUALLY"];

export default function TransportPage() {
    const { user } = useAuth();
    const { showToast } = useToast();
    const canMutateTransport = user?.role === "SCHOOL_ADMIN" || user?.role === "SUPER_ADMIN";

    const [routes, setRoutes] = useState<Route[]>([]);
    const [selectedRoute, setSelectedRoute] = useState<Route | null>(null);
    const [pickups, setPickups] = useState<PickupPoint[]>([]);

    const [loadingRoutes, setLoadingRoutes] = useState(true);
    const [loadingPickups, setLoadingPickups] = useState(false);

    const [showRouteModal, setShowRouteModal] = useState(false);
    const [showPickupModal, setShowPickupModal] = useState(false);
    const [routeToDelete, setRouteToDelete] = useState<number | null>(null);

    const [routeForm, setRouteForm] = useState({ name: "", description: "", capacity: "30" });
    const [pickupForm, setPickupForm] = useState({
        name: "",
        amount: "",
        frequency: "MONTHLY",
    });

    const [isSaving, setIsSaving] = useState(false);

    function getErrorMessage(error: unknown): string {
        if (error && typeof error === "object" && "response" in error) {
            const response = (error as { response?: { data?: { message?: string } } }).response;
            if (response?.data?.message) return response.data.message;
        }
        if (error instanceof Error) return error.message;
        return "Unknown error";
    }

    const loadRoutes = useCallback(async () => {
        try {
            setLoadingRoutes(true);
            const res = await transportApi.getAllRoutes();
            setRoutes(res.data || []);
        } catch {
            showToast("Failed to load transport routes", "error");
        } finally {
            setLoadingRoutes(false);
        }
    }, [showToast]);

    useEffect(() => {
        loadRoutes();
    }, [loadRoutes]);

    async function onSelectRoute(route: Route) {
        setSelectedRoute(route);
        setPickups([]);
        try {
            setLoadingPickups(true);
            const res = await transportApi.getPickupsByRoute(route.id);
            setPickups(res.data || []);
        } catch {
            showToast("Failed to load pickup points", "error");
        } finally {
            setLoadingPickups(false);
        }
    }

    async function saveRoute() {
        if (!canMutateTransport) return;
        if (!routeForm.name) return;
        try {
            setIsSaving(true);
            const res = await transportApi.createRoute({
                ...routeForm,
                capacity: Number(routeForm.capacity)
            });
            setRoutes([...routes, res.data]);
            setShowRouteModal(false);
            setRouteForm({ name: "", description: "", capacity: "30" });
            showToast("Route created successfully", "success");
        } catch (e: unknown) {
            showToast("Failed to create route: " + getErrorMessage(e), "error");
        } finally {
            setIsSaving(false);
        }
    }

    async function deleteRoute(id: number) {
        if (!canMutateTransport) return;
        try {
            setIsSaving(true);
            await transportApi.deleteRoute(id);
            setRoutes(routes.filter(r => r.id !== id));
            if (selectedRoute?.id === id) setSelectedRoute(null);
            setRouteToDelete(null);
            showToast("Route deleted successfully", "success");
        } catch (e: unknown) {
            showToast("Failed to delete route: " + getErrorMessage(e), "error");
        } finally {
            setIsSaving(false);
        }
    }

    async function savePickup() {
        if (!canMutateTransport) return;
        if (!selectedRoute || !pickupForm.name || !pickupForm.amount) return;
        try {
            setIsSaving(true);
            const res = await transportApi.createPickup({
                ...pickupForm,
                amount: Number(pickupForm.amount),
                routeId: selectedRoute.id,
            });
            setPickups([...pickups, res.data]);
            setShowPickupModal(false);
            setPickupForm({ name: "", amount: "", frequency: "MONTHLY" });
            showToast("Pickup point added", "success");
        } catch (e: unknown) {
            showToast("Failed to add pickup point: " + getErrorMessage(e), "error");
        } finally {
            setIsSaving(false);
        }
    }

    return (
        <div className="mx-auto px-6 py-6 space-y-6">
            <header className="flex justify-between items-center mb-2">
                <div>
                    <h1 className="text-lg font-semibold">Transport Management</h1>
                    <p className="text-gray-500 text-base mt-1">Define routes and associated pickup point fees.</p>
                </div>
                <button
                    onClick={() => setShowRouteModal(true)}
                    disabled={!canMutateTransport}
                    className="bg-blue-600 text-white px-6 py-2.5 rounded-md font-medium hover:bg-blue-700 flex items-center gap-2 text-base"
                >
                    <span>+</span> New Route
                </button>
            </header>

            <div className="bg-blue-50 text-blue-800 p-4 rounded-lg flex items-start gap-3 border border-blue-100 mb-6 font-medium text-sm">
                <span className="text-blue-500">ℹ️</span>
                <p>
                    <strong className="font-bold">Note:</strong> Transport fee billing uses the core Fee Structure.
                    The Pickup Point fees listed below are for informational/routing purposes and are not directly billed here.
                </p>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Routes List */}
                <div className="bg-white rounded-lg shadow border border-gray-100 overflow-hidden flex flex-col h-[70vh]">
                    <div className="p-4 border-b border-gray-100 bg-gray-50 text-lg font-semibold">
                        Active Routes
                    </div>
                    <div className="flex-1 overflow-y-auto divide-y">
                        {loadingRoutes ? (
                            <div className="p-4 space-y-3">
                                <Skeleton className="h-12 w-full" />
                                <Skeleton className="h-12 w-full" />
                                <Skeleton className="h-12 w-full" />
                            </div>
                        ) : routes.map(r => (
                            <div
                                key={r.id}
                                className={`group relative border-b last:border-b-0 ${selectedRoute?.id === r.id ? "bg-blue-50/80" : "hover:bg-gray-50"}`}
                            >
                                <button
                                    onClick={() => onSelectRoute(r)}
                                    className="w-full text-left p-6 transition-all"
                                >
                                    <div className="flex justify-between items-start">
                                        <div className="font-bold text-gray-800 group-hover:text-blue-700">{r.name}</div>
                                        <div className="text-[10px] font-black uppercase px-2 py-0.5 rounded bg-gray-100 text-gray-500">
                                            {r.currentStrength} / {r.capacity}
                                        </div>
                                    </div>
                                    <div className="text-xs text-gray-400 mt-1 italic">{r.description || "No description"}</div>

                                    {/* Capacity indicator */}
                                    <div className="mt-3 h-1.5 w-full bg-gray-200 rounded-full overflow-hidden">
                                        <div
                                            className={`h-full rounded-full transition-all ${(r.currentStrength / r.capacity) > 0.9 ? 'bg-red-500' : 'bg-blue-500'
                                                }`}
                                            style={{ width: `${Math.min(100, (r.currentStrength / r.capacity) * 100)}%` }}
                                        />
                                    </div>
                                </button>
                                <button
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        setRouteToDelete(r.id);
                                    }}
                                    disabled={!canMutateTransport}
                                    className="absolute top-4 right-4 p-2 text-gray-400 hover:text-red-600 opacity-0 group-hover:opacity-100 transition-opacity"
                                >
                                    🗑️
                                </button>
                            </div>
                        ))}
                        {!loadingRoutes && routes.length === 0 && (
                            <div className="p-12 text-center text-gray-400 italic text-sm">No routes defined yet.</div>
                        )}
                    </div>
                </div>

                {/* Delete Confirmation Modal */}
                <Modal
                    isOpen={routeToDelete !== null}
                    onClose={() => setRouteToDelete(null)}
                    title="Confirm Deletion"
                    footer={
                        <div className="flex gap-2">
                            <button onClick={() => setRouteToDelete(null)} className="px-6 py-2 rounded-md bg-white border border-gray-300 font-medium text-gray-600 hover:bg-gray-50">Cancel</button>
                            <button
                                onClick={() => routeToDelete && deleteRoute(routeToDelete)}
                                disabled={isSaving}
                                className="bg-red-600 text-white px-6 py-2 rounded-md font-medium hover:bg-red-700 disabled:opacity-50"
                            >
                                {isSaving ? "Deleting..." : "Delete Route"}
                            </button>
                        </div>
                    }
                >
                    <p className="text-gray-600">Are you sure you want to delete this route? This action cannot be undone if there are no active enrollments.</p>
                </Modal>

                {/* Pickup Points */}
                <div className="lg:col-span-2 space-y-6">
                    {selectedRoute ? (
                        <div className="bg-white rounded-lg shadow border border-gray-100 overflow-hidden flex flex-col h-[70vh]">
                            <div className="p-4 border-b border-gray-100 bg-gray-50 flex justify-between items-center">
                                <span className="text-lg font-semibold">
                                    Stops for {selectedRoute.name}
                                </span>
                                <button
                                    onClick={() => setShowPickupModal(true)}
                                    disabled={!canMutateTransport}
                                    className="text-xs font-bold text-blue-600 hover:underline"
                                >
                                    + Add Stop
                                </button>
                            </div>
                            <div className="flex-1 overflow-auto">
                                {loadingPickups ? (
                                    <div className="p-6">
                                        <TableSkeleton rows={5} cols={3} />
                                    </div>
                                ) : (
                                    <table className="w-full text-sm">
                                        <thead className="bg-gray-50/50 text-gray-400 font-bold border-b text-[10px] uppercase">
                                            <tr>
                                                <th className="p-4 text-left">Stop Name</th>
                                                <th className="p-4 text-center">Frequency</th>
                                                <th className="p-4 text-right">Fee Amount</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-gray-100 italic font-medium">
                                            {pickups.map(p => (
                                                <tr key={p.id} className="hover:bg-gray-50/50 transition-colors">
                                                    <td className="p-6 font-bold text-gray-800 not-italic">{p.name}</td>
                                                    <td className="p-6 text-center">
                                                        <span className="px-3 py-1 bg-blue-50 text-blue-600 rounded-lg text-[10px] font-black uppercase">
                                                            {p.frequency}
                                                        </span>
                                                    </td>
                                                    <td className="p-6 text-right font-black text-gray-900 not-italic">
                                                        ₹ {p.amount.toLocaleString()}
                                                    </td>
                                                </tr>
                                            ))}
                                            {pickups.length === 0 && (
                                                <tr>
                                                    <td colSpan={3} className="p-20 text-center text-gray-400 italic">
                                                        No stops added to this route yet.
                                                    </td>
                                                </tr>
                                            )}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        </div>
                    ) : (
                        <div className="h-[70vh] flex flex-col items-center justify-center bg-white rounded-lg shadow border border-gray-100 space-y-4 text-gray-500">
                            <span className="text-4xl opacity-20">🚌</span>
                            <p className="italic">Select a route to configure pickup stops and fees.</p>
                        </div>
                    )}
                </div>
            </div>

            {/* Route Modal */}
            <Modal
                isOpen={showRouteModal}
                onClose={() => setShowRouteModal(false)}
                title="Create Transport Route"
                footer={
                    <div className="flex gap-2">
                        <button onClick={() => setShowRouteModal(false)} className="px-6 py-2 rounded-md bg-white border border-gray-300 font-medium text-gray-600 hover:bg-gray-50">Cancel</button>
                        <button onClick={saveRoute} disabled={isSaving} className="px-6 py-2 rounded-md bg-blue-600 text-white font-medium hover:bg-blue-700 disabled:opacity-50">
                            {isSaving ? "Saving..." : "Create Route"}
                        </button>
                    </div>
                }
            >
                <div className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-500 mb-2">Route Name *</label>
                        <input
                            className="input-ref"
                            placeholder="e.g. South Campus Express"
                            value={routeForm.name}
                            onChange={e => setRouteForm({ ...routeForm, name: e.target.value })}
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-500 mb-2">Capacity (Seats) *</label>
                        <input
                            type="number"
                            className="input-ref"
                            placeholder="30"
                            value={routeForm.capacity}
                            onChange={e => setRouteForm({ ...routeForm, capacity: e.target.value })}
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-500 mb-2">Description</label>
                        <textarea
                            className="input-ref h-24"
                            placeholder="Primary landmarks or areas covered..."
                            value={routeForm.description}
                            onChange={e => setRouteForm({ ...routeForm, description: e.target.value })}
                        />
                    </div>

                </div>
            </Modal>

            {/* Pickup Modal */}
            <Modal
                isOpen={showPickupModal}
                onClose={() => setShowPickupModal(false)}
                title="Add Pickup Point Stop"
                footer={
                    <div className="flex gap-2">
                        <button onClick={() => setShowPickupModal(false)} className="px-6 py-2 rounded-md bg-white border border-gray-300 font-medium text-gray-600 hover:bg-gray-50">Cancel</button>
                        <button onClick={savePickup} disabled={isSaving} className="px-6 py-2 rounded-md bg-blue-600 text-white font-medium hover:bg-blue-700 disabled:opacity-50">
                            {isSaving ? "Adding..." : "Add Stop"}
                        </button>
                    </div>
                }
            >
                <div className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-500 mb-2">Stop Name *</label>
                        <input
                            className="input-ref"
                            placeholder="e.g. Green Park Circle"
                            value={pickupForm.name}
                            onChange={e => setPickupForm({ ...pickupForm, name: e.target.value })}
                        />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-500 mb-2">Fee Frequency *</label>
                            <select
                                className="input-ref"
                                value={pickupForm.frequency}
                                onChange={e => setPickupForm({ ...pickupForm, frequency: e.target.value })}
                            >
                                {FREQUENCIES.map(f => <option key={f} value={f}>{f.replace('_', ' ')}</option>)}
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-500 mb-2">Amount (₹) *</label>
                            <input
                                type="number"
                                className="input-ref"
                                placeholder="0.00"
                                value={pickupForm.amount}
                                onChange={e => setPickupForm({ ...pickupForm, amount: e.target.value })}
                            />
                        </div>
                    </div>
                </div>
            </Modal>

        </div>
    );
}
