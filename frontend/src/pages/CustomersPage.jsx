/**
 * ================================================================
 *  CustomersPage.jsx – Customer CIF Management Module
 *
 *  Features:
 *   • Customer search (name, phone, email, CIF)
 *   • Full customer profile panel (personal, KYC, accounts)
 *   • KYC status pipeline with visual progress
 *   • Create new customer modal (onboarding form)
 *   • KYC status update workflow (Submit → Review → Approve)
 *   • Customer segment badges (RETAIL / HNI / CORPORATE)
 *   • Linked accounts summary per customer
 * ================================================================
 */
import React, { useState } from 'react';
import {
  Users, Search, Plus, ShieldCheck, ShieldAlert, ShieldX,
  Phone, Mail, MapPin, Calendar, CreditCard, X, CheckCircle2,
  Clock, AlertCircle, User, FileText, Eye, ChevronRight
} from 'lucide-react';
import { customerApi } from '../api';

// ── KYC status pipeline config ───────────────────────────────────
const KYC_STAGES = [
  { key: 'NOT_SUBMITTED', label: 'Not Submitted', icon: '○', color: 'var(--slate-300)' },
  { key: 'SUBMITTED',     label: 'Docs Submitted', icon: '→', color: 'var(--amber-500)' },
  { key: 'UNDER_REVIEW',  label: 'Under Review',   icon: '⧖', color: 'var(--blue-500)' },
  { key: 'APPROVED',      label: 'KYC Approved',   icon: '✓', color: 'var(--emerald-600)' },
];

const KYC_META = {
  NOT_SUBMITTED: { cls: 'badge-neutral', label: 'Not Submitted',  icon: AlertCircle },
  SUBMITTED:     { cls: 'badge-warning', label: 'Docs Submitted', icon: Clock },
  UNDER_REVIEW:  { cls: 'badge-info',    label: 'Under Review',   icon: Eye },
  APPROVED:      { cls: 'badge-success', label: 'KYC Approved',   icon: CheckCircle2 },
  REJECTED:      { cls: 'badge-danger',  label: 'Rejected',       icon: X },
  RE_KYC_DUE:    { cls: 'badge-warning', label: 'Re-KYC Due',     icon: AlertCircle },
  EXPIRED:       { cls: 'badge-danger',  label: 'KYC Expired',    icon: ShieldX },
};

const SEGMENT_COLORS = {
  RETAIL:     { bg: '#DBEAFE', color: '#1D4ED8' },
  HNI:        { bg: '#EDE9FE', color: '#7C3AED' },
  CORPORATE:  { bg: '#D1FAE5', color: '#059669' },
  NRI:        { bg: '#FEF3C7', color: '#92400E' },
  STUDENT:    { bg: '#FFE4E6', color: '#BE123C' },
  SENIOR_CITIZEN: { bg: '#F0FDF4', color: '#166534' },
};

// ── Demo customers ───────────────────────────────────────────────
const DEMO_CUSTOMERS = [
  {
    customerId: 1, cifNumber: 'CNB-CUST-20250001',
    firstName: 'Aman', lastName: 'Verma',
    dateOfBirth: '1988-04-15', gender: 'Male',
    email: 'aman.verma@email.com', phone: '9876543210',
    aadhaarNumber: '1234-5678-9012', panNumber: 'ABCPV1234D',
    addressLine1: '12, Green Park Colony', city: 'New Delhi', state: 'Delhi', pincode: '110016',
    kycStatus: 'APPROVED', customerSegment: 'RETAIL',
    occupation: 'Software Engineer', annualIncome: 1200000,
    homeBranchCode: 'MAIN001', relationshipManager: 'Sunita Kapoor',
    isActive: true, kycVerifiedBy: 'manager',
    kycVerifiedAt: '2025-01-10T10:30:00',
    accounts: [
      { accountNumber: '1001000001', accountType: 'SAVINGS', currentBalance: 125000, accountStatus: 'ACTIVE' }
    ],
  },
  {
    customerId: 2, cifNumber: 'CNB-CUST-20250002',
    firstName: 'Priya', lastName: 'Sharma',
    dateOfBirth: '1992-08-20', gender: 'Female',
    email: 'priya.sharma@email.com', phone: '9876543211',
    aadhaarNumber: '2345-6789-0123', panNumber: 'BCQPS5678E',
    addressLine1: '45, Bandra West', city: 'Mumbai', state: 'Maharashtra', pincode: '400050',
    kycStatus: 'APPROVED', customerSegment: 'HNI',
    occupation: 'Business Owner', annualIncome: 5000000,
    homeBranchCode: 'MAIN001', relationshipManager: 'Sunita Kapoor',
    isActive: true,
    accounts: [
      { accountNumber: '1001000002', accountType: 'CURRENT', currentBalance: 350000, accountStatus: 'ACTIVE' }
    ],
  },
  {
    customerId: 3, cifNumber: 'CNB-CUST-20250003',
    firstName: 'Ravi', lastName: 'Kumar',
    dateOfBirth: '1975-12-05', gender: 'Male',
    email: 'ravi.kumar@email.com', phone: '9876543212',
    aadhaarNumber: '3456-7890-1234', panNumber: 'CDRRK9012F',
    addressLine1: '78, Koramangala 5th Block', city: 'Bengaluru', state: 'Karnataka', pincode: '560095',
    kycStatus: 'APPROVED', customerSegment: 'RETAIL',
    occupation: 'Government Employee', annualIncome: 800000,
    homeBranchCode: 'MAIN001',
    isActive: true,
    accounts: [
      { accountNumber: '1001000003', accountType: 'SALARY', currentBalance: 75000, accountStatus: 'ACTIVE' }
    ],
  },
  {
    customerId: 4, cifNumber: 'CNB-CUST-20250004',
    firstName: 'Meera', lastName: 'Nair',
    dateOfBirth: '1995-03-12', gender: 'Female',
    email: 'meera.nair@email.com', phone: '9876543213',
    aadhaarNumber: '4567-8901-2345', panNumber: 'EFGMN3456G',
    addressLine1: '9, Marine Drive', city: 'Mumbai', state: 'Maharashtra', pincode: '400020',
    kycStatus: 'UNDER_REVIEW', customerSegment: 'RETAIL',
    occupation: 'Doctor', annualIncome: 2500000,
    homeBranchCode: 'MAIN001',
    isActive: true,
    accounts: [],
  },
  {
    customerId: 5, cifNumber: 'CNB-CUST-20250005',
    firstName: 'Amit', lastName: 'Shah',
    dateOfBirth: '1969-07-25', gender: 'Male',
    email: 'amit.shah@corp.com', phone: '9876543214',
    aadhaarNumber: '5678-9012-3456', panNumber: 'GHIAS7890H',
    addressLine1: '1, Business Tower, BKC', city: 'Mumbai', state: 'Maharashtra', pincode: '400051',
    kycStatus: 'APPROVED', customerSegment: 'CORPORATE',
    occupation: 'CEO', annualIncome: 25000000,
    homeBranchCode: 'MAIN001',
    isActive: true,
    accounts: [],
  },
  {
    customerId: 6, cifNumber: 'CNB-CUST-20250006',
    firstName: 'Leena', lastName: 'Patel',
    dateOfBirth: '2001-11-14', gender: 'Female',
    email: 'leena.patel@student.edu', phone: '9876543215',
    aadhaarNumber: '6789-0123-4567', panNumber: 'IJKLP4567I',
    addressLine1: '22, Hostel Block A, IIT', city: 'Chennai', state: 'Tamil Nadu', pincode: '600036',
    kycStatus: 'SUBMITTED', customerSegment: 'STUDENT',
    occupation: 'Student', annualIncome: 0,
    homeBranchCode: 'MAIN001',
    isActive: true,
    accounts: [],
  },
];

// ── Create Customer Modal ────────────────────────────────────────
function CreateCustomerModal({ onClose, onSuccess }) {
  const [form, setForm]   = useState({
    firstName: '', lastName: '', dateOfBirth: '', gender: 'Male',
    phone: '', email: '', panNumber: '', aadhaarNumber: '',
    addressLine1: '', city: '', state: '', pincode: '',
    occupation: '', customerSegment: 'RETAIL',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const handleCreate = async () => {
    if (!form.firstName || !form.lastName || !form.phone || !form.dateOfBirth) {
      setError('First name, last name, phone, and date of birth are required.'); return;
    }
    setLoading(true);
    setError('');
    try {
      await customerApi.create(form);
      onSuccess(`Customer ${form.firstName} ${form.lastName} created successfully!`);
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Demo mode: Customer creation requires backend connection.');
      setTimeout(onClose, 1500);
    } finally {
      setLoading(false);
    }
  };

  const field = (label, key, props = {}) => (
    <div className="form-group">
      <label className="form-label">{label}</label>
      <input className="form-input" value={form[key]} onChange={e => set(key, e.target.value)} {...props} />
    </div>
  );

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 1000, background: 'rgba(10,22,40,0.55)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 20,
    }}>
      <div style={{
        background: 'var(--white)', borderRadius: 20, width: '100%', maxWidth: 620,
        maxHeight: '90vh', overflow: 'auto', boxShadow: 'var(--shadow-lg)',
      }}>
        {/* Header */}
        <div style={{
          background: 'linear-gradient(135deg, var(--navy-900), var(--navy-700))',
          padding: '24px 28px', borderRadius: '20px 20px 0 0',
          display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        }}>
          <div>
            <h3 style={{ color: '#fff', fontSize: 16, fontWeight: 700 }}>New Customer Onboarding</h3>
            <p style={{ color: 'rgba(255,255,255,0.5)', fontSize: 12, marginTop: 2 }}>
              Create a new Customer Information File (CIF)
            </p>
          </div>
          <button onClick={onClose} style={{ background: 'rgba(255,255,255,0.1)', border: 'none', borderRadius: '50%', width: 32, height: 32, cursor: 'pointer', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <X size={16} />
          </button>
        </div>

        <div style={{ padding: 28 }}>
          {error && <div style={{ background: 'var(--rose-100)', color: 'var(--rose-600)', padding: '10px 14px', borderRadius: 8, marginBottom: 16, fontSize: 13 }}>{error}</div>}

          {/* Personal Info */}
          <p style={{ fontSize: 11, fontWeight: 700, color: 'var(--slate-400)', letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: 12 }}>Personal Information</p>
          <div className="grid-2" style={{ marginBottom: 16 }}>
            {field('First Name *', 'firstName', { placeholder: 'e.g. Aman' })}
            {field('Last Name *',  'lastName',  { placeholder: 'e.g. Verma' })}
            {field('Date of Birth *', 'dateOfBirth', { type: 'date' })}
            <div className="form-group">
              <label className="form-label">Gender</label>
              <select className="form-input form-select" value={form.gender} onChange={e => set('gender', e.target.value)}>
                <option>Male</option><option>Female</option><option>Other</option>
              </select>
            </div>
            {field('Occupation', 'occupation', { placeholder: 'e.g. Software Engineer' })}
            <div className="form-group">
              <label className="form-label">Customer Segment</label>
              <select className="form-input form-select" value={form.customerSegment} onChange={e => set('customerSegment', e.target.value)}>
                {Object.keys(SEGMENT_COLORS).map(s => <option key={s}>{s}</option>)}
              </select>
            </div>
          </div>

          {/* Contact */}
          <p style={{ fontSize: 11, fontWeight: 700, color: 'var(--slate-400)', letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: 12 }}>Contact Details</p>
          <div className="grid-2" style={{ marginBottom: 16 }}>
            {field('Mobile Number *', 'phone', { placeholder: '10-digit mobile' })}
            {field('Email Address',   'email', { placeholder: 'name@example.com', type: 'email' })}
            {field('Address Line 1',  'addressLine1', { placeholder: 'House/Flat no., Street' })}
            {field('City',            'city',         { placeholder: 'e.g. Mumbai' })}
            {field('State',           'state',        { placeholder: 'e.g. Maharashtra' })}
            {field('PIN Code',        'pincode',      { placeholder: '6-digit PIN' })}
          </div>

          {/* KYC Documents */}
          <p style={{ fontSize: 11, fontWeight: 700, color: 'var(--slate-400)', letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: 12 }}>KYC Documents</p>
          <div className="grid-2">
            {field('Aadhaar Number', 'aadhaarNumber', { placeholder: '12-digit Aadhaar' })}
            {field('PAN Number',     'panNumber',     { placeholder: 'e.g. ABCDE1234F' })}
          </div>

          <div style={{ display: 'flex', gap: 10, marginTop: 24, justifyContent: 'flex-end' }}>
            <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
            <button className="btn btn-navy" onClick={handleCreate} disabled={loading}>
              {loading ? 'Creating...' : '+ Create Customer CIF'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── KYC Progress Bar ─────────────────────────────────────────────
function KycProgressBar({ status }) {
  const stageIndex = KYC_STAGES.findIndex(s => s.key === status);
  const effectiveIndex = stageIndex === -1 ? 0 : stageIndex;

  return (
    <div style={{ position: 'relative', marginBottom: 8 }}>
      {/* Track */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', position: 'relative' }}>
        <div style={{
          position: 'absolute', top: '50%', left: 0, right: 0, height: 2,
          background: 'var(--slate-200)', transform: 'translateY(-50%)', zIndex: 0,
        }} />
        <div style={{
          position: 'absolute', top: '50%', left: 0, height: 2,
          background: status === 'REJECTED' ? 'var(--rose-500)' : 'var(--emerald-500)',
          transform: 'translateY(-50%)', zIndex: 1,
          width: `${(effectiveIndex / (KYC_STAGES.length - 1)) * 100}%`,
          transition: 'width 0.5s ease',
        }} />
        {KYC_STAGES.map((stage, i) => {
          const done   = i <= effectiveIndex;
          const active = i === effectiveIndex;
          return (
            <div key={stage.key} style={{ position: 'relative', zIndex: 2 }}>
              <div style={{
                width: active ? 28 : 20, height: active ? 28 : 20,
                borderRadius: '50%',
                background:  done ? (status === 'REJECTED' && i === effectiveIndex ? 'var(--rose-500)' : 'var(--emerald-500)') : 'var(--white)',
                border:      done ? 'none' : '2px solid var(--slate-300)',
                display:     'flex', alignItems: 'center', justifyContent: 'center',
                fontSize:    10, fontWeight: 700, color: done ? '#fff' : 'var(--slate-400)',
                boxShadow:   active ? '0 0 0 4px rgba(16,185,129,0.15)' : 'none',
                transition:  'all 0.3s ease',
              }}>
                {done ? '✓' : i + 1}
              </div>
            </div>
          );
        })}
      </div>
      {/* Labels */}
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 8 }}>
        {KYC_STAGES.map((stage, i) => (
          <span key={stage.key} style={{
            fontSize: 9, fontWeight: 600, textAlign: 'center',
            color: i <= effectiveIndex ? 'var(--emerald-600)' : 'var(--slate-400)',
            maxWidth: 70, letterSpacing: '0.02em',
          }}>
            {stage.label}
          </span>
        ))}
      </div>
    </div>
  );
}

// ════════════════════════════════════════════════════════════════
//  CUSTOMERS PAGE
// ════════════════════════════════════════════════════════════════
export default function CustomersPage() {
  const [customers,   setCustomers]   = useState(DEMO_CUSTOMERS);
  const [selected,    setSelected]    = useState(null);
  const [search,      setSearch]      = useState('');
  const [kycFilter,   setKycFilter]   = useState('ALL');
  const [showModal,   setShowModal]   = useState(false);
  const [toast,       setToast]       = useState('');
  const [kycUpdating, setKycUpdating] = useState(false);

  const showToast = (msg) => { setToast(msg); setTimeout(() => setToast(''), 4000); };

  const filtered = customers.filter(c => {
    const matchSearch = !search.trim() ||
      `${c.firstName} ${c.lastName}`.toLowerCase().includes(search.toLowerCase()) ||
      c.phone.includes(search) ||
      c.email.toLowerCase().includes(search.toLowerCase()) ||
      c.cifNumber.includes(search);
    const matchKyc = kycFilter === 'ALL' || c.kycStatus === kycFilter;
    return matchSearch && matchKyc;
  });

  // KYC status advance
  const handleAdvanceKyc = async () => {
    if (!selected) return;
    const transitions = { NOT_SUBMITTED: 'SUBMITTED', SUBMITTED: 'UNDER_REVIEW', UNDER_REVIEW: 'APPROVED' };
    const nextStatus = transitions[selected.kycStatus];
    if (!nextStatus) return;
    setKycUpdating(true);
    try {
      await customerApi.updateKycStatus(selected.cifNumber, nextStatus, 'KYC progressed by officer');
    } catch { /* demo mode */ }
    const updated = { ...selected, kycStatus: nextStatus };
    setCustomers(cs => cs.map(c => c.cifNumber === selected.cifNumber ? updated : c));
    setSelected(updated);
    showToast(`KYC status updated to ${nextStatus} for ${selected.firstName} ${selected.lastName}`);
    setKycUpdating(false);
  };

  const kycCounts = {
    APPROVED:     customers.filter(c => c.kycStatus === 'APPROVED').length,
    UNDER_REVIEW: customers.filter(c => c.kycStatus === 'UNDER_REVIEW').length,
    SUBMITTED:    customers.filter(c => c.kycStatus === 'SUBMITTED').length,
    NOT_SUBMITTED:customers.filter(c => c.kycStatus === 'NOT_SUBMITTED').length,
  };

  return (
    <div>
      {/* Toast */}
      {toast && (
        <div style={{ position: 'fixed', top: 80, right: 24, zIndex: 2000, background: 'var(--navy-900)', color: '#fff', padding: '12px 20px', borderRadius: 10, fontSize: 13, fontWeight: 500, boxShadow: 'var(--shadow-lg)' }}>
          ✓ {toast}
        </div>
      )}

      {/* ── Page header ──────────────────────────────────────────── */}
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">Customer Management</h1>
          <p className="page-description">
            {customers.length} CIFs registered · {kycCounts.APPROVED} KYC approved · {kycCounts.UNDER_REVIEW + kycCounts.SUBMITTED} pending review
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>
          <Plus size={15} /> New Customer CIF
        </button>
      </div>

      {/* ── KYC Status KPIs ──────────────────────────────────────── */}
      <div className="grid-4" style={{ marginBottom: 24 }}>
        {[
          { label: 'KYC Approved',     value: kycCounts.APPROVED,     color: 'var(--emerald-500)', key: 'APPROVED' },
          { label: 'Under Review',      value: kycCounts.UNDER_REVIEW, color: 'var(--blue-500)',    key: 'UNDER_REVIEW' },
          { label: 'Docs Submitted',    value: kycCounts.SUBMITTED,    color: 'var(--amber-500)',   key: 'SUBMITTED' },
          { label: 'KYC Not Submitted', value: kycCounts.NOT_SUBMITTED,color: 'var(--slate-400)',   key: 'NOT_SUBMITTED' },
        ].map(kpi => (
          <button key={kpi.key}
            onClick={() => setKycFilter(kycFilter === kpi.key ? 'ALL' : kpi.key)}
            className="card"
            style={{
              padding: '16px 20px', textAlign: 'left', cursor: 'pointer', border: 'none',
              outline: kycFilter === kpi.key ? `2px solid ${kpi.color}` : '1px solid rgba(226,232,240,0.7)',
            }}>
            <div style={{ fontSize: 26, fontWeight: 800, color: 'var(--slate-900)', fontFamily: "'JetBrains Mono',monospace" }}>
              {kpi.value}
            </div>
            <div style={{ fontSize: 12, color: 'var(--slate-500)', marginTop: 4, display: 'flex', alignItems: 'center', gap: 6 }}>
              <span style={{ width: 8, height: 8, borderRadius: '50%', background: kpi.color, display: 'inline-block' }} />
              {kpi.label}
            </div>
          </button>
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '340px 1fr', gap: 20 }}>

        {/* ── Left: Customer list ──────────────────────────────────── */}
        <div>
          {/* Search */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, background: 'var(--white)', border: '1px solid var(--slate-200)', borderRadius: 10, padding: '10px 14px', marginBottom: 12 }}>
            <Search size={14} color="var(--slate-400)" />
            <input placeholder="Search name, phone, CIF..." value={search} onChange={e => setSearch(e.target.value)}
              style={{ border: 'none', outline: 'none', background: 'transparent', fontSize: 13, flex: 1, color: 'var(--slate-800)' }} />
            {search && <button onClick={() => setSearch('')} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--slate-400)', padding: 0 }}><X size={13} /></button>}
          </div>

          {/* Customer list */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {filtered.map(c => {
              const kyc  = KYC_META[c.kycStatus]  || KYC_META.NOT_SUBMITTED;
              const seg  = SEGMENT_COLORS[c.customerSegment] || SEGMENT_COLORS.RETAIL;
              const isSelected = selected?.cifNumber === c.cifNumber;

              return (
                <button key={c.cifNumber} onClick={() => setSelected(c)}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 12, padding: '12px 14px',
                    borderRadius: 10, border: 'none', cursor: 'pointer', textAlign: 'left',
                    background: isSelected ? 'rgba(41,82,163,0.06)' : 'var(--white)',
                    outline: isSelected ? '2px solid var(--navy-600)' : '1px solid rgba(226,232,240,0.7)',
                    boxShadow: isSelected ? 'var(--shadow)' : 'var(--shadow-sm)',
                  }}>
                  {/* Avatar */}
                  <div style={{
                    width: 40, height: 40, borderRadius: 10, flexShrink: 0,
                    background: `linear-gradient(135deg, ${seg.color}20, ${seg.color}40)`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 15, fontWeight: 700, color: seg.color,
                  }}>
                    {c.firstName.charAt(0)}{c.lastName.charAt(0)}
                  </div>

                  <div style={{ flex: 1, overflow: 'hidden' }}>
                    <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--slate-800)' }}>
                      {c.firstName} {c.lastName}
                    </div>
                    <div style={{ fontSize: 11, color: 'var(--slate-400)', fontFamily: "'JetBrains Mono',monospace" }}>
                      {c.cifNumber}
                    </div>
                  </div>

                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
                    <span style={{ fontSize: 10, fontWeight: 700, padding: '2px 8px', borderRadius: 99, background: seg.bg, color: seg.color }}>
                      {c.customerSegment}
                    </span>
                    <span className={`badge ${kyc.cls}`} style={{ fontSize: 9, padding: '2px 6px' }}>
                      {kyc.label}
                    </span>
                  </div>
                </button>
              );
            })}

            {filtered.length === 0 && (
              <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--slate-400)' }}>
                <Users size={28} style={{ margin: '0 auto 10px', display: 'block', opacity: 0.3 }} />
                <p style={{ fontSize: 13 }}>No customers match your search.</p>
              </div>
            )}
          </div>
        </div>

        {/* ── Right: Customer detail panel ─────────────────────────── */}
        <div>
          {selected ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

              {/* Profile header card */}
              <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                <div style={{
                  background: 'linear-gradient(135deg, var(--navy-900), var(--navy-700))',
                  padding: '28px', display: 'flex', alignItems: 'center', gap: 20,
                }}>
                  <div style={{
                    width: 60, height: 60, borderRadius: 14, flexShrink: 0,
                    background: 'linear-gradient(135deg, var(--amber-500), var(--amber-400))',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 22, fontWeight: 800, color: 'var(--navy-950)',
                  }}>
                    {selected.firstName.charAt(0)}{selected.lastName.charAt(0)}
                  </div>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: 20, fontWeight: 800, color: '#fff', letterSpacing: '-0.01em' }}>
                      {selected.firstName} {selected.lastName}
                    </div>
                    <div style={{ fontFamily: "'JetBrains Mono',monospace", fontSize: 12, color: 'var(--amber-400)', marginTop: 2 }}>
                      {selected.cifNumber}
                    </div>
                    <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
                      <span style={{
                        padding: '3px 10px', borderRadius: 99, fontSize: 10, fontWeight: 700,
                        background: (SEGMENT_COLORS[selected.customerSegment] || SEGMENT_COLORS.RETAIL).bg,
                        color: (SEGMENT_COLORS[selected.customerSegment] || SEGMENT_COLORS.RETAIL).color,
                      }}>
                        {selected.customerSegment}
                      </span>
                      <span className={`badge ${KYC_META[selected.kycStatus]?.cls || 'badge-neutral'}`}>
                        {KYC_META[selected.kycStatus]?.label || selected.kycStatus}
                      </span>
                    </div>
                  </div>
                </div>

                {/* KYC Pipeline */}
                <div style={{ padding: '20px 28px', borderBottom: '1px solid var(--slate-100)' }}>
                  <p style={{ fontSize: 11, fontWeight: 700, color: 'var(--slate-400)', letterSpacing: '0.06em', textTransform: 'uppercase', marginBottom: 16 }}>
                    KYC Verification Pipeline
                  </p>
                  <KycProgressBar status={selected.kycStatus} />
                  {['NOT_SUBMITTED','SUBMITTED','UNDER_REVIEW'].includes(selected.kycStatus) && (
                    <button
                      onClick={handleAdvanceKyc}
                      disabled={kycUpdating}
                      className="btn btn-success"
                      style={{ marginTop: 14, fontSize: 12, padding: '8px 16px' }}>
                      <ChevronRight size={13} />
                      {kycUpdating ? 'Updating...' : `Advance KYC → ${
                        { NOT_SUBMITTED: 'Submit Docs', SUBMITTED: 'Start Review', UNDER_REVIEW: 'Approve KYC' }[selected.kycStatus]
                      }`}
                    </button>
                  )}
                </div>

                {/* Details grid */}
                <div style={{ padding: '16px 28px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 24px' }}>
                  {[
                    { icon: Calendar, label: 'Date of Birth', value: new Date(selected.dateOfBirth).toLocaleDateString('en-IN', { day: '2-digit', month: 'long', year: 'numeric' }) },
                    { icon: User,     label: 'Gender',        value: selected.gender },
                    { icon: Phone,    label: 'Mobile',         value: selected.phone },
                    { icon: Mail,     label: 'Email',          value: selected.email },
                    { icon: MapPin,   label: 'City / State',   value: `${selected.city}, ${selected.state}` },
                    { icon: FileText, label: 'PAN Number',     value: selected.panNumber || 'Not Provided' },
                    { icon: ShieldCheck, label: 'Aadhaar',     value: selected.aadhaarNumber || 'Not Provided' },
                    { icon: Users,    label: 'RM Assigned',    value: selected.relationshipManager || 'Not Assigned' },
                  ].map((row, i) => {
                    const Icon = row.icon;
                    return (
                      <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 0', borderBottom: '1px solid var(--slate-100)' }}>
                        <Icon size={13} color="var(--slate-400)" style={{ flexShrink: 0 }} />
                        <div>
                          <div style={{ fontSize: 10, color: 'var(--slate-400)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.04em' }}>{row.label}</div>
                          <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--slate-800)' }}>{row.value}</div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Linked Accounts */}
              {selected.accounts?.length > 0 && (
                <div className="card">
                  <h3 className="card-title" style={{ marginBottom: 14 }}>
                    <CreditCard size={14} style={{ display: 'inline', marginRight: 6, verticalAlign: 'middle' }} />
                    Linked Accounts
                  </h3>
                  {selected.accounts.map((acc, i) => (
                    <div key={i} style={{
                      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                      padding: '12px 16px', borderRadius: 10,
                      background: 'var(--slate-50)', marginBottom: 8,
                      border: '1px solid var(--slate-200)',
                    }}>
                      <div>
                        <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--slate-800)' }}>{acc.accountType} Account</div>
                        <div style={{ fontFamily: "'JetBrains Mono',monospace", fontSize: 11, color: 'var(--slate-400)' }}>{acc.accountNumber}</div>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <div style={{ fontFamily: "'JetBrains Mono',monospace", fontSize: 15, fontWeight: 800, color: 'var(--emerald-600)' }}>
                          ₹{acc.currentBalance.toLocaleString('en-IN')}
                        </div>
                        <span className={`badge ${acc.accountStatus === 'ACTIVE' ? 'badge-success' : 'badge-danger'}`}>
                          {acc.accountStatus}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}

            </div>
          ) : (
            <div className="card" style={{ textAlign: 'center', padding: '60px 24px', color: 'var(--slate-400)' }}>
              <Users size={36} style={{ margin: '0 auto 14px', display: 'block', opacity: 0.25 }} />
              <p style={{ fontSize: 14, fontWeight: 500 }}>Select a customer to view their full profile</p>
              <p style={{ fontSize: 12, marginTop: 6 }}>KYC details, linked accounts, and loan portfolio</p>
            </div>
          )}
        </div>
      </div>

      {/* Create Customer Modal */}
      {showModal && <CreateCustomerModal onClose={() => setShowModal(false)} onSuccess={showToast} />}
    </div>
  );
}
