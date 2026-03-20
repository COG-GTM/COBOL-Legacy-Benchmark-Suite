import { Briefcase } from 'lucide-react';

export default function PortfolioInquiry() {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <div className="h-16 w-16 rounded-2xl bg-blue-50 flex items-center justify-center mb-6">
        <Briefcase className="h-8 w-8 text-blue-600" />
      </div>
      <h1 className="text-2xl font-bold text-gray-900 mb-3">Portfolio Position Inquiry</h1>
      <p className="text-gray-500 max-w-md">
        Coming soon &mdash; this page will allow you to look up portfolio positions by account number.
      </p>
    </div>
  );
}
