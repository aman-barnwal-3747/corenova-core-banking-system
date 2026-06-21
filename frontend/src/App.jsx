/**
 * App.jsx – Root Router
 * All 16 routes wired — matches Image 2 sidebar exactly
 * PDF Phase 5: React Frontend
 */
import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './hooks/useAuth';
import AppShell from './components/layout/AppShell';

// Pages
import LoginPage        from './pages/LoginPage';
import DashboardPage    from './pages/DashboardPage';
import AccountsPage     from './pages/AccountsPage';
import TransactionsPage from './pages/TransactionsPage';
import FundTransferPage from './pages/FundTransferPage';
import BeneficiariesPage from './pages/BeneficiariesPage';
import PaymentsPage     from './pages/PaymentsPage';
import LoansPage        from './pages/LoansPage';
import CardsPage        from './pages/CardsPage';
import InvestmentsPage  from './pages/InvestmentsPage';
import ReportsPage      from './pages/ReportsPage';
import ApprovalsPage    from './pages/ApprovalsPage';
import UsersRolesPage   from './pages/UsersRolesPage';
import SettingsPage     from './pages/SettingsPage';
import AuditLogsPage    from './pages/AuditLogsPage';
import SupportPage      from './pages/SupportPage';
import CustomersPage    from './pages/CustomersPage';

function ProtectedRoute({ children }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? children : <Navigate to="/login" replace />;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<ProtectedRoute><AppShell /></ProtectedRoute>}>
        <Route index                element={<DashboardPage />} />
        <Route path="accounts"      element={<AccountsPage />} />
        <Route path="transactions"  element={<TransactionsPage />} />
        <Route path="fund-transfer" element={<FundTransferPage />} />
        <Route path="beneficiaries" element={<BeneficiariesPage />} />
        <Route path="payments"      element={<PaymentsPage />} />
        <Route path="loans"         element={<LoansPage />} />
        <Route path="cards"         element={<CardsPage />} />
        <Route path="investments"   element={<InvestmentsPage />} />
        <Route path="reports"       element={<ReportsPage />} />
        <Route path="approvals"     element={<ApprovalsPage />} />
        <Route path="users"         element={<UsersRolesPage />} />
        <Route path="settings"      element={<SettingsPage />} />
        <Route path="audit-logs"    element={<AuditLogsPage />} />
        <Route path="support"       element={<SupportPage />} />
        <Route path="customers"     element={<CustomersPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  );
}
