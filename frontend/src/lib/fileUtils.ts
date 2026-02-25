/**
 * Utility to trigger browser download of an Excel file from a Blob.
 * Handles temporary object URL creation and cleanup to prevent memory leaks.
 */
export function downloadExcel(blob: Blob, filename: string) {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", filename);
    document.body.appendChild(link);
    link.click();

    // Cleanup
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
}
