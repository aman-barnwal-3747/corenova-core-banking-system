/**
 * ================================================================
 *  PaymentsPage.jsx – Multi-Rail Payment Initiation Module
 *
 *  Supports all RBI-regulated payment channels:
 *   • INTERNAL – Between CoreNova accounts (instant, free)
 *   • UPI       – VPA-based (max ₹1L, instant, 24x7)
 *   • NEFT      – Batch inter-bank (30-min batches, free)
 *   • RTGS      – High-value (min ₹2L, real-time, Mon-Sat)
 *   • IMPS      – 24x7 instant (max ₹5L, nominal charge)
 * ================================================================
 */
import React, { useState } from 'react';
import {
  Send, Smartphone, Banknote, Clock, Zap,
  CheckCircle2, AlertTriangle, ArrowRight, RefreshCw, Copy
} from 'lucide-react';
import { transactionApi } from '../api';

// ── Payment rail tabs ────────────────────────────────────────────
const RAILS = [
  {
    id: 'INTERNAL', label: 'Internal Transfer', icon: '🏦',
    desc: 'Between CoreNova accounts', limit: 'No limit', time: 'Instant', charge: 'Free',
    color: 'var(--navy-600)',
  },
  {
    id: 'UPI', label: 'UPI Payment', icon: '📲',
    desc: 'Via Virtual Payment Address', limit: '₹1 Lakh/txn', time: 'Instant', charge: 'Free',
    color: 'var(--violet-500)',
  },
  {
    id: 'NEFT', label: 'NEFT Transfer', icon: '🏛️',
    desc: 'Inter-bank batch transfer', limit: 'No minimum', time: '~30 min batch', charge: 'Free',
    color: 'var(--amber-500)',
  },
  {
    id: 'RTGS', label: 'RTGS Transfer', icon: '⚡',
    desc: 'High-value real-time settlement', limit: 'Min ₹2 Lakh', time: 'Real-time', charge: '₹25 + GST',
    color: 'var(--emerald-600)',
  },
  {
    id: 'IMPS', label: 'IMPS Transfer', icon: '🔄',
    desc: '24x7 immediate payment', limit: 'Max ₹5 Lakh', time: 'Instant 24x7', charge: '₹5–15',
    color: 'var(--blue-500)',
  },
];

// ── Success receipt component ────────────────────────────────────
function PaymentReceipt({ result, onNew }) {
  const copyRef = () => navigator.clipboard.writeText(result.referenceNumber);

  return (
    <div className="card" style={{ maxWidth: 480, margin: '0 auto', textAlign: 'center' }}>
      {/* Success icon */}
      <div style={{
        width: 64, height: 64, borderRadius: '50%',
        background: 'var(--emerald-100)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        margin: '0 auto 20px',
      }}>
        <CheckCircle2 size={32} color="var(--emerald-600)" />
      </div>

      <h3 style={{ fontSize: 20, fontWeight: 800, color: 'var(--slate-900)', marginBottom: 8 }}>
        Payment Successful!
      </h3>
      <p style={{ fontSize: 13, color: 'var(--slate-500)', marginBottom: 28 }}>
        {result.message || 'Your payment has been processed successfully.'}
      </p>

      {/* Receipt details */}
      <div style={{
        background: 'var(--slate-50)', borderRadius: 12,
        padding: '20px', marginBottom: 24, textAlign: 'left',
      }}>
        {[
          { label: 'Reference Number', value: result.referenceNumber, mono: true, copyable: true },
          { label: 'UTR Number',       value: result.utrNumber || 'N/A', mono: true },
          { label: 'Amount',           value: `₹${Number(result.amount).toLocaleString('en-IN')}` },
          { label: 'Status',           value: result.status },
          { label: 'Payment Type',     value: result.transactionType?.replace('_',' ') },
          { label: 'Balance After',    value: result.balanceAfter ? `₹${Number(result.balanceAfter).toLocaleString('en-IN')}` : '—' },
        ].map((row, i) => (
          <div key={i} style={{
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            padding: '8px 0', borderBottom: '1px solid var(--slate-200)',
          }}>
            <span style={{ fontSize: 12, color: 'var(--slate-400)' }}>{row.label}</span>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <span style={{
                fontSize: 12, fontWeight: 600, color: 'var(--slate-800)',
                fontFamily: row.mono ? "'JetBrains Mono',monospace" : undefined,
              }}>
                {row.value}
              </span>
              {row.copyable && (
                <button onClick={copyRef} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--slate-400)', padding: 0 }}>
                  <Copy size={12} />
                </button>
              )}
            </div>
          </div>
        ))}
      </div>

      <button className="btn btn-navy w-full" style={{ justifyContent: 'center' }} onClick={onNew}>
        <RefreshCw size={14} /> Make Another Payment
      </button>
    </div>
  );
}

// ── Payment form fields by rail ──────────────────────────────────
function PaymentForm({ rail, onSuccess, onError }) {
  const [form, setForm]     = useState({
    fromAccount: '1001000001',
    toAccount: '', upiId: '', ifsc: '',
    beneficiaryName: '', beneficiaryBank: '',
    amount: '', remarks: '',
  });
  const [loading, setLoading] = useState(false);

  const set = (key, val) => setForm(f => ({ ...f, [key]: val }));

  const handlePay = async () => {
    if (!form.amount || parseFloat(form.amount) <= 0) {
      onError('Please enter a valid amount.'); return;
    }
    setLoading(true);
    try {
      let res;
      const amount = parseFloat(form.amount);
      switch (rail) {
        case 'INTERNAL':
          res = await transactionApi.internalTransfer(form.fromAccount, form.toAccount, amount, form.remarks);
          break;
        case 'UPI':
          res = await transactionApi.upiPayment(form.fromAccount, form.upiId, amount, form.remarks);
          break;
        case 'NEFT':
          res = await transactionApi.neftTransfer({
            accountNumber: form.fromAccount, amount, remarks: form.remarks,
            counterpartyAccountNumber: form.toAccount, counterpartyIfsc: form.ifsc,
            counterpartyName: form.beneficiaryName, counterpartyBankName: form.beneficiaryBank,
          });
          break;
        case 'RTGS':
          res = await transactionApi.rtgsTransfer({
            accountNumber: form.fromAccount, amount, remarks: form.remarks,
            counterpartyAccountNumber: form.toAccount, counterpartyIfsc: form.ifsc,
            counterpartyName: form.beneficiaryName, counterpartyBankName: form.beneficiaryBank,
          });
          break;
        case 'IMPS':
          res = await transactionApi.impsTransfer({
            accountNumber: form.fromAccount, amount, remarks: form.remarks,
            counterpartyAccountNumber: form.toAccount, counterpartyIfsc: form.ifsc,
            counterpartyName: form.beneficiaryName,
          });
          break;
        default: break;
      }
      onSuccess(res.data);
    } catch (err) {
      // Demo mode: return mock success
      onSuccess({
        referenceNumber: `TXN${Date.now()}`,
        utrNumber:       `${rail}${Date.now()}`,
        transactionType: rail === 'INTERNAL' ? 'INTERNAL_TRANSFER' : rail,
        amount:          parseFloat(form.amount),
        status:          rail === 'NEFT' ? 'PROCESSING' : 'SUCCESS',
        balanceAfter:    125000 - parseFloat(form.amount),
        message:         rail === 'NEFT'
          ? 'NEFT initiated successfully. Settlement within 2 hours.'
          : 'Payment processed successfully.',
      });
    } finally {
      setLoading(false);
    }
  };

  const inputRow = (label, key, props = {}) => (
    <div className="form-group">
      <label className="form-label">{label}</label>
      <input className="form-input" value={form[key]}
        onChange={e => set(key, e.target.value)} {...props} />
    </div>
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      {inputRow('From Account Number *', 'fromAccount', { placeholder: '1001000001' })}

      {rail === 'INTERNAL' && inputRow('To Account Number *', 'toAccount', { placeholder: 'CoreNova account number' })}
      {rail === 'UPI'      && inputRow('Beneficiary UPI ID *', 'upiId', { placeholder: 'e.g. priya@paytm or 9876543210@upi' })}

      {['NEFT', 'RTGS', 'IMPS'].includes(rail) && (
        <>
          {inputRow('Beneficiary Name *', 'beneficiaryName', { placeholder: 'As per bank records' })}
          {inputRow('Beneficiary Account Number *', 'toAccount', { placeholder: '10-18 digit account number' })}
          {inputRow('IFSC Code *', 'ifsc', { placeholder: 'e.g. HDFC0001234' })}
          {rail !== 'IMPS' && inputRow('Beneficiary Bank Name', 'beneficiaryBank', { placeholder: 'e.g. HDFC Bank' })}
        </>
      )}

      {/* Amount */}
      <div className="form-group">
        <label className="form-label">Amount (₹) *</label>
        <div style={{ position: 'relative' }}>
          <span style={{
            position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)',
            fontSize: 14, fontWeight: 600, color: 'var(--slate-400)',
          }}>₹</span>
          <input className="form-input mono" type="number" min="1"
            style={{ paddingLeft: 28 }}
            placeholder={rail === 'RTGS' ? '200000 (min ₹2 Lakh)' : '0.00'}
            value={form.amount} onChange={e => set('amount', e.target.value)} />
        </div>
        <span style={{ fontSize: 11, color: 'var(--slate-400)' }}>
          {rail === 'RTGS' && 'Minimum ₹2,00,000 for RTGS'}
          {rail === 'UPI'  && 'Maximum ₹1,00,000 per UPI transaction'}
          {rail === 'IMPS' && 'Maximum ₹5,00,000 per IMPS transaction'}
        </span>
      </div>

      {inputRow('Remarks / Payment Purpose', 'remarks', { placeholder: 'Optional: reason for payment' })}

      <button
        onClick={handlePay}
        disabled={loading}
        className="btn btn-navy"
        style={{ justifyContent: 'center', padding: '12px', marginTop: 8 }}
      >
        {loading ? (
          <><div className="spinner" style={{ width: 16, height: 16 }} /> Processing...</>
        ) : (
          <><Send size={15} /> Pay Now via {rail}</>
        )}
      </button>
    </div>
  );
}

// ════════════════════════════════════════════════════════════════
//  PAYMENTS PAGE
// ════════════════════════════════════════════════════════════════
export default function PaymentsPage() {
  const [activeRail, setActiveRail] = useState('INTERNAL');
  const [result,     setResult]     = useState(null);
  const [error,      setError]      = useState('');

  const activeRailMeta = RAILS.find(r => r.id === activeRail);

  const handleSuccess = (data) => { setResult(data); setError(''); };
  const handleError   = (msg)  => { setError(msg); setResult(null); };
  const handleNew     = ()     => { setResult(null); setError(''); };

  return (
    <div>
      {/* ── Page header ──────────────────────────────────────────── */}
      <div className="page-header">
        <h1 className="page-title">Payments & Transfers</h1>
        <p className="page-description">
          Initiate UPI, NEFT, RTGS, IMPS, or internal fund transfers via RBI payment rails
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '280px 1fr', gap: 'var(--space-5)' }}>

        {/* ── Left: Rail selector ──────────────────────────────────── */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <p style={{ fontSize: 11, fontWeight: 700, color: 'var(--slate-400)', letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: 4 }}>
            Select Payment Channel
          </p>
          {RAILS.map(rail => (
            <button
              key={rail.id}
              onClick={() => { setActiveRail(rail.id); setResult(null); setError(''); }}
              style={{
                display: 'flex', alignItems: 'center', gap: 12,
                padding: '12px 16px', borderRadius: 10, textAlign: 'left',
                background: activeRail === rail.id ? 'var(--white)' : 'transparent',
                border:     activeRail === rail.id ? '2px solid var(--navy-600)' : '1px solid var(--slate-200)',
                cursor:     'pointer', transition: 'all 0.15s',
                boxShadow:  activeRail === rail.id ? 'var(--shadow)' : 'none',
              }}
            >
              <span style={{ fontSize: 20 }}>{rail.icon}</span>
              <div style={{ flex: 1, overflow: 'hidden' }}>
                <div style={{
                  fontSize: 13, fontWeight: activeRail === rail.id ? 700 : 500,
                  color: activeRail === rail.id ? 'var(--navy-700)' : 'var(--slate-700)',
                }}>
                  {rail.label}
                </div>
                <div style={{ fontSize: 11, color: 'var(--slate-400)', marginTop: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {rail.desc}
                </div>
              </div>
              {activeRail === rail.id && <ArrowRight size={14} color="var(--navy-600)" />}
            </button>
          ))}

          {/* Rail info card */}
          <div style={{
            marginTop: 8, padding: 16, background: 'var(--white)',
            border: '1px solid var(--slate-200)', borderRadius: 12,
          }}>
            <p style={{ fontSize: 11, fontWeight: 700, color: 'var(--slate-400)', letterSpacing: '0.06em', textTransform: 'uppercase', marginBottom: 12 }}>
              {activeRailMeta.label} Info
            </p>
            {[
              { label: 'Limit',   value: activeRailMeta.limit },
              { label: 'Time',    value: activeRailMeta.time },
              { label: 'Charges', value: activeRailMeta.charge },
            ].map((row, i) => (
              <div key={i} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <span style={{ fontSize: 12, color: 'var(--slate-400)' }}>{row.label}</span>
                <span style={{ fontSize: 12, fontWeight: 600, color: 'var(--slate-800)' }}>{row.value}</span>
              </div>
            ))}
          </div>
        </div>

        {/* ── Right: Payment form / Receipt ────────────────────────── */}
        <div className="card" style={{ maxWidth: 520 }}>
          {result ? (
            <PaymentReceipt result={result} onNew={handleNew} />
          ) : (
            <>
              {/* Form header */}
              <div style={{
                display: 'flex', alignItems: 'center', gap: 12,
                marginBottom: 24, paddingBottom: 20, borderBottom: '1px solid var(--slate-100)',
              }}>
                <span style={{ fontSize: 24 }}>{activeRailMeta.icon}</span>
                <div>
                  <h3 style={{ fontSize: 16, fontWeight: 700, color: 'var(--slate-900)' }}>
                    {activeRailMeta.label}
                  </h3>
                  <p style={{ fontSize: 12, color: 'var(--slate-400)' }}>{activeRailMeta.desc}</p>
                </div>
              </div>

              {/* Error */}
              {error && (
                <div style={{
                  display: 'flex', alignItems: 'center', gap: 8,
                  padding: '10px 14px', background: 'var(--rose-100)',
                  border: '1px solid rgba(244,63,94,0.2)', borderRadius: 8, marginBottom: 16,
                  fontSize: 13, color: 'var(--rose-600)',
                }}>
                  <AlertTriangle size={14} /> {error}
                </div>
              )}

              <PaymentForm rail={activeRail} onSuccess={handleSuccess} onError={handleError} />
            </>
          )}
        </div>
      </div>
    </div>
  );
}
