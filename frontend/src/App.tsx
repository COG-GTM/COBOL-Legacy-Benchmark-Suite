import { BrowserRouter, Routes, Route } from "react-router-dom";
import { ErrorProvider } from "./contexts/ErrorContext";
import { AuthProvider } from "./contexts/AuthContext";
import MainMenu from "./components/MainMenu";
import PortfolioView from "./components/PortfolioView";
import HistoryView from "./components/HistoryView";

export default function App() {
  return (
    <BrowserRouter>
      <ErrorProvider>
        <AuthProvider>
          <Routes>
            <Route path="/" element={<MainMenu />} />
            <Route path="/portfolio" element={<PortfolioView />} />
            <Route path="/history" element={<HistoryView />} />
          </Routes>
        </AuthProvider>
      </ErrorProvider>
    </BrowserRouter>
  );
}
