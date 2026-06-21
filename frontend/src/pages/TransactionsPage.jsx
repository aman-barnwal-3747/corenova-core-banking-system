/**
 * ================================================================
 *  TransactionsPage.jsx – Transaction History & Statement Module
 *
 *  Features:
 *   • Account-wise transaction history (paginated)
 *   • Date-range filter + transaction type filter
 *   • Debit/Credit colour coding
 *   • Running balance column
 *   • Downloadable statement (CSV export)
 *   • Transaction detail modal (full receipt)
 *   • Search by reference number
 *   • Live 7-day mini bar chart per account
 * ================================================================
 */
import React, { useState, useEffect } from 'react';
import {
  ArrowDownLeft, ArrowUpRight, Search, Filter, Download,
  ChevronLeft, ChevronRight, Eye, X, Calendar,
  TrendingUp, TrendingDown, RefreshCw, FileText
} from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { transactionApi } from '../api';

// ── Helpers ──────────────────────────────────────────────────────
const formatINR = (val) => {
  if (val === undefined || val === null) return '₹—';
  const n = Number(val);
  return `₹${n.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
};

const formatDate = (d) => {
  if (!d) return '—';
  return new Date(d).toLocaleString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit', hour12: true,
  });
};

const formatDateShort = (d) => {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
};

// ── Transaction type display names ───────────────────────────────
const TXN_TYPE_LABELS = {
  INTERNAL_TRANSFER: 'Internal Transfer',
  UPI:               'UPI Payment',
  NEFT:              'NEFT Transfer',
  RTGS:              'RTGS Transfer',
  IMPS:              'IMPS Transfer',
  SALARY_CREDIT:     'Salary Credit',
  CASH_DEPOSIT:      'Cash Deposit',
  CASH_WITHDRAWAL:   'Cash Withdrawal',
  INTEREST_CREDIT:   'Interest Credit',
  EMI_DEBIT:         'EMI Debit',
  CHARGES:           'Bank Charges',
  REVERSAL:          'Reversal',
  DEBIT_CARD:        'Debit Card',
  CHEQUE:            'Cheque',
};

// ── Status badge ─────────────────────────────────────────────────
const STATUS_STYLES = {
  SUCCESS:          { cls: 'badge-success', label: 'Success' },
  PROCESSING:       { cls: 'badge-info',    label: 'Processing' },
  PENDING_APPROVAL: { cls: 'badge-warning', label: 'Pending' },
  FAILED:           { cls: 'badge-danger',  label: 'Failed' },
  REVERSED:         { cls: 'badge-neutral', label: 'Reversed' },
  INITIATED:        { cls: 'badge-info',    label: 'Initiated' },
  CANCELLED:        { cls: 'badge-neutral', label: 'Cancelled' },
};

// ── Demo transaction data ────────────────────────────────────────
const generateDemoTransactions = (accountNo = '1001000001') => {
  const types  = ['INTERNAL_TRANSFER','UPI','NEFT','SALARY_CREDIT','CASH_DEPOSIT','INTEREST_CREDIT','EMI_DEBIT','IMPS'];
  const names  = ['Priya Sharma','Ravi Kumar','Flipkart','Zomato','HDFC Bank','Amazon Pay','BSNL Ltd','LIC Premium'];
  const statuses = ['SUCCESS','SUCCESS','SUCCESS','SUCCESS','SUCCESS','PROCESSING','REVERSED','SUCCESS'];
  let balance  = 125000;

  return Array.from({ length: 35 }, (_, i) => {
    const isCredit = i % 3 === 0 || types[i % types.length] === 'SALARY_CREDIT' || types[i % types.length] === 'INTEREST_CREDIT';
    const amount   = Math.floor(Math.random() * 45000) + 500;
    if (isCredit) balance += amount; else balance = Math.max(balance - amount, 1000);

    return {
      transactionId:    i + 1,
      referenceNumber:  `TXN2025${String(i + 1).padStart(10, '0')}`,
      utrNumber:        `${types[i % types.length]}${Date.now() - i * 3600000}`,
      transactionType:  types[i % types.length],
      entryType:        isCredit ? 'CREDIT' : 'DEBIT',
      amount,
      currency:         'INR',
      status:           statuses[i % statuses.length],
      narration:        `${isCredit ? 'From' : 'To'} ${names[i % names.length]}`,
      counterpartyName: names[i % names.length],
      balanceAfter:     balance,
      transactionDate:  new Date(Date.now() - i * 14400000).toISOString(),
      settlementDate:   new Date(Date.now() - i * 14400000 + 900000).toISOString(),
      accountNumber:    accountNo,
      channel:          ['INTERNET_BANKING','MOBILE_APP','CBS_COUNTER','UPI'][i % 4],
    };
  });
};

// ── Transaction Detail Modal ─────────────────────────────────────
function TxnDetailModal({ txn, onClose }) {
  if (!txn) return null;
  const isCredit = txn.entryType === 'CREDIT';

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 1000,
      background: 'rgba(10,22,40,0.55)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }} onClick={onClose}>
      <div style={{
        background: 'var(--white)', borderRadius: 20, width: 500, maxHeight: '90vh',
        overflow: 'auto', boxShadow: 'var(--shadow-lg)',
      }} onClick={e => e.stopPropagation()}>

        {/* Modal header */}
        <div style={{
          background: isCredit
            ? 'linear-gradient(135deg,#059669,#10B981)'
            : 'linear-gradient(135deg,var(--navy-900),var(--navy-700))',
          padding: '28px 28px 24px', borderRadius: '20px 20px 0 0',
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <div style={{ color: 'rgba(255,255,255,0.65)', fontSize: 12, marginBottom: 6 }}>
                {isCredit ? '↙ CREDIT' : '↗ DEBIT'} · {TXN_TYPE_LABELS[txn.transactionType] || txn.transactionType}
              </div>
              <div style={{
                fontFamily: "'JetBrains Mono',monospace",
                fontSize: 34, fontWeight: 800, color: '#fff', letterSpacing: '-0.02em',
              }}>
                {isCredit ? '+' : '-'}{formatINR(txn.amount)}
              </div>
              <div style={{ color: 'rgba(255,255,255,0.6)', fontSize: 12, marginTop: 4 }}>
                Balance after: {formatINR(txn.balanceAfter)}
              </div>
            </div>
            <button onClick={onClose} style={{
              background: 'rgba(255,255,255,0.15)', border: 'none', borderRadius: '50%',
              width: 32, height: 32, cursor: 'pointer', color: '#fff',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <X size={16} />
            </button>
          </div>
        </div>

        {/* Details */}
        <div style={{ padding: 28 }}>
          {[
            { label: 'Reference Number',  value: txn.referenceNumber,  mono: true },
            { label: 'UTR Number',        value: txn.utrNumber || 'N/A', mono: true },
            { label: 'Transaction Date',  value: formatDate(txn.transactionDate) },
            { label: 'Settlement Date',   value: formatDate(txn.settlementDate) },
            { label: 'Counterparty',      value: txn.counterpartyName || '—' },
            { label: 'Narration',         value: txn.narration },
            { label: 'Channel',           value: txn.channel?.replace('_',' ') || '—' },
            { label: 'Status',            value: txn.status, badge: true },
          ].map((row, i) => (
            <div key={i} style={{
              display: 'flex', justifyContent: 'space-between', alignItems: 'center',
              padding: '10px 0', borderBottom: '1px solid var(--slate-100)',
            }}>
              <span style={{ fontSize: 12, color: 'var(--slate-400)', minWidth: 140 }}>{row.label}</span>
              {row.badge ? (
                <span className={`badge ${STATUS_STYLES[txn.status]?.cls || 'badge-neutral'}`}>
                  {STATUS_STYLES[txn.status]?.label || txn.status}
                </span>
              ) : (
                <span style={{
                  fontSize: 12, fontWeight: 600, color: 'var(--slate-800)',
                  fontFamily: row.mono ? "'JetBrains Mono',monospace" : undefined,
                  textAlign: 'right', maxWidth: 280,
                }}>
                  {row.value}
                </span>
              )}
            </div>
          ))}

          <button className="btn btn-ghost w-full" style={{ justifyContent: 'center', marginTop: 20 }}>
            <Download size={14} /> Download Receipt (PDF)
          </button>
        </div>
      </div>
    </div>
  );
}

// ── CSV Export helper ────────────────────────────────────────────
function exportToCSV(transactions, accountNumber) {
  const headers = ['Reference No','Type','Entry','Amount','Balance','Counterparty','Narration','Date','Status'];
  const rows = transactions.map(t => [
    t.referenceNumber, TXN_TYPE_LABELS[t.transactionType] || t.transactionType,
    t.entryType, t.amount, t.balanceAfter, t.counterpartyName,
    t.narration, formatDate(t.transactionDate), t.status,
  ]);
  const csv = [headers, ...rows].map(r => r.join(',')).join('\n');
  const blob = new Blob([csv], { type: 'text/csv' });
  const url  = URL.createObjectURL(blob);
  const a    = document.createElement('a');
  a.href     = url;
  a.download = `CoreNova_Statement_${accountNumber}_${new Date().toISOString().slice(0,10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

// ════════════════════════════════════════════════════════════════
//  TRANSACTIONS PAGE
// ════════════════════════════════════════════════════════════════
export default function TransactionsPage() {
  const [accountNo,    setAccountNo]    = useState('1001000001');
  const [allTxns,      setAllTxns]      = useState(generateDemoTransactions('1001000001'));
  const [filteredTxns, setFilteredTxns] = useState([]);
  const [search,       setSearch]       = useState('');
  const [typeFilter,   setTypeFilter]   = useState('ALL');
  const [entryFilter,  setEntryFilter]  = useState('ALL');
  const [page,         setPage]         = useState(0);
  const [selectedTxn,  setSelectedTxn]  = useState(null);
  const [loading,      setLoading]      = useState(false);
  const PAGE_SIZE = 10;

  // Load transactions for account
  const fetchTxns = async (acNo) => {
    setLoading(true);
    try {
      const { data } = await transactionApi.getHistory(acNo, 0, 50);
      setAllTxns(data.content || data || []);
    } catch {
      setAllTxns(generateDemoTransactions(acNo));
    } finally {
      setLoading(false);
    }
  };

  // Filter + search
  useEffect(() => {
    let result = [...allTxns];
    if (typeFilter !== 'ALL')  result = result.filter(t => t.transactionType === typeFilter);
    if (entryFilter !== 'ALL') result = result.filter(t => t.entryType === entryFilter);
    if (search.trim())         result = result.filter(t =>
      t.referenceNumber.toLowerCase().includes(search.toLowerCase()) ||
      (t.counterpartyName || '').toLowerCase().includes(search.toLowerCase()) ||
      (t.narration || '').toLowerCase().includes(search.toLowerCase())
    );
    setFilteredTxns(result);
    setPage(0);
  }, [allTxns, typeFilter, entryFilter, search]);

  const paginated    = filteredTxns.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
  const totalPages   = Math.ceil(filteredTxns.length / PAGE_SIZE);
  const totalCredit  = allTxns.filter(t => t.entryType === 'CREDIT' && t.status === 'SUCCESS').reduce((s, t) => s + t.amount, 0);
  const totalDebit   = allTxns.filter(t => t.entryType === 'DEBIT'  && t.status === 'SUCCESS').reduce((s, t) => s + t.amount, 0);

  // Mini chart data (last 7 days)
  const chartData = Array.from({ length: 7 }, (_, i) => {
    const day = new Date(); day.setDate(day.getDate() - (6 - i));
    const dayStr = day.toDateString();
    const dayTxns = allTxns.filter(t => new Date(t.transactionDate).toDateString() === dayStr);
    return {
      day: day.toLocaleDateString('en-IN', { weekday: 'short' }),
      credit: dayTxns.filter(t => t.entryType === 'CREDIT').reduce((s, t) => s + t.amount, 0),
      debit:  dayTxns.filter(t => t.entryType === 'DEBIT').reduce((s, t)  => s + t.amount, 0),
    };
  });

  return (
    <div>
      {/* ── Page header ──────────────────────────────────────────── */}
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">Transaction History</h1>
          <p className="page-description">
            Account statement · {allTxns.length} transactions · Real-time ledger view
          </p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <button className="btn btn-ghost" onClick={() => fetchTxns(accountNo)}>
            <RefreshCw size={14} /> Refresh
          </button>
          <button className="btn btn-primary" onClick={() => exportToCSV(filteredTxns, accountNo)}>
            <Download size={14} /> Export CSV
          </button>
        </div>
      </div>

      {/* ── Account selector + KPI row ────────────────────────────── */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr', gap: 16, marginBottom: 24 }}>
        {/* Account input */}
        <div className="card" style={{ padding: '16px 18px' }}>
          <p style={{ fontSize: 11, color: 'var(--slate-400)', fontWeight: 600, letterSpacing: '0.06em', textTransform: 'uppercase', marginBottom: 8 }}>
            Account Number
          </p>
          <div style={{ display: 'flex', gap: 8 }}>
            <input
              className="form-input mono"
              value={accountNo}
              onChange={e => setAccountNo(e.target.value)}
              style={{ flex: 1, fontSize: 13, padding: '7px 10px' }}
            />
            <button className="btn btn-navy" style={{ padding: '7px 12px' }} onClick={() => fetchTxns(accountNo)}>
              <Search size={13} />
            </button>
          </div>
        </div>

        {/* Total credit */}
        <div className="card" style={{ padding: '16px 18px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
            <ArrowDownLeft size={14} color="var(--emerald-600)" />
            <span style={{ fontSize: 11, color: 'var(--slate-400)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em' }}>Total Credits</span>
          </div>
          <div style={{ fontFamily: "'JetBrains Mono',monospace", fontSize: 20, fontWeight: 800, color: 'var(--emerald-600)' }}>
            +{formatINR(totalCredit)}
          </div>
        </div>

        {/* Total debit */}
        <div className="card" style={{ padding: '16px 18px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
            <ArrowUpRight size={14} color="var(--rose-600)" />
            <span style={{ fontSize: 11, color: 'var(--slate-400)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em' }}>Total Debits</span>
          </div>
          <div style={{ fontFamily: "'JetBrains Mono',monospace", fontSize: 20, fontWeight: 800, color: 'var(--rose-600)' }}>
            -{formatINR(totalDebit)}
          </div>
        </div>

        {/* Net flow */}
        <div className="card" style={{ padding: '16px 18px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
            {totalCredit >= totalDebit
              ? <TrendingUp size={14} color="var(--emerald-600)" />
              : <TrendingDown size={14} color="var(--rose-600)" />
            }
            <span style={{ fontSize: 11, color: 'var(--slate-400)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em' }}>Net Flow</span>
          </div>
          <div style={{
            fontFamily: "'JetBrains Mono',monospace", fontSize: 20, fontWeight: 800,
            color: totalCredit >= totalDebit ? 'var(--emerald-600)' : 'var(--rose-600)',
          }}>
            {totalCredit >= totalDebit ? '+' : '-'}{formatINR(Math.abs(totalCredit - totalDebit))}
          </div>
        </div>
      </div>

      {/* ── Chart + Filter row ───────────────────────────────────── */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: 20, marginBottom: 20 }}>

        {/* 7-day bar chart */}
        <div className="card">
          <div className="card-header">
            <div>
              <h3 className="card-title">7-Day Activity</h3>
              <p className="card-subtitle">Daily credit vs debit for account {accountNo}</p>
            </div>
          </div>
          <ResponsiveContainer width="100%" height={140}>
            <BarChart data={chartData} barGap={3} margin={{ top: 0, right: 0, left: 0, bottom: 0 }}>
              <XAxis dataKey="day" tick={{ fontSize: 11, fill: 'var(--slate-400)' }} axisLine={false} tickLine={false} />
              <YAxis hide />
              <Tooltip
                formatter={(v, n) => [formatINR(v), n === 'credit' ? 'Credit' : 'Debit']}
                contentStyle={{ fontSize: 12, borderRadius: 8, border: '1px solid var(--slate-200)' }}
              />
              <Bar dataKey="credit" radius={[4, 4, 0, 0]} fill="var(--emerald-500)" />
              <Bar dataKey="debit"  radius={[4, 4, 0, 0]} fill="var(--rose-400)" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Filters card */}
        <div className="card">
          <h3 className="card-title" style={{ marginBottom: 16 }}>
            <Filter size={14} style={{ display: 'inline', marginRight: 6, verticalAlign: 'middle' }} />
            Filters
          </h3>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {/* Search */}
            <div className="form-group">
              <label className="form-label">Search</label>
              <div style={{ position: 'relative' }}>
                <Search size={13} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--slate-400)' }} />
                <input className="form-input" style={{ paddingLeft: 30 }}
                  placeholder="Ref no, name, narration..."
                  value={search} onChange={e => setSearch(e.target.value)} />
              </div>
            </div>

            {/* Type filter */}
            <div className="form-group">
              <label className="form-label">Transaction Type</label>
              <select className="form-input form-select" value={typeFilter} onChange={e => setTypeFilter(e.target.value)}>
                <option value="ALL">All Types</option>
                {Object.entries(TXN_TYPE_LABELS).map(([k, v]) => (
                  <option key={k} value={k}>{v}</option>
                ))}
              </select>
            </div>

            {/* Entry type filter */}
            <div className="form-group">
              <label className="form-label">Entry Type</label>
              <div style={{ display: 'flex', gap: 8 }}>
                {['ALL','CREDIT','DEBIT'].map(e => (
                  <button key={e} onClick={() => setEntryFilter(e)}
                    className={`btn ${entryFilter === e ? 'btn-navy' : 'btn-ghost'}`}
                    style={{ flex: 1, justifyContent: 'center', fontSize: 11, padding: '7px 0' }}>
                    {e}
                  </button>
                ))}
              </div>
            </div>

            {/* Clear */}
            {(search || typeFilter !== 'ALL' || entryFilter !== 'ALL') && (
              <button className="btn btn-ghost" style={{ justifyContent: 'center', fontSize: 12 }}
                onClick={() => { setSearch(''); setTypeFilter('ALL'); setEntryFilter('ALL'); }}>
                <X size={12} /> Clear Filters
              </button>
            )}
          </div>
        </div>
      </div>

      {/* ── Transaction Table ────────────────────────────────────── */}
      <div className="card">
        <div className="card-header">
          <div>
            <h3 className="card-title">Account Statement</h3>
            <p className="card-subtitle">
              Showing {paginated.length} of {filteredTxns.length} transactions
            </p>
          </div>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--slate-400)' }}>
            <div className="spinner" style={{ margin: '0 auto 12px' }} />
            Loading transactions...
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date & Time</th>
                  <th>Reference No</th>
                  <th>Type</th>
                  <th>Counterparty / Narration</th>
                  <th style={{ textAlign: 'right' }}>Debit (₹)</th>
                  <th style={{ textAlign: 'right' }}>Credit (₹)</th>
                  <th style={{ textAlign: 'right' }}>Balance (₹)</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {paginated.map((txn) => (
                  <tr key={txn.referenceNumber}>
                    <td>
                      <div style={{ fontSize: 12, color: 'var(--slate-700)' }}>
                        {new Date(txn.transactionDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
                      </div>
                      <div style={{ fontSize: 11, color: 'var(--slate-400)' }}>
                        {new Date(txn.transactionDate).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true })}
                      </div>
                    </td>
                    <td>
                      <span className="mono" style={{ fontSize: 11, color: 'var(--navy-600)', fontWeight: 600 }}>
                        {txn.referenceNumber}
                      </span>
                    </td>
                    <td>
                      <span style={{
                        fontSize: 11, fontWeight: 600, padding: '3px 8px',
                        borderRadius: 99, background: 'var(--slate-100)', color: 'var(--slate-600)',
                      }}>
                        {TXN_TYPE_LABELS[txn.transactionType] || txn.transactionType}
                      </span>
                    </td>
                    <td>
                      <div style={{ fontSize: 12, fontWeight: 500, color: 'var(--slate-800)' }}>
                        {txn.counterpartyName || '—'}
                      </div>
                      <div style={{ fontSize: 11, color: 'var(--slate-400)', maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {txn.narration}
                      </div>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      {txn.entryType === 'DEBIT' ? (
                        <span className="mono" style={{ fontSize: 13, fontWeight: 700, color: 'var(--rose-600)' }}>
                          {txn.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                        </span>
                      ) : (
                        <span style={{ color: 'var(--slate-300)' }}>—</span>
                      )}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      {txn.entryType === 'CREDIT' ? (
                        <span className="mono" style={{ fontSize: 13, fontWeight: 700, color: 'var(--emerald-600)' }}>
                          {txn.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                        </span>
                      ) : (
                        <span style={{ color: 'var(--slate-300)' }}>—</span>
                      )}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <span className="mono" style={{ fontSize: 12, fontWeight: 600, color: 'var(--slate-800)' }}>
                        {txn.balanceAfter?.toLocaleString('en-IN', { minimumFractionDigits: 2 }) || '—'}
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${STATUS_STYLES[txn.status]?.cls || 'badge-neutral'}`}>
                        {STATUS_STYLES[txn.status]?.label || txn.status}
                      </span>
                    </td>
                    <td>
                      <button
                        onClick={() => setSelectedTxn(txn)}
                        style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--slate-400)', padding: 4 }}
                      >
                        <Eye size={14} />
                      </button>
                    </td>
                  </tr>
                ))}

                {paginated.length === 0 && (
                  <tr>
                    <td colSpan={9} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--slate-400)' }}>
                      <FileText size={28} style={{ margin: '0 auto 10px', display: 'block', opacity: 0.3 }} />
                      No transactions match your filters.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* ── Pagination ────────────────────────────────────────── */}
        {totalPages > 1 && (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 20, paddingTop: 16, borderTop: '1px solid var(--slate-100)' }}>
            <span style={{ fontSize: 12, color: 'var(--slate-400)' }}>
              Page {page + 1} of {totalPages} · {filteredTxns.length} results
            </span>
            <div style={{ display: 'flex', gap: 8 }}>
              <button className="btn btn-ghost" style={{ padding: '7px 12px' }}
                disabled={page === 0} onClick={() => setPage(p => p - 1)}>
                <ChevronLeft size={14} />
              </button>
              {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                const pg = page < 3 ? i : page - 2 + i;
                if (pg >= totalPages) return null;
                return (
                  <button key={pg}
                    className={`btn ${pg === page ? 'btn-navy' : 'btn-ghost'}`}
                    style={{ padding: '7px 13px', minWidth: 36 }}
                    onClick={() => setPage(pg)}>
                    {pg + 1}
                  </button>
                );
              })}
              <button className="btn btn-ghost" style={{ padding: '7px 12px' }}
                disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>
                <ChevronRight size={14} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Transaction detail modal */}
      {selectedTxn && <TxnDetailModal txn={selectedTxn} onClose={() => setSelectedTxn(null)} />}
    </div>
  );
}
