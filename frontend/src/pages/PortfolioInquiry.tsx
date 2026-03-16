import { Briefcase } from "lucide-react";

export default function PortfolioInquiry() {
  return (
    <div className="flex flex-col items-center justify-center py-20">
      <div className="mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-blue-100">
        <Briefcase size={32} className="text-blue-600" />
      </div>
      <h1 className="mb-2 text-2xl font-bold text-gray-900">
        Portfolio Position Inquiry
      </h1>
      <p className="mb-4 text-gray-500">
        Look up portfolio positions by account number
      </p>
      <span className="rounded-full bg-blue-100 px-4 py-2 text-sm font-medium text-blue-700">
        Coming Soon
      </span>
      <p className="mt-6 max-w-md text-center text-sm text-gray-400">
        This page will replace the POSMAP screen from the original CICS application,
        allowing you to search and view portfolio positions with a modern interface.
      </p>
    </div>
  );
}
