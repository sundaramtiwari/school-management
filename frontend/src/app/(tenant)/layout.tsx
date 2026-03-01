import ClientLayout from "@/components/ClientLayout";

export default function TenantGroupLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    return <ClientLayout>{children}</ClientLayout>;
}
