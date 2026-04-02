import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { ToastProvider } from './contexts/ToastContext';
import { ErrorProvider } from './contexts/ErrorContext';
import ErrorDemo from './pages/ErrorDemo';
import PortfolioInquiry from './pages/PortfolioInquiry';
import TransactionHistory from './pages/TransactionHistory';

function Home() {
  return (
    <div style={{ maxWidth: 640, margin: '0 auto', padding: 24 }}>
      <h1>Portfolio Management System</h1>
      <nav>
        <ul style={{ listStyle: 'none', padding: 0 }}>
          <li style={{ marginBottom: 8 }}>
            <Link to="/portfolio">1. Portfolio Position Inquiry</Link>
          </li>
          <li style={{ marginBottom: 8 }}>
            <Link to="/history">2. Transaction History</Link>
          </li>
          <li style={{ marginBottom: 8 }}>
            <Link to="/error-demo">Error Handling Demo</Link>
          </li>
        </ul>
      </nav>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <ErrorProvider>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/portfolio" element={<PortfolioInquiry />} />
            <Route path="/history" element={<TransactionHistory />} />
            <Route path="/error-demo" element={<ErrorDemo />} />
          </Routes>
        </ErrorProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}

export default App
