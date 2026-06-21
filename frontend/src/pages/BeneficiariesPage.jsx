/**
 * BeneficiariesPage.jsx – PDF §8 Payment Gateway
 * Saved payees with cooling period status, add/remove
 */
import React, { useState } from 'react';
import { beneficiaryApi } from '../api';

const DEMO = [
  { beneficiaryId:1, beneficiaryName:'Priya Sharma',   nickname:'Wife',       beneficiaryType:'INTERNAL', beneficiaryAccountNumber:'1001000002', beneficiaryBankName:'CoreNova Bank', beneficiaryIfsc:'CNB0MAIN001', isVerified:true,  totalTransfers:12, totalAmountTransferred:245000 },
  { beneficiaryId:2, beneficiaryName:'Ramesh Kumar',   nickname:'Father',     beneficiaryType:'EXTERNAL', beneficiaryAccountNumber:'9876543210', beneficiaryBankName:'HDFC Bank',     beneficiaryIfsc:'HDFC0001234', isVerified:true,  totalTransfers:5,  totalAmountTransferred:85000 },
  { beneficiaryId:3, beneficiaryName:'ABC Pvt. Ltd.',  nickname:'Employer',   beneficiaryType:'EXTERNAL', beneficiaryAccountNumber:'1122334455', beneficiaryBankName:'ICICI Bank',    beneficiaryIfsc:'ICIC0002345', isVerified:true,  totalTransfers:1,  totalAmountTransferred:55000 },
  { beneficiaryId:4, beneficiaryName:'priya@paytm',    nickname:'Priya UPI',  beneficiaryType:'UPI',      beneficiaryAccountNumber:null,        beneficiaryBankName:'Paytm',         beneficiaryIfsc:null,          isVerified:true,  totalTransfers:8,  totalAmountTransferred:32000 },
  { beneficiaryId:5, beneficiaryName:'Sunita Patel',   nickname:'Sister',     beneficiaryType:'INTERNAL', beneficiaryAccountNumber:'1001000004', beneficiaryBankName:'CoreNova Bank', beneficiaryIfsc:'CNB0MAIN001', isVerified:false, totalTransfers:0,  totalAmountTransferred:0, coolingPeriodEndsAt: new Date(Date.now()+14*3600000).toISOString() },
];

const TYPE_STYLE = { INTERNAL:{bg:'#DBEAFE',color:'#1D4ED8',icon:'🏦'}, EXTERNAL:{bg:'#D1FAE5',color:'#059669',icon:'🏛️'}, UPI:{bg:'#EDE9FE',color:'#7C3AED',icon:'📱'} };

function AddModal({ onClose, onAdd }) {
  const [form, setForm] = useState({ accountNumber:'1001000001', type:'EXTERNAL', beneficiaryName:'', nickname:'', beneficiaryAccountNumber:'', beneficiaryIfsc:'', beneficiaryBankName:'', upiId:'' });
  const [loading, setLoading] = useState(false);
  const set = (k,v) => setForm(f=>({...f,[k]:v}));

  const handleAdd = async () => {
    if (!form.beneficiaryName) return;
    setLoading(true);
    try { await beneficiaryApi.add({ ...form, byBranch: false }); }
    catch { /* demo */ }
    onAdd(`${form.beneficiaryName} added! 24-hour cooling period applies.`);
    onClose(); setLoading(false);
  };

  return (
    <div style={{ position:'fixed',inset:0,zIndex:1000,background:'rgba(15,23,42,0.5)',display:'flex',alignItems:'center',justifyContent:'center',padding:20 }}>
      <div style={{ background:'#fff',borderRadius:16,width:'100%',maxWidth:500,maxHeight:'90vh',overflow:'auto',boxShadow:'0 20px 60px rgba(0,0,0,0.15)' }}>
        <div style={{ background:'linear-gradient(135deg,#0F2342,#1E3A6E)',padding:'22px 24px',borderRadius:'16px 16px 0 0',display:'flex',justifyContent:'space-between',alignItems:'center' }}>
          <h3 style={{ color:'#fff',fontSize:15,fontWeight:700 }}>Add New Beneficiary</h3>
          <button onClick={onClose} style={{ background:'rgba(255,255,255,0.1)',border:'none',borderRadius:'50%',width:28,height:28,cursor:'pointer',color:'#fff',display:'flex',alignItems:'center',justifyContent:'center' }}>✕</button>
        </div>
        <div style={{ padding:24, display:'flex', flexDirection:'column', gap:14 }}>
          <div className="form-group">
            <label className="form-label">From Account</label>
            <input className="form-input mono" value={form.accountNumber} onChange={e=>set('accountNumber',e.target.value)} />
          </div>
          <div className="form-group">
            <label className="form-label">Beneficiary Type</label>
            <select className="form-input form-select" value={form.type} onChange={e=>set('type',e.target.value)}>
              <option value="INTERNAL">Internal (CoreNova)</option>
              <option value="EXTERNAL">External (Other Bank)</option>
              <option value="UPI">UPI</option>
            </select>
          </div>
          <div className="grid-2">
            <div className="form-group"><label className="form-label">Full Name *</label><input className="form-input" value={form.beneficiaryName} onChange={e=>set('beneficiaryName',e.target.value)} placeholder="Name as per bank" /></div>
            <div className="form-group"><label className="form-label">Nickname</label><input className="form-input" value={form.nickname} onChange={e=>set('nickname',e.target.value)} placeholder="e.g. Mom, Rent" /></div>
          </div>
          {form.type !== 'UPI' && <>
            <div className="form-group"><label className="form-label">Account Number *</label><input className="form-input mono" value={form.beneficiaryAccountNumber} onChange={e=>set('beneficiaryAccountNumber',e.target.value)} /></div>
            <div className="grid-2">
              <div className="form-group"><label className="form-label">IFSC Code *</label><input className="form-input mono" value={form.beneficiaryIfsc} onChange={e=>set('beneficiaryIfsc',e.target.value)} placeholder="XXXX0XXXXXX" /></div>
              <div className="form-group"><label className="form-label">Bank Name</label><input className="form-input" value={form.beneficiaryBankName} onChange={e=>set('beneficiaryBankName',e.target.value)} /></div>
            </div>
          </>}
          {form.type === 'UPI' && <div className="form-group"><label className="form-label">UPI ID *</label><input className="form-input" value={form.upiId} onChange={e=>set('upiId',e.target.value)} placeholder="name@upi" /></div>}
          <div style={{ background:'#FEF3C7', border:'1px solid #FCD34D', borderRadius:8, padding:'10px 14px', fontSize:12, color:'#92400E' }}>
            ⏳ A 24-hour cooling period will apply before you can transfer to this beneficiary.
          </div>
          <div style={{ display:'flex', gap:10, marginTop:4 }}>
            <button onClick={onClose} className="btn btn-ghost">Cancel</button>
            <button onClick={handleAdd} disabled={loading} className="btn btn-primary" style={{ flex:1, justifyContent:'center' }}>
              {loading ? 'Adding...' : '+ Add Beneficiary'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function BeneficiariesPage() {
  const [bens,      setBens]      = useState(DEMO);
  const [showModal, setShowModal] = useState(false);
  const [toast,     setToast]     = useState('');
  const [filter,    setFilter]    = useState('ALL');

  const showToast = (m) => { setToast(m); setTimeout(()=>setToast(''),4000); };
  const filtered  = filter === 'ALL' ? bens : bens.filter(b => b.beneficiaryType === filter);
  const handleDelete = (id) => { setBens(b => b.filter(x => x.beneficiaryId !== id)); showToast('Beneficiary removed.'); };

  const hoursLeft = (iso) => {
    if (!iso) return 0;
    return Math.max(0, Math.round((new Date(iso) - Date.now()) / 3600000));
  };

  return (
    <div>
      {toast && <div style={{ position:'fixed',top:80,right:24,zIndex:2000,background:'#0F2342',color:'#fff',padding:'12px 20px',borderRadius:10,fontSize:13,fontWeight:500 }}>✓ {toast}</div>}
      <div className="page-header flex items-center justify-between">
        <div><h1 className="page-title">Beneficiaries</h1><p className="page-desc">Saved payees — manage your trusted transfer recipients</p></div>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>+ Add Beneficiary</button>
      </div>

      {/* Stats */}
      <div className="grid-4 mb-5">
        {[{l:'Total',v:bens.length},{l:'Internal',v:bens.filter(b=>b.beneficiaryType==='INTERNAL').length},{l:'External',v:bens.filter(b=>b.beneficiaryType==='EXTERNAL').length},{l:'UPI',v:bens.filter(b=>b.beneficiaryType==='UPI').length}].map((s,i)=>(
          <div key={i} className="card" style={{ padding:'16px 20px' }}>
            <div style={{ fontSize:24,fontWeight:800,color:'#0F172A' }}>{s.v}</div>
            <div style={{ fontSize:12,color:'#64748B',marginTop:4 }}>{s.l} Beneficiaries</div>
          </div>
        ))}
      </div>

      {/* Filter */}
      <div style={{ display:'flex', gap:8, marginBottom:16 }}>
        {['ALL','INTERNAL','EXTERNAL','UPI'].map(f=>(
          <button key={f} onClick={()=>setFilter(f)} className={`btn ${filter===f?'btn-primary':'btn-ghost'}`} style={{ fontSize:12 }}>{f}</button>
        ))}
      </div>

      {/* Cards grid */}
      <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill,minmax(300px,1fr))', gap:14 }}>
        {filtered.map(b => {
          const ts = TYPE_STYLE[b.beneficiaryType] || TYPE_STYLE.EXTERNAL;
          const cooling = !b.isVerified && b.coolingPeriodEndsAt;
          return (
            <div key={b.beneficiaryId} className="card" style={{ position:'relative' }}>
              <div style={{ display:'flex', alignItems:'flex-start', gap:12, marginBottom:14 }}>
                <div style={{ width:44,height:44,borderRadius:10,background:ts.bg,display:'flex',alignItems:'center',justifyContent:'center',fontSize:20,flexShrink:0 }}>{ts.icon}</div>
                <div style={{ flex:1, minWidth:0 }}>
                  <div style={{ fontWeight:700,fontSize:14,color:'#0F172A' }}>{b.beneficiaryName}</div>
                  <div style={{ fontSize:11,color:'#64748B' }}>{b.nickname}</div>
                </div>
                <span style={{ padding:'3px 8px',borderRadius:99,fontSize:10,fontWeight:700,background:ts.bg,color:ts.color }}>{b.beneficiaryType}</span>
              </div>
              <div style={{ display:'flex',flexDirection:'column',gap:6,fontSize:12,marginBottom:14 }}>
                {b.beneficiaryAccountNumber && <div style={{ display:'flex',justifyContent:'space-between' }}><span style={{ color:'#94A3B8' }}>Account</span><span className="mono" style={{ fontWeight:600 }}>{b.beneficiaryAccountNumber}</span></div>}
                {b.beneficiaryIfsc          && <div style={{ display:'flex',justifyContent:'space-between' }}><span style={{ color:'#94A3B8' }}>IFSC</span><span className="mono" style={{ fontWeight:600 }}>{b.beneficiaryIfsc}</span></div>}
                <div style={{ display:'flex',justifyContent:'space-between' }}><span style={{ color:'#94A3B8' }}>Bank</span><span style={{ fontWeight:600,color:'#334155' }}>{b.beneficiaryBankName}</span></div>
                <div style={{ display:'flex',justifyContent:'space-between' }}><span style={{ color:'#94A3B8' }}>Transfers</span><span style={{ fontWeight:600 }}>{b.totalTransfers}</span></div>
              </div>

              {/* Cooling indicator */}
              {cooling && (
                <div style={{ background:'#FEF3C7',border:'1px solid #FCD34D',borderRadius:8,padding:'8px 12px',fontSize:11,color:'#92400E',marginBottom:12 }}>
                  ⏳ Cooling period: {hoursLeft(b.coolingPeriodEndsAt)}h remaining — transfers blocked until verified
                </div>
              )}
              {b.isVerified && (
                <div style={{ background:'#D1FAE5',borderRadius:8,padding:'6px 12px',fontSize:11,color:'#059669',fontWeight:600,marginBottom:12 }}>
                  ✅ Verified — transfers allowed
                </div>
              )}

              <button onClick={() => handleDelete(b.beneficiaryId)} style={{ background:'none',border:'1px solid #FEE2E2',borderRadius:8,padding:'6px 14px',cursor:'pointer',fontSize:12,color:'#DC2626',width:'100%',fontFamily:'inherit' }}>
                Remove Beneficiary
              </button>
            </div>
          );
        })}
      </div>
      {showModal && <AddModal onClose={()=>setShowModal(false)} onAdd={showToast} />}
    </div>
  );
}
