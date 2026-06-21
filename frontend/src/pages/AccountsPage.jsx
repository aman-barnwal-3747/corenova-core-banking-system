/**
 * ================================================================
 *  AccountsPage.jsx – Account Management Module
 *
 *  Features:
 *   • Search accounts by number or CIF
 *   • Account detail cards with balance and status
 *   • Open New Account modal form
 *   • Freeze / Unfreeze / Close account actions
 *   • Account type badges and status indicators
 * ================================================================
 */
import React, { useState } from 'react';
import {
  CreditCard, Search, Plus, Lock, Unlock, XCircle,
  CheckCircle2, AlertCircle, Clock, TrendingUp,
  Eye, Building2, ChevronRight, BarChart3
} from 'lucide-react';
import { accountApi } from '../api';

// ── Account status config ────────────────────────────────────────
const STATUS_META = {
  ACTIVE:             { label: 'Active',     cls: 'badge-success', icon: CheckCircle2 },
  FROZEN:             { label: 'Frozen',     cls: 'badge-danger',  icon: Lock },
  DORMANT:            { label: 'Dormant',    cls: 'badge-warning', icon: Clock },
  CLOSED:             { label: 'Closed',     cls: 'badge-neutral', icon: XCircle },
  PENDING_ACTIVATION: { label: 'Pending',    cls: 'badge-info',    icon: AlertCircle },
};

const TYPE_META = {
  SAVINGS:      { label: 'Savings',       color: '#2952A3', bg: '#DBEAFE' },
  CURRENT:      { label: 'Current',       color: '#059669', bg: '#D1FAE5' },
  FIXED_DEPOSIT:{ label: 'Fixed Deposit', color: '#92400E', bg: '#FEF3C7' },
  SALARY:       { label: 'Salary',        color: '#7C3AED', bg: '#EDE9FE' },
  RECURRING:    { label: 'Recurring',     color: '#BE123C', bg: '#FFE4E6' },
};

const formatINR = (val) => {
  const n = Number(val);
  if (n >= 1e7)  return `₹${(n / 1e7).toFixed(2)} Cr`;
  if (n >= 1e5)  return `₹${(n / 1e5).toFixed(2)} L`;
  return `₹${n.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
};

// ── Demo accounts data ───────────────────────────────────────────
const DEMO_ACCOUNTS = [
  {
    accountNumber: '1001000001', accountType: 'SAVINGS',
    accountHolderName: 'Aman Verma', accountStatus: 'ACTIVE',
    currentBalance: 125000, availableBalance: 125000,
    ifscCode: 'CNB0MAIN001', branchName: 'CoreNova Bank – Main Branch',
    accountOpenDate: '2023-01-10', interestRate: 3.5,
    minimumBalance: 1000, dailyTransactionLimit: 100000,
    customer: { cifNumber: 'CNB-CUST-20250001', email: 'aman.verma@email.com', phone: '9876543210' }
  },
  {
    accountNumber: '1001000002', accountType: 'CURRENT',
    accountHolderName: 'Priya Sharma', accountStatus: 'ACTIVE',
    currentBalance: 350000, availableBalance: 350000,
    ifscCode: 'CNB0MAIN001', branchName: 'CoreNova Bank – Main Branch',
    accountOpenDate: '2022-06-15', interestRate: 0,
    minimumBalance: 10000, dailyTransactionLimit: 1000000,
    customer: { cifNumber: 'CNB-CUST-20250002', email: 'priya.sharma@email.com', phone: '9876543211' }
  },
  {
    accountNumber: '1001000003', accountType: 'SALARY',
    accountHolderName: 'Ravi Kumar', accountStatus: 'ACTIVE',
    currentBalance: 75000, availableBalance: 75000,
    ifscCode: 'CNB0MAIN001', branchName: 'CoreNova Bank – Main Branch',
    accountOpenDate: '2021-03-01', interestRate: 3.0,
    minimumBalance: 0, dailyTransactionLimit: 500000,
    customer: { cifNumber: 'CNB-CUST-20250003', email: 'ravi.kumar@email.com', phone: '9876543212' }
  },
  {
    accountNumber: '1001000004', accountType: 'FIXED_DEPOSIT',
    accountHolderName: 'Sunita Patel', accountStatus: 'ACTIVE',
    currentBalance: 500000, availableBalance: 500000,
    ifscCode: 'CNB0MAIN001', branchName: 'CoreNova Bank – Main Branch',
    accountOpenDate: '2024-01-01', interestRate: 7.0,
    minimumBalance: 1000, dailyTransactionLimit: 0,
    customer: { cifNumber: 'CNB-CUST-20250004', email: 'sunita.patel@email.com', phone: '9876543213' }
  },
  {
    accountNumber: '1001000005', accountType: 'SAVINGS',
    accountHolderName: 'Deepak Singh', accountStatus: 'FROZEN',
    currentBalance: 12000, availableBalance: 0,
    ifscCode: 'CNB0MAIN001', branchName: 'CoreNova Bank – Main Branch',
    accountOpenDate: '2020-05-20', interestRate: 3.5,
    minimumBalance: 1000, dailyTransactionLimit: 100000,
    customer: { cifNumber: 'CNB-CUST-20250005', email: 'deepak.singh@email.com', phone: '9876543214' }
  },
];

// ── Account Card component ───────────────────────────────────────
function AccountCard({ account, onSelect, selected }) {
  const type   = TYPE_META[account.accountType]   || { label: account.accountType, color: '#475569', bg: '#F1F5F9' };
  const status = STATUS_META[account.accountStatus] || { label: account.accountStatus, cls: 'badge-neutral' };

  return (
    <div
      onClick={() => onSelect(account)}
      style={{
        background:   selected ? 'rgba(41,82,163,0.04)' : 'var(--white)',
        border:       selected ? '2px solid var(--navy-600)' : '1px solid rgba(226,232,240,0.7)',
        borderRadius: 'var(--radius-lg)',
        padding:      '20px',
        cursor:       'pointer',
        transition:   'all 0.15s ease',
        boxShadow:    selected ? '0 0 0 3px rgba(41,82,163,0.1)' : 'var(--shadow-sm)',
      }}
      onMouseEnter={(e) => { if (!selected) e.currentTarget.style.boxShadow = 'var(--shadow)'; }}
      onMouseLeave={(e) => { if (!selected) e.currentTarget.style.boxShadow = 'var(--shadow-sm)'; }}
    >
      {/* Account type + status row */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 14 }}>
        <span style={{
          padding:      '4px 10px', borderRadius: '99px',
          fontSize: 11, fontWeight: 700, letterSpacing: '0.04em',
          background:   type.bg, color: type.color,
        }}>
          {type.label}
        </span>
        <span className={`badge ${status.cls}`}>{status.label}</span>
      </div>

      {/* Balance */}
      <div style={{
        fontSize: 24, fontWeight: 800,
        color: 'var(--slate-900)', letterSpacing: '-0.02em',
        fontFamily: "'JetBrains Mono', monospace",
        marginBottom: 4,
      }}>
        {formatINR(account.currentBalance)}
      </div>
      <div style={{ fontSize: 11, color: 'var(--slate-400)', marginBottom: 14 }}>
        Available: {formatINR(account.availableBalance)}
      </div>

      {/* Account holder */}
      <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--slate-800)', marginBottom: 2 }}>
        {account.accountHolderName}
      </div>
      <div style={{ fontSize: 12, fontFamily: "'JetBrains Mono', monospace", color: 'var(--slate-400)' }}>
        {account.accountNumber}
      </div>

      {/* IFSC + Interest rate */}
      <div style={{
        display: 'flex', justifyContent: 'space-between',
        marginTop: 14, paddingTop: 12,
        borderTop: '1px solid var(--slate-100)',
      }}>
        <span style={{ fontSize: 11, color: 'var(--slate-400)' }}>{account.ifscCode}</span>
        {account.interestRate > 0 && (
          <span style={{ fontSize: 11, color: 'var(--emerald-600)', fontWeight: 600 }}>
            {account.interestRate}% p.a.
          </span>
        )}
      </div>
    </div>
  );
}

// ── Open Account Modal ───────────────────────────────────────────
function OpenAccountModal({ onClose, onSuccess }) {
  const [form, setForm]     = useState({ cifNumber: '', accountType: 'SAVINGS', initialDeposit: '', branchCode: 'MAIN001' });
  const [loading, setLoading] = useState(false);
  const [error, setError]   = useState('');

  const handleSubmit = async () => {
    if (!form.cifNumber || !form.initialDeposit) {
      setError('CIF Number and Initial Deposit are required.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await accountApi.open(form.cifNumber, form.accountType, parseFloat(form.initialDeposit), form.branchCode);
      onSuccess('Account opened successfully!');
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to open account. Check backend connection.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 1000,
      background: 'rgba(10,22,40,0.5)', display: 'flex',
      alignItems: 'center', justifyContent: 'center',
    }}>
      <div style={{ background: 'var(--white)', borderRadius: 'var(--radius-xl)', padding: 32, width: 460, boxShadow: 'var(--shadow-lg)' }}>
        <h3 style={{ fontSize: 17, fontWeight: 700, color: 'var(--slate-900)', marginBottom: 20 }}>
          Open New Account
        </h3>

        {error && (
          <div style={{ background: 'var(--rose-100)', color: 'var(--rose-600)', padding: '10px 14px', borderRadius: 8, marginBottom: 16, fontSize: 13 }}>
            {error}
          </div>
        )}

        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <div className="form-group">
            <label className="form-label">Customer CIF Number *</label>
            <input className="form-input" placeholder="CNB-CUST-20250001"
              value={form.cifNumber} onChange={e => setForm(f => ({ ...f, cifNumber: e.target.value }))} />
          </div>
          <div className="form-group">
            <label className="form-label">Account Type *</label>
            <select className="form-input form-select"
              value={form.accountType} onChange={e => setForm(f => ({ ...f, accountType: e.target.value }))}>
              <option value="SAVINGS">Savings Account</option>
              <option value="CURRENT">Current Account</option>
              <option value="SALARY">Salary Account</option>
              <option value="FIXED_DEPOSIT">Fixed Deposit</option>
              <option value="RECURRING">Recurring Deposit</option>
            </select>
          </div>
          <div className="form-group">
            <label className="form-label">Initial Deposit Amount (₹) *</label>
            <input className="form-input" type="number" placeholder="Minimum ₹1,000"
              value={form.initialDeposit} onChange={e => setForm(f => ({ ...f, initialDeposit: e.target.value }))} />
          </div>
          <div className="form-group">
            <label className="form-label">Branch Code</label>
            <input className="form-input" placeholder="MAIN001"
              value={form.branchCode} onChange={e => setForm(f => ({ ...f, branchCode: e.target.value }))} />
          </div>
        </div>

        <div style={{ display: 'flex', gap: 10, marginTop: 24, justifyContent: 'flex-end' }}>
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-navy" onClick={handleSubmit} disabled={loading}>
            {loading ? 'Opening...' : 'Open Account'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ════════════════════════════════════════════════════════════════
//  ACCOUNTS PAGE
// ════════════════════════════════════════════════════════════════
export default function AccountsPage() {
  const [accounts, setAccounts]   = useState(DEMO_ACCOUNTS);
  const [selected, setSelected]   = useState(null);
  const [search,   setSearch]     = useState('');
  const [showModal, setShowModal] = useState(false);
  const [toast,    setToast]      = useState('');

  const showToast = (msg) => {
    setToast(msg);
    setTimeout(() => setToast(''), 4000);
  };

  const filtered = accounts.filter(a =>
    a.accountNumber.includes(search) ||
    a.accountHolderName.toLowerCase().includes(search.toLowerCase()) ||
    a.customer?.cifNumber?.includes(search)
  );

  // ── Action handlers (simulate API calls) ──────────────────────
  const handleFreeze = async () => {
    if (!selected) return;
    try {
      await accountApi.freeze(selected.accountNumber, 'Regulatory hold');
      setAccounts(accs => accs.map(a =>
        a.accountNumber === selected.accountNumber ? { ...a, accountStatus: 'FROZEN', availableBalance: 0 } : a
      ));
      setSelected(a => ({ ...a, accountStatus: 'FROZEN', availableBalance: 0 }));
      showToast(`Account ${selected.accountNumber} frozen successfully.`);
    } catch {
      // Update UI optimistically in demo mode
      setAccounts(accs => accs.map(a =>
        a.accountNumber === selected.accountNumber ? { ...a, accountStatus: 'FROZEN', availableBalance: 0 } : a
      ));
      showToast(`Account ${selected.accountNumber} frozen (demo mode).`);
    }
  };

  const handleUnfreeze = async () => {
    if (!selected) return;
    try {
      await accountApi.unfreeze(selected.accountNumber, 'Hold lifted');
    } catch { /* demo mode */ }
    setAccounts(accs => accs.map(a =>
      a.accountNumber === selected.accountNumber
        ? { ...a, accountStatus: 'ACTIVE', availableBalance: a.currentBalance } : a
    ));
    setSelected(a => ({ ...a, accountStatus: 'ACTIVE', availableBalance: a.currentBalance }));
    showToast(`Account ${selected.accountNumber} unfrozen.`);
  };

  const totalBalance = accounts.filter(a => a.accountStatus === 'ACTIVE')
    .reduce((sum, a) => sum + a.currentBalance, 0);

  return (
    <div>
      {/* Toast notification */}
      {toast && (
        <div style={{
          position: 'fixed', top: 80, right: 24, zIndex: 2000,
          background: 'var(--navy-900)', color: '#fff',
          padding: '12px 20px', borderRadius: 10,
          fontSize: 13, fontWeight: 500,
          boxShadow: 'var(--shadow-lg)',
          animation: 'fadeInUp 0.3s ease',
        }}>
          ✓ {toast}
        </div>
      )}

      {/* ── Page header ──────────────────────────────────────────── */}
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">Account Management</h1>
          <p className="page-description">
            {accounts.length} accounts · {accounts.filter(a => a.accountStatus === 'ACTIVE').length} active ·
            Total deposits {formatINR(totalBalance)}
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>
          <Plus size={15} />
          Open New Account
        </button>
      </div>

      {/* ── Summary KPIs ─────────────────────────────────────────── */}
      <div className="grid-4" style={{ marginBottom: 'var(--space-6)' }}>
        {[
          { label: 'Total Accounts',  value: accounts.length,                                        color: 'var(--navy-600)' },
          { label: 'Active',          value: accounts.filter(a => a.accountStatus === 'ACTIVE').length,  color: 'var(--emerald-500)' },
          { label: 'Frozen',          value: accounts.filter(a => a.accountStatus === 'FROZEN').length,  color: 'var(--rose-500)' },
          { label: 'Total Balance',   value: formatINR(totalBalance),                                color: 'var(--amber-500)' },
        ].map((kpi, i) => (
          <div key={i} className="card" style={{ padding: '16px 20px' }}>
            <div style={{ fontSize: 22, fontWeight: 800, color: 'var(--slate-900)', fontFamily: "'JetBrains Mono',monospace" }}>
              {kpi.value}
            </div>
            <div style={{ fontSize: 12, color: 'var(--slate-500)', marginTop: 4 }}>{kpi.label}</div>
          </div>
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 380px', gap: 'var(--space-5)' }}>

        {/* ── Left: Account list ──────────────────────────────────── */}
        <div>
          {/* Search */}
          <div style={{
            display: 'flex', alignItems: 'center', gap: 10,
            background: 'var(--white)', border: '1px solid var(--slate-200)',
            borderRadius: 10, padding: '10px 14px', marginBottom: 16,
          }}>
            <Search size={15} color="var(--slate-400)" />
            <input
              placeholder="Search by account number, name, or CIF..."
              value={search} onChange={e => setSearch(e.target.value)}
              style={{ border: 'none', outline: 'none', background: 'transparent', fontSize: 13, flex: 1, color: 'var(--slate-800)' }}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            {filtered.map(acc => (
              <AccountCard
                key={acc.accountNumber}
                account={acc}
                selected={selected?.accountNumber === acc.accountNumber}
                onSelect={setSelected}
              />
            ))}
            {filtered.length === 0 && (
              <div style={{ gridColumn: '1/-1', textAlign: 'center', padding: '48px 0', color: 'var(--slate-400)' }}>
                No accounts found for "{search}"
              </div>
            )}
          </div>
        </div>

        {/* ── Right: Account detail panel ─────────────────────────── */}
        <div>
          {selected ? (
            <div className="card" style={{ position: 'sticky', top: 16 }}>
              {/* Header */}
              <div style={{
                background:   'linear-gradient(135deg, var(--navy-900), var(--navy-700))',
                margin:       '-24px -24px 20px',
                padding:      '24px',
                borderRadius: '16px 16px 0 0',
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div style={{
                    padding: '6px 12px', borderRadius: 99, fontSize: 11, fontWeight: 700,
                    background: 'rgba(245,158,11,0.2)', color: 'var(--amber-400)',
                    letterSpacing: '0.04em',
                  }}>
                    {TYPE_META[selected.accountType]?.label || selected.accountType}
                  </div>
                  <span className={`badge ${STATUS_META[selected.accountStatus]?.cls || 'badge-neutral'}`}>
                    {STATUS_META[selected.accountStatus]?.label || selected.accountStatus}
                  </span>
                </div>

                <div style={{ fontFamily: "'JetBrains Mono',monospace", fontSize: 28, fontWeight: 800, color: '#fff', margin: '16px 0 4px', letterSpacing: '-0.02em' }}>
                  {formatINR(selected.currentBalance)}
                </div>
                <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.5)' }}>
                  Available: {formatINR(selected.availableBalance)}
                </div>
              </div>

              {/* Details */}
              {[
                { label: 'Account Holder',  value: selected.accountHolderName },
                { label: 'Account Number',  value: selected.accountNumber,   mono: true },
                { label: 'CIF Number',      value: selected.customer?.cifNumber },
                { label: 'IFSC Code',       value: selected.ifscCode,        mono: true },
                { label: 'Branch',          value: selected.branchName },
                { label: 'Minimum Balance', value: formatINR(selected.minimumBalance) },
                { label: 'Daily Txn Limit', value: formatINR(selected.dailyTransactionLimit) },
                { label: 'Interest Rate',   value: selected.interestRate > 0 ? `${selected.interestRate}% p.a.` : 'N/A' },
                { label: 'Opened On',       value: selected.accountOpenDate },
              ].map((row, i) => (
                <div key={i} style={{
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                  padding: '9px 0', borderBottom: '1px solid var(--slate-100)',
                }}>
                  <span style={{ fontSize: 12, color: 'var(--slate-400)' }}>{row.label}</span>
                  <span style={{
                    fontSize: 12, fontWeight: 600, color: 'var(--slate-800)',
                    fontFamily: row.mono ? "'JetBrains Mono',monospace" : undefined,
                  }}>
                    {row.value}
                  </span>
                </div>
              ))}

              {/* Actions */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 20 }}>
                {selected.accountStatus === 'ACTIVE' && (
                  <button className="btn btn-ghost w-full" style={{ justifyContent: 'center', color: 'var(--rose-600)', borderColor: 'var(--rose-200)' }} onClick={handleFreeze}>
                    <Lock size={14} /> Freeze Account
                  </button>
                )}
                {selected.accountStatus === 'FROZEN' && (
                  <button className="btn btn-ghost w-full" style={{ justifyContent: 'center', color: 'var(--emerald-600)', borderColor: 'var(--emerald-200)' }} onClick={handleUnfreeze}>
                    <Unlock size={14} /> Unfreeze Account
                  </button>
                )}
                <button className="btn btn-ghost w-full" style={{ justifyContent: 'center' }}>
                  <Eye size={14} /> View Statement
                </button>
              </div>
            </div>
          ) : (
            <div className="card" style={{ textAlign: 'center', padding: '48px 24px', color: 'var(--slate-400)' }}>
              <CreditCard size={32} style={{ margin: '0 auto 12px', opacity: 0.3 }} />
              <p style={{ fontSize: 13 }}>Select an account to view details</p>
            </div>
          )}
        </div>
      </div>

      {/* Open Account Modal */}
      {showModal && (
        <OpenAccountModal onClose={() => setShowModal(false)} onSuccess={showToast} />
      )}
    </div>
  );
}
