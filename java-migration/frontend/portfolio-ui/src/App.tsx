import { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useLocation } from 'react-router-dom';
import { Briefcase, TrendingUp, History, Settings, FileText, Home } from 'lucide-react';
import Dashboard from './components/Dashboard';
import PortfolioList from './components/PortfolioList';
import PortfolioDetail from './components/PortfolioDetail';
import TransactionForm from './components/TransactionForm';
import TransactionHistory from './components/TransactionHistory';
import AdminPanel from './components/AdminPanel';
import AuditLogViewer from './components/AuditLogViewer';

function Navigation() {
  const location = useLocation();
  
  const navItems = [
    { path: '/', icon: Home, label: 'Dashboard' },
    { path: '/portfolios', icon: Briefcase, label: 'Portfolios' },
    { path: '/transactions/new', icon: TrendingUp, label: 'New Transaction' },
    { path: '/transactions', icon: History, label: 'Transaction History' },
    { path: '/admin', icon: Settings, label: 'Admin' },
    { path: '/audit', icon: FileText, label: 'Audit Log' },
  ];

  return (
    <nav className="bg-[#1E293B] border-b border-[#334155]">
      <div className="max-w-7xl mx-auto px-6">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center">
            <Briefcase className="h-8 w-8 text-[#22D3EE]" />
            <span className="ml-3 text-xl font-semibold text-white">Portfolio Management System</span>
          </div>
          <div className="flex space-x-1">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname === item.path || 
                (item.path !== '/' && location.pathname.startsWith(item.path));
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`flex items-center px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-[#22D3EE]/20 text-[#22D3EE]'
                      : 'text-[#CBD5E1] hover:bg-[#334155] hover:text-white'
                  }`}
                >
                  <Icon className="h-4 w-4 mr-2" />
                  {item.label}
                </Link>
              );
            })}
          </div>
        </div>
      </div>
    </nav>
  );
}

function App() {
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => setIsLoading(false), 500);
    return () => clearTimeout(timer);
  }, []);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#0F172A] flex items-center justify-center">
        <div className="text-center">
          <Briefcase className="h-16 w-16 text-[#22D3EE] mx-auto animate-pulse" />
          <p className="mt-4 text-[#CBD5E1]">Loading Portfolio Management System...</p>
        </div>
      </div>
    );
  }

  return (
    <Router>
      <div className="min-h-screen bg-[#0F172A]">
        <Navigation />
        <main className="max-w-7xl mx-auto px-6 py-8">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/portfolios" element={<PortfolioList />} />
            <Route path="/portfolios/:portfolioId" element={<PortfolioDetail />} />
            <Route path="/transactions/new" element={<TransactionForm />} />
            <Route path="/transactions" element={<TransactionHistory />} />
            <Route path="/admin" element={<AdminPanel />} />
            <Route path="/audit" element={<AuditLogViewer />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
