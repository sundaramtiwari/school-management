import { redirect } from "next/navigation";

export default function FeesPage() {
    redirect("/fees/summary");
}
type RecentPayment = {
    id: number;
    studentId: number;
    mode: string;
    paymentDate: string;
    amountPaid: number;
};
