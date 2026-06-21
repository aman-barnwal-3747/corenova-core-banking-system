/**
 * LoansPage.jsx – Loan portfolio + EMI calculator + application
 * Backend: LoanController (apply/approve/disburse/EMI calc)
 */
import React, { useState } from 'react';
import { loanApi } from '../api';

const STATUS_BADGE = { APPLIED:'badge-warning', APPROVED:'badge-info', ACTIVE:'badge-success', DISBURSED:'badge-success', REJECTED:'badge-danger', DEFAULTED:'badge-danger', CLOSED:'badge-neutral' };
const TYPE_ICON = { HOME:'🏠', PERSONAL:'💰', AUTO:'🚗', EDUCATION:'🎓', BUSINESS:'🏢', GOLD:'🪙' };

const DEMO_LOANS = [
  { loanNumber:'LNHL20260001', loanType:'HOME',     loanStatus:'ACTIVE',   sanctionedAmount:3500000, outstandingPrincipal:3180000, interestRate:8.5, tenureMonths:240, emiAmount:30362, emisPaid:8,  customer:{cifNumber:'CNB-CUST-20250001',firstName:'Aman',lastName:'Verma'}, nextEmiDate:'2026-07-01' },
  { loanNumber:'LNPL20260002', loanType:'PERSONAL', loanStatus:'ACTIVE',   sanctionedAmount:500000,  outstandingPrincipal:380000,  interestRate:11.5,tenureMonths:36,  emiAmount:16489, emisPaid:14, customer:{cifNumber:'CNB-CUST-20250002',firstName:'Priya',lastName:'Sharma'}, nextEmiDate:'2026-07-05' },
  { loanNumber:'LNAL20260003', loanType:'AUTO',     loanStatus:'APPROVED', sanctionedAmount:850000,  outstandingPrincipal:850000,  interestRate:9.0, tenureMonths:60,  emiAmount:17657, emisPaid:0,  customer:{cifNumber:'CNB-CUST-20250003',firstName:'Ravi',lastName:'Kumar'},   nextEmiDate:null },
  { loanNumber:'LNED20260004', loanType:'EDUCATION',loanStatus:'APPLIED',  sanctionedAmount:1200000, outstandingPrincipal:1200000, interestRate:7.5, tenureMonths:120, emiAmount:14254, emisPaid:0,  customer:{cifNumber:'CNB-CUST-20250006',firstName:'Leena',lastName:'Patel'},  nextEmiDate:null },
];

function ApplyModal({ onClose, onSubmit }) {
  const [form,setForm]=useState({ cifNumber:'', loanType:'HOME', requestedAmount:'', tenureMonths:'', interestRate:'8.5', purpose:'', repaymentAccountNumber:'' });
  const [emi,setEmi]=useState(null);
  const set=(k,v)=>setForm(f=>({...f,[k]:v}));

  const calcEmi = async () => {
    if(!form.requestedAmount||!form.tenureMonths) return;
    try {
      const { data } = await loanApi.calcEmi(form.requestedAmount, form.interestRate, form.tenureMonths);
      setEmi(data);
    } catch {
      const P=+form.requestedAmount, r=+form.interestRate/1200, n=+form.tenureMonths;
      const e = r===0 ? P/n : (P*r*Math.pow(1+r,n))/(Math.pow(1+r,n)-1);
      setEmi({ emiAmount:e, totalPayment:e*n, totalInterest:e*n-P });
    }
  };

  return (
    <div style={{ position:'fixed',inset:0,zIndex:1000,background:'rgba(15,23,42,0.5)',display:'flex',alignItems:'center',justifyContent:'center',padding:20 }}>
      <div style={{ background:'#fff',borderRadius:16,width:'100%',maxWidth:520,maxHeight:'90vh',overflow:'auto',padding:24 }}>
        <h3 style={{ fontSize:15,fontWeight:700,marginBottom:16 }}>New Loan Application</h3>
        <div style={{ display:'flex',flexDirection:'column',gap:12 }}>
          <div className="grid-2">
            <div className="form-group"><label className="form-label">Customer CIF *</label><input className="form-input" value={form.cifNumber} onChange={e=>set('cifNumber',e.target.value)} placeholder="CNB-CUST-20250001" /></div>
            <div className="form-group">
              <label className="form-label">Loan Type *</label>
              <select className="form-input form-select" value={form.loanType} onChange={e=>set('loanType',e.target.value)}>
                {Object.entries(TYPE_ICON).map(([k])=><option key={k} value={k}>{k}</option>)}
              </select>
            </div>
          </div>
          <div className="grid-2">
            <div className="form-group"><label className="form-label">Amount (₹) *</label><input className="form-input mono" type="number" value={form.requestedAmount} onChange={e=>set('requestedAmount',e.target.value)} /></div>
            <div className="form-group"><label className="form-label">Tenure (months) *</label><input className="form-input" type="number" value={form.tenureMonths} onChange={e=>set('tenureMonths',e.target.value)} placeholder="e.g. 240" /></div>
          </div>
          <div className="grid-2">
            <div className="form-group"><label className="form-label">Interest Rate (% p.a.)</label><input className="form-input" type="number" step="0.1" value={form.interestRate} onChange={e=>set('interestRate',e.target.value)} /></div>
            <div className="form-group"><label className="form-label">Repayment Account</label><input className="form-input mono" value={form.repaymentAccountNumber} onChange={e=>set('repaymentAccountNumber',e.target.value)} placeholder="1001000001" /></div>
          </div>
          <div className="form-group"><label className="form-label">Purpose</label><input className="form-input" value={form.purpose} onChange={e=>set('purpose',e.target.value)} placeholder="e.g. Home purchase" /></div>

          <button onClick={calcEmi} className="btn btn-ghost" style={{ alignSelf:'flex-start' }}>🧮 Calculate EMI</button>

          {emi && (
            <div style={{ background:'#EFF6FF',border:'1px solid #BFDBFE',borderRadius:10,padding:16,display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:12,textAlign:'center' }}>
              <div><div style={{fontSize:10,color:'#64748B'}}>Monthly EMI</div><div style={{fontSize:16,fontWeight:800,color:'#1D4ED8'}}>₹{Number(emi.emiAmount).toLocaleString('en-IN',{maximumFractionDigits:0})}</div></div>
              <div><div style={{fontSize:10,color:'#64748B'}}>Total Interest</div><div style={{fontSize:16,fontWeight:800,color:'#0F172A'}}>₹{Number(emi.totalInterest).toLocaleString('en-IN',{maximumFractionDigits:0})}</div></div>
              <div><div style={{fontSize:10,color:'#64748B'}}>Total Payment</div><div style={{fontSize:16,fontWeight:800,color:'#0F172A'}}>₹{Number(emi.totalPayment).toLocaleString('en-IN',{maximumFractionDigits:0})}</div></div>
            </div>
          )}
        </div>
        <div style={{ display:'flex',gap:10,marginTop:18 }}>
          <button onClick={onClose} className="btn btn-ghost">Cancel</button>
          <button onClick={()=>{onSubmit(form);onClose();}} disabled={!form.cifNumber||!form.requestedAmount||!form.tenureMonths} className="btn btn-primary" style={{ flex:1,justifyContent:'center' }}>Submit Application</button>
        </div>
      </div>
    </div>
  );
}

export default function LoansPage() {
  const [loans, setLoans] = useState(DEMO_LOANS);
  const [showModal, setShowModal] = useState(false);
  const [toast, setToast] = useState('');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const showToast=(m)=>{setToast(m);setTimeout(()=>setToast(''),4000);};

  const totalBook = loans.filter(l=>['ACTIVE','DISBURSED'].includes(l.loanStatus)).reduce((s,l)=>s+l.outstandingPrincipal,0);
  const filtered = typeFilter==='ALL'?loans:loans.filter(l=>l.loanType===typeFilter);

  const apply = async (form) => {
    try { const { data } = await loanApi.apply({...form, requestedAmount:+form.requestedAmount, tenureMonths:+form.tenureMonths, interestRate:+form.interestRate}); setLoans(l=>[data,...l]); }
    catch {
      const P=+form.requestedAmount, r=+form.interestRate/1200, n=+form.tenureMonths;
      const emi = r===0?P/n:(P*r*Math.pow(1+r,n))/(Math.pow(1+r,n)-1);
      setLoans(l=>[{ loanNumber:`LN${form.loanType.slice(0,2)}${Date.now()}`, loanType:form.loanType, loanStatus:'APPLIED', sanctionedAmount:P, outstandingPrincipal:P, interestRate:+form.interestRate, tenureMonths:n, emiAmount:emi, emisPaid:0, customer:{cifNumber:form.cifNumber,firstName:'New',lastName:'Applicant'}, nextEmiDate:null },...l]);
    }
    showToast('Loan application submitted! Awaiting checker approval.');
  };

  const approve = async (ln) => {
    try { await loanApi.approve(ln,'Approved'); } catch {}
    setLoans(l=>l.map(x=>x.loanNumber===ln?{...x,loanStatus:'APPROVED'}:x));
    showToast(`${ln} approved.`);
  };

  return (
    <div>
      {toast && <div style={{ position:'fixed',top:80,right:24,zIndex:2000,background:'#0F2342',color:'#fff',padding:'12px 20px',borderRadius:10,fontSize:13,fontWeight:500 }}>✓ {toast}</div>}
      <div className="page-header flex items-center justify-between">
        <div><h1 className="page-title">Loans</h1><p className="page-desc">Loan portfolio, EMI tracking, and application management</p></div>
        <button className="btn btn-primary" onClick={()=>setShowModal(true)}>+ New Loan Application</button>
      </div>

      <div className="grid-4 mb-5">
        <div className="card" style={{padding:'16px 20px'}}><div style={{fontSize:22,fontWeight:800}}>₹{(totalBook/100000).toFixed(2)} L</div><div style={{fontSize:12,color:'#64748B',marginTop:4}}>Total Loan Book</div></div>
        <div className="card" style={{padding:'16px 20px'}}><div style={{fontSize:22,fontWeight:800}}>{loans.filter(l=>['ACTIVE','DISBURSED'].includes(l.loanStatus)).length}</div><div style={{fontSize:12,color:'#64748B',marginTop:4}}>Active Loans</div></div>
        <div className="card" style={{padding:'16px 20px'}}><div style={{fontSize:22,fontWeight:800}}>{loans.filter(l=>l.loanStatus==='APPLIED').length}</div><div style={{fontSize:12,color:'#64748B',marginTop:4}}>Pending Applications</div></div>
        <div className="card" style={{padding:'16px 20px'}}><div style={{fontSize:22,fontWeight:800}}>{loans.filter(l=>l.loanStatus==='APPROVED').length}</div><div style={{fontSize:12,color:'#64748B',marginTop:4}}>Awaiting Disbursement</div></div>
      </div>

      <div style={{ display:'flex',gap:8,marginBottom:16 }}>
        {['ALL',...Object.keys(TYPE_ICON)].map(t=>(
          <button key={t} onClick={()=>setTypeFilter(t)} className={`btn ${typeFilter===t?'btn-primary':'btn-ghost'}`} style={{fontSize:12}}>{t!=='ALL'&&TYPE_ICON[t]+' '}{t}</button>
        ))}
      </div>

      <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill,minmax(320px,1fr))', gap:14 }}>
        {filtered.map(l=>{
          const progress = l.tenureMonths ? Math.round((l.emisPaid/l.tenureMonths)*100) : 0;
          return (
            <div key={l.loanNumber} className="card">
              <div style={{ display:'flex',justifyContent:'space-between',alignItems:'flex-start',marginBottom:12 }}>
                <div style={{ display:'flex',gap:10,alignItems:'center' }}>
                  <div style={{ width:40,height:40,borderRadius:10,background:'#F1F5F9',display:'flex',alignItems:'center',justifyContent:'center',fontSize:18 }}>{TYPE_ICON[l.loanType]}</div>
                  <div>
                    <div className="mono" style={{ fontSize:11,fontWeight:700,color:'#1D4ED8' }}>{l.loanNumber}</div>
                    <div style={{ fontSize:12,color:'#64748B' }}>{l.customer.firstName} {l.customer.lastName}</div>
                  </div>
                </div>
                <span className={`badge ${STATUS_BADGE[l.loanStatus]}`}>{l.loanStatus}</span>
              </div>
              <div style={{ fontSize:20,fontWeight:800,color:'#0F172A',marginBottom:2 }}>₹{l.sanctionedAmount.toLocaleString('en-IN')}</div>
              <div style={{ fontSize:11,color:'#94A3B8',marginBottom:14 }}>@ {l.interestRate}% p.a. · {l.tenureMonths} months</div>

              {['ACTIVE','DISBURSED'].includes(l.loanStatus) && (
                <>
                  <div style={{ display:'flex',justifyContent:'space-between',fontSize:11,color:'#64748B',marginBottom:4 }}>
                    <span>EMI Progress</span><span>{l.emisPaid}/{l.tenureMonths} paid</span>
                  </div>
                  <div style={{ height:6,background:'#F1F5F9',borderRadius:99,overflow:'hidden',marginBottom:10 }}>
                    <div style={{ height:'100%',width:`${progress}%`,background:'linear-gradient(90deg,#3B82F6,#10B981)',borderRadius:99 }} />
                  </div>
                </>
              )}

              <div style={{ display:'flex',justifyContent:'space-between',fontSize:12,padding:'10px 0',borderTop:'1px solid #F1F5F9' }}>
                <span style={{ color:'#64748B' }}>Monthly EMI</span>
                <span style={{ fontWeight:700, color:'#0F172A' }} className="mono">₹{Math.round(l.emiAmount).toLocaleString('en-IN')}</span>
              </div>
              <div style={{ display:'flex',justifyContent:'space-between',fontSize:12,padding:'4px 0 10px' }}>
                <span style={{ color:'#64748B' }}>Outstanding</span>
                <span style={{ fontWeight:700, color:'#0F172A' }} className="mono">₹{l.outstandingPrincipal.toLocaleString('en-IN')}</span>
              </div>
              {l.nextEmiDate && (
                <div style={{ display:'flex',justifyContent:'space-between',fontSize:11,color:'#94A3B8' }}>
                  <span>Next EMI</span><span>{new Date(l.nextEmiDate).toLocaleDateString('en-IN',{day:'2-digit',month:'short',year:'numeric'})}</span>
                </div>
              )}

              {l.loanStatus==='APPLIED' && (
                <button onClick={()=>approve(l.loanNumber)} className="btn btn-success w-full" style={{ justifyContent:'center', marginTop:12, fontSize:12 }}>
                  ✅ Approve (Checker)
                </button>
              )}
            </div>
          );
        })}
      </div>

      {showModal && <ApplyModal onClose={()=>setShowModal(false)} onSubmit={apply} />}
    </div>
  );
}
