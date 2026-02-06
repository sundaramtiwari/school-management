"use client";

export default function FeesDashboard() {
    return (
        <div className="text-center p-12 text-gray-500">
            <h2 className="text-xl font-semibold mb-2">Fee Management Dashboard</h2>
            <p>Select "Structures" to define fees or "Collect Fees" to manage payments.</p>

            <div className="grid grid-cols-3 gap-6 mt-12 text-left">
                <div className="p-6 border rounded bg-white shadow-sm">
                    <h3 className="text-gray-500 text-sm">Today's Collection</h3>
                    <p className="text-2xl font-bold">₹ 0</p>
                </div>
                <div className="p-6 border rounded bg-white shadow-sm">
                    <h3 className="text-gray-500 text-sm">Pending Dues</h3>
                    <p className="text-2xl font-bold">₹ --</p>
                </div>
                <div className="p-6 border rounded bg-white shadow-sm">
                    <h3 className="text-gray-500 text-sm">Total Students</h3>
                    <p className="text-2xl font-bold">--</p>
                </div>
            </div>
        </div>
    );
}
