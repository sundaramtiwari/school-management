export default function Dashboard() {
  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Dashboard</h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white p-4 rounded shadow">
          Total Schools
        </div>

        <div className="bg-white p-4 rounded shadow">
          Total Students
        </div>

        <div className="bg-white p-4 rounded shadow">
          Pending Fees
        </div>
      </div>
    </div>
  );
}
