"use client";

import { Toaster } from "react-hot-toast";
import { Sidebar } from "@/components/ui/Sidebar";

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <>
      <Toaster position="top-right" toastOptions={{ duration: 4000 }} />
      <Sidebar />
      <main className="ml-60 min-h-screen p-6">{children}</main>
    </>
  );
}
