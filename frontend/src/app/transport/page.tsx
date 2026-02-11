"use client";

import { useEffect, useState } from "react";
import { transportApi } from "@/lib/transportApi";
import { useToast } from "@/components/ui/Toast";
import Modal from "@/components/ui/Modal";
import { Skeleton, TableSkeleton } from "@/components/ui/Skeleton";

type Route = {
    id: number;
    name: string;
    description: string;
};

type PickupPoint = {
    id: number;
    name: string;
    amount: number;
    frequency: string;
};

const FREQUENCIES = ["MONTHLY", "QUARTERLY", "HALF_YEARLY", "ANNUALLY"];

export default function TransportPage() {
    const { showToast } = useToast();

    const [routes, setRoutes] = useState<Route[]>([]);
    const [selectedRoute, setSelectedRoute] = useState<Route | null>(null);
    const [pickups, setPickups] = useState<PickupPoint[]>([]);

    const [loadingRoutes, setLoadingRoutes] = useState(true);
    const [loadingPickups, setLoadingPickups] = useState(false);

    const [showRouteModal, setShowRouteModal] = useState(false);
    const [showPickupModal, setShowPickupModal] = useState(false);

    const [routeForm, setRouteForm] = useState({ name: "", description: "" });
    const [pickupForm, setPickupForm] = useState({
        name: "",
        amount: "",
        frequency: "MONTHLY",
    });

    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        loadRoutes();
    }, []);

    async function loadRoutes() {
        try {
            setLoadingRoutes(true);
            const res = await transportApi.getAllRoutes();
            setRoutes(res.data || []);
        } catch {
            showToast("Failed to load transport routes", "error");
        } finally {
            setLoadingRoutes(false);
        }
    }

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
        if (!routeForm.name) return;
        try {
            setIsSaving(true);
            const res = await transportApi.createRoute(routeForm);
            setRoutes([...routes, res.data]);
            setShowRouteModal(false);
            setRouteForm({ name: "", description: "" });
            showToast("Route created successfully", "success");
        } catch (e: any) {
            showToast("Failed to create route: " + (e.response?.data?.message || e.message), "error");
        } finally {
            setIsSaving(false);
        }
    }

    async function savePickup() {
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
        } catch (e: any) {
            showToast("Failed to add pickup point: " + (e.response?.data?.message || e.message), "error");
        } finally {
            setIsSaving(false);
        }
    }

    return (
        <div className="space-y-6">
            <header className="flex justify-between items-center">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">Transport Management</h1>
                    <p className="text-gray-500">Define routes and associated pickup point fees.</p>
                </div>
                <button
                    onClick={() => setShowRouteModal(true)}
                    className="bg-blue-600 text-white px-6 py-2.5 rounded-xl font-bold shadow-lg hover:bg-blue-700 transition-all flex items-center gap-2"
                >
                    <span>+</span> New Route
                </button>
            </header>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Routes List */}
                <div className="bg-white border rounded-2xl shadow-sm overflow-hidden flex flex-col h-[70vh]">
                    <div className="p-4 border-b bg-gray-50 text-xs font-black uppercase text-gray-400 tracking-widest">
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
                            <button
                                key={r.id}
                                onClick={() => onSelectRoute(r)}
                                className={`w-full text-left p-6 hover:bg-blue-50/50 transition-all group ${selectedRoute?.id === r.id ? "bg-blue-50 border-r-4 border-blue-600" : ""
                                    }`}
                            >
                                <div className="font-bold text-gray-800 group-hover:text-blue-700">{r.name}</div>
                                <div className="text-xs text-gray-400 mt-1 italic">{r.description || "No description"}</div>
                            </button>
                        ))}
                        {!loadingRoutes && routes.length === 0 && (
                            <div className="p-12 text-center text-gray-400 italic text-sm">No routes defined yet.</div>
                        )}
                    </div>
                </div>

                {/* Pickup Points */}
                <div className="lg:col-span-2 space-y-6">
                    {selectedRoute ? (
                        <div className="bg-white border rounded-2xl shadow-sm overflow-hidden flex flex-col h-[70vh]">
                            <div className="p-4 border-b bg-gray-50 flex justify-between items-center">
                                <span className="text-xs font-black uppercase text-gray-400 tracking-widest">
                                    Stops for {selectedRoute.name}
                                </span>
                                <button
                                    onClick={() => setShowPickupModal(true)}
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
                        <div className="h-[70vh] flex flex-col items-center justify-center bg-gray-50 border rounded-2xl border-dashed space-y-4 text-gray-400">
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
                        <button onClick={() => setShowRouteModal(false)} className="btn-secondary">Cancel</button>
                        <button onClick={saveRoute} disabled={isSaving} className="btn-primary">
                            {isSaving ? "Saving..." : "Create Route"}
                        </button>
                    </div>
                }
            >
                <div className="space-y-4">
                    <div>
                        <label className="label-ref">Route Name *</label>
                        <input
                            className="input-ref"
                            placeholder="e.g. South Campus Express"
                            value={routeForm.name}
                            onChange={e => setRouteForm({ ...routeForm, name: e.target.value })}
                        />
                    </div>
                    <div>
                        <label className="label-ref">Description</label>
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
                        <button onClick={() => setShowPickupModal(false)} className="btn-secondary">Cancel</button>
                        <button onClick={savePickup} disabled={isSaving} className="btn-primary">
                            {isSaving ? "Adding..." : "Add Stop"}
                        </button>
                    </div>
                }
            >
                <div className="space-y-4">
                    <div>
                        <label className="label-ref">Stop Name *</label>
                        <input
                            className="input-ref"
                            placeholder="e.g. Green Park Circle"
                            value={pickupForm.name}
                            onChange={e => setPickupForm({ ...pickupForm, name: e.target.value })}
                        />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="label-ref">Fee Frequency *</label>
                            <select
                                className="input-ref"
                                value={pickupForm.frequency}
                                onChange={e => setPickupForm({ ...pickupForm, frequency: e.target.value })}
                            >
                                {FREQUENCIES.map(f => <option key={f} value={f}>{f.replace('_', ' ')}</option>)}
                            </select>
                        </div>
                        <div>
                            <label className="label-ref">Amount (₹) *</label>
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

            <style jsx>{`
        .label-ref {
          display: block;
          font-size: 0.75rem;
          font-weight: 700;
          text-transform: uppercase;
          color: #9ca3af;
          margin-bottom: 0.5rem;
          margin-left: 0.25rem;
        }
        .btn-primary {
          background-color: #2563eb;
          color: white;
          padding: 0.625rem 1.5rem;
          border-radius: 0.75rem;
          font-weight: 700;
          box-shadow: 0 10px 15px -3px rgba(37, 99, 235, 0.2);
          transition: all 0.2s;
        }
        .btn-primary:hover {
          background-color: #1d4ed8;
        }
        .btn-primary:disabled {
          background-color: #9ca3af;
          box-shadow: none;
        }
        .btn-secondary {
          padding: 0.625rem 1.5rem;
          border-radius: 0.75rem;
          border-width: 1px;
          font-weight: 500;
          color: #4b5563;
          transition: all 0.2s;
        }
        .btn-secondary:hover {
          background-color: #f9fafb;
        }
      `}</style>
        </div>
    );
}
