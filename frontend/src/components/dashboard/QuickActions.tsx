import { useNavigate } from "react-router-dom";
import { Briefcase, Clock, BarChart3, ArrowRight } from "lucide-react";

const actions = [
  {
    title: "Portfolio Position Inquiry",
    description: "Look up portfolio positions by account number",
    icon: <Briefcase size={28} className="text-blue-600" />,
    path: "/portfolio-inquiry",
    bgGradient: "from-blue-50 to-blue-100",
    borderColor: "border-blue-200",
    buttonColor: "bg-blue-600 hover:bg-blue-700",
  },
  {
    title: "Transaction History",
    description: "View transaction history for an account",
    icon: <Clock size={28} className="text-emerald-600" />,
    path: "/transaction-history",
    bgGradient: "from-emerald-50 to-emerald-100",
    borderColor: "border-emerald-200",
    buttonColor: "bg-emerald-600 hover:bg-emerald-700",
  },
  {
    title: "Reports",
    description: "View position, audit, and statistics reports",
    icon: <BarChart3 size={28} className="text-purple-600" />,
    path: "/reports",
    bgGradient: "from-purple-50 to-purple-100",
    borderColor: "border-purple-200",
    buttonColor: "bg-purple-600 hover:bg-purple-700",
  },
];

export default function QuickActions() {
  const navigate = useNavigate();

  return (
    <div>
      <h2 className="mb-4 text-lg font-semibold text-gray-900">
        Quick Actions
      </h2>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {actions.map((action) => (
          <div
            key={action.title}
            className={`rounded-lg border ${action.borderColor} bg-gradient-to-br ${action.bgGradient} p-6 shadow-sm transition-all hover:shadow-md`}
          >
            <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-lg bg-white shadow-sm">
              {action.icon}
            </div>
            <h3 className="mb-2 text-base font-semibold text-gray-900">
              {action.title}
            </h3>
            <p className="mb-4 text-sm text-gray-600">{action.description}</p>
            <button
              onClick={() => navigate(action.path)}
              className={`${action.buttonColor} inline-flex items-center gap-2 rounded-md px-4 py-2 text-sm font-medium text-white transition-colors`}
            >
              Open
              <ArrowRight size={14} />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
