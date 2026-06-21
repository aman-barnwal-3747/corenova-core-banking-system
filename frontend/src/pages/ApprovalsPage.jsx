/**
 * ApprovalsPage.jsx – PDF §9 Maker-Checker Workflow
 * Two tabs: Pending Queue (Checker) | My Requests (Maker)
 * Four-eyes principle enforced — checker ≠ maker
 */
import React, { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { approvalApi } from '../api';

const RISK_STYLE = { CRITICAL:{bg:'#FEE2E2',color:'#DC2626'}, HIGH:{bg:'#FFEDD5',color:'#EA580C'}, MEDIUM:{bg:'#FEF3C7',color:'#D97706'}, LOW:{bg:'#D1FAE5',color:'#059669'} };
const STATUS_BADGE = { PENDING:'badge-warning', APPROVED:'badge-success', REJECTED:'badge-danger', RETURNED:'badge-info', EXPIRED:'badge-neutral' };

const DEMO_PENDING = [
  { approvalRef:'APR2026000012', actionType:'FUND_TRANSFER', actionDescription:'High-value transfer ₹6,50,000 to Ramesh Kumar (HDFC0001234)', entityType:'ACCOUNT', entityRef:'1001000001', amountInvolved:650000, riskLevel:'CRITICAL', createdByMaker:'teller', createdAt:new Date(Date.now()-3600000).toISOString(), expiresAt:new Date(Date.now()+20*3600000).toISOString(), approvalStatus:'PENDING' },
  { approvalRef:'APR2026000013', actionType:'ACCOUNT_FREEZE', actionDescription:'Freeze account 1001000005 — suspicious activity flagged by fraud system', entityType:'ACCOUNT', entityRef:'1001000005', amountInvolved:null, riskLevel:'HIGH', createdByMaker:'teller', createdAt:new Date(Date.now()-7200000).toISOString(), expiresAt:new Date(Date.now()+16*3600000).toISOString(), approvalStatus:'PENDING' },
  { approvalRef:'APR2026000014', actionType:'KYC_APPROVE', actionDescription:'Approve KYC for Meera Nair (CNB-CUST-20250004) — all documents verified', entityType:'CUSTOMER', entityRef:'CNB-CUST-20250004', amountInvolved:null, riskLevel:'HIGH', createdByMaker:'teller', createdAt:new Date(Date.now()-10800000).toISOString(), expiresAt:new Date(Date.now()+13*3600000).toISOString(), approvalStatus:'PENDING' },
  { approvalRef:'APR2026000015', actionType:'LOAN_SANCTION', actionDescription:'Sanction Home Loan LNHL20260002 for ₹35,00,000 @ 8.5% — 240 months', entityType:'LOAN', entityRef:'LNHL20260002', amountInvolved:3500000, riskLevel:'CRITICAL', createdByMaker:'manager', createdAt:new Date(Date.now()-1800000).toISOString(), expiresAt:new Date(Date.now()+22*3600000).toISOString(), approvalStatus:'PENDING' },
  { approvalRef:'APR2026000016', actionType:'FUND_TRANSFER', actionDescription:'NEFT ₹2,40,000 to ABC Suppliers (ICIC0002345)', entityType:'ACCOUNT', entityRef:'1001000002', amountInvolved:240000, riskLevel:'MEDIUM', createdByMaker:'teller', createdAt:new Date(Date.now()-300000).toISOString(), expiresAt:new Date(Date.now()+23*3600000).toISOString(), approvalStatus:'PENDING' },
];

const DEMO_MY_REQUESTS = [
  { approvalRef:'APR2026000010', actionType:'FUND_TRANSFER', actionDescription:'Transfer ₹5,20,000 to Priya Sharma', amountInvolved:520000, riskLevel:'HIGH', approvalStatus:'APPROVED', createdAt:new Date(Date.now()-86400000).toISOString(), checkedBy:'manager', checkedAt:new Date(Date.now()-82800000).toISOString(), checkerRemarks:'Verified — customer KYC complete' },
  { approvalRef:'APR2026000011', actionType:'ACCOUNT_FREEZE', actionDescription:'Freeze request for account 1001000099', amountInvolved:null, riskLevel:'HIGH', approvalStatus:'REJECTED', createdAt:new Date(Date.now()-172800000).toISOString(), checkedBy:'manager', checkedAt:new Date(Date.now()-169200000).toISOString(), checkerRemarks:'Insufficient justification — please attach fraud alert reference' },
];

function timeAgo(iso){
  const mins = Math.floor((Date.now()-new Date(iso))/60000);
  if (mins<60) return `${mins}m ago`;
  if (mins<1440) return `${Math.floor(mins/60)}h ago`;
  return `${Math.floor(mins/1440)}d ago`;
}
function hoursLeft(iso){ return Math.max(0, Math.round((new Date(iso)-Date.now())/3600000)); }

function ActionModal({ item, type, onClose, onSubmit }) {
  const [remarks, setRemarks] = useState('');
  const [loading, setLoading] = useState(false);
  const isReject = type==='reject';
  return (
    <div style={{ position:'fixed',inset:0,zIndex:1000,background:'rgba(15,23,42,0.5)',display:'flex',alignItems:'center',justifyContent:'center',padding:20 }}>
      <div style={{ background:'#fff',borderRadius:16,width:'100%',maxWidth:440,padding:24 }}>
        <h3 style={{ fontSize:15,fontWeight:700,marginBottom:4 }}>{isReject?'Reject':'Approve'} Request</h3>
        <p style={{ fontSize:12,color:'#64748B',marginBottom:16 }}>{item.approvalRef} · {item.actionDescription}</p>
        <div className="form-group">
          <label className="form-label">{isReject?'Rejection Reason *':'Remarks'}</label>
          <textarea className="form-input" rows={3} value={remarks} onChange={e=>setRemarks(e.target.value)}
            placeholder={isReject?'Mandatory — explain why this is rejected':'Optional approval notes'}
            style={{ resize:'vertical', fontFamily:'inherit' }} />
        </div>
        <div style={{ display:'flex',gap:10,marginTop:16 }}>
          <button onClick={onClose} className="btn btn-ghost">Cancel</button>
          <button
            onClick={async ()=>{ if(isReject && !remarks.trim()) return; setLoading(true); await onSubmit(remarks); setLoading(false); }}
            disabled={loading || (isReject && !remarks.trim())}
            className={`btn ${isReject?'btn-danger':'btn-success'}`} style={{ flex:1, justifyContent:'center' }}>
            {loading?'Processing...':isReject?'Confirm Reject':'Confirm Approve'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function ApprovalsPage() {
  const { user } = useAuth();
  const isChecker = user?.role === 'ROLE_MANAGER' || user?.role === 'ROLE_ADMIN';
  const [tab,     setTab]     = useState(isChecker ? 'pending' : 'mine');
  const [pending, setPending] = useState(DEMO_PENDING);
  const [mine]                 = useState(DEMO_MY_REQUESTS);
  const [modal,   setModal]   = useState(null); // {item, type}
  const [toast,   setToast]   = useState('');

  const showToast = (m)=>{ setToast(m); setTimeout(()=>setToast(''),4000); };

  const handleAction = async (remarks) => {
    const { item, type } = modal;
    try {
      if (type==='approve') await approvalApi.approve(item.approvalRef, remarks||'Approved');
      else await approvalApi.reject(item.approvalRef, remarks);
    } catch { /* demo mode */ }
    setPending(p => p.filter(x => x.approvalRef !== item.approvalRef));
    showToast(`${item.approvalRef} ${type==='approve'?'approved':'rejected'} successfully.`);
    setModal(null);
  };

  const fmtAmt = (v) => v ? `₹ ${Number(v).toLocaleString('en-IN')}` : '—';

  return (
    <div>
      {toast && <div style={{ position:'fixed',top:80,right:24,zIndex:2000,background:'#0F2342',color:'#fff',padding:'12px 20px',borderRadius:10,fontSize:13,fontWeight:500 }}>✓ {toast}</div>}

      <div className="page-header">
        <h1 className="page-title">Approvals</h1>
        <p className="page-desc">PDF §9 Maker-Checker Workflow — four-eyes principle for high-value operations</p>
      </div>

      {/* Stats */}
      <div className="grid-4 mb-5">
        {[
          {l:'Pending Queue',v:pending.length,c:'#F59E0B'},
          {l:'Critical Risk',v:pending.filter(p=>p.riskLevel==='CRITICAL').length,c:'#DC2626'},
          {l:'SLA: 24 Hours',v:'⏱️',c:'#3B82F6'},
          {l:'My Requests',v:mine.length,c:'#8B5CF6'},
        ].map((s,i)=>(
          <div key={i} className="card" style={{ padding:'16px 20px' }}>
            <div style={{ fontSize:24,fontWeight:800,color:'#0F172A' }}>{s.v}</div>
            <div style={{ fontSize:12,color:'#64748B',marginTop:4,display:'flex',alignItems:'center',gap:6 }}>
              <span style={{ width:8,height:8,borderRadius:'50%',background:s.c,display:'inline-block' }} />{s.l}
            </div>
          </div>
        ))}
      </div>

      {/* Tabs */}
      <div style={{ display:'flex', gap:4, marginBottom:16, borderBottom:'1px solid #E2E8F0' }}>
        {[
          {k:'pending',l:'Pending Queue (Checker)',show:isChecker},
          {k:'mine',   l:'My Requests (Maker)',     show:true},
        ].filter(t=>t.show).map(t=>(
          <button key={t.k} onClick={()=>setTab(t.k)}
            style={{
              padding:'10px 18px', background:'none', border:'none', cursor:'pointer',
              fontSize:13, fontWeight:600, fontFamily:'inherit',
              color: tab===t.k?'#1D4ED8':'#64748B',
              borderBottom: tab===t.k?'2px solid #1D4ED8':'2px solid transparent',
              marginBottom:-1,
            }}>{t.l}</button>
        ))}
      </div>

      {/* Pending queue */}
      {tab==='pending' && (
        <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
          {pending.length===0 && <div className="card" style={{ textAlign:'center', padding:40, color:'#94A3B8' }}>✅ No pending approvals — queue is clear!</div>}
          {pending.map(item => {
            const risk = RISK_STYLE[item.riskLevel] || RISK_STYLE.LOW;
            const sameUser = item.createdByMaker === user?.username;
            return (
              <div key={item.approvalRef} className="card">
                <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:12 }}>
                  <div style={{ display:'flex', alignItems:'center', gap:10 }}>
                    <span className="mono" style={{ fontSize:12, fontWeight:700, color:'#1D4ED8' }}>{item.approvalRef}</span>
                    <span style={{ padding:'2px 9px',borderRadius:99,fontSize:10,fontWeight:700,background:risk.bg,color:risk.color }}>{item.riskLevel}</span>
                    <span style={{ padding:'2px 9px',borderRadius:99,fontSize:10,fontWeight:700,background:'#F1F5F9',color:'#475569' }}>{item.actionType.replace(/_/g,' ')}</span>
                  </div>
                  <div style={{ textAlign:'right' }}>
                    <div style={{ fontSize:11, color:'#94A3B8' }}>{timeAgo(item.createdAt)}</div>
                    <div style={{ fontSize:10, color: hoursLeft(item.expiresAt)<6?'#DC2626':'#94A3B8' }}>⏱ {hoursLeft(item.expiresAt)}h to expiry</div>
                  </div>
                </div>
                <p style={{ fontSize:13, color:'#334155', marginBottom:12, lineHeight:1.5 }}>{item.actionDescription}</p>
                <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
                  <div style={{ display:'flex', gap:20, fontSize:11, color:'#64748B' }}>
                    <span>Maker: <b style={{color:'#334155'}}>{item.createdByMaker}</b></span>
                    {item.amountInvolved && <span>Amount: <b style={{color:'#334155'}}>{fmtAmt(item.amountInvolved)}</b></span>}
                    <span>Entity: <b className="mono" style={{color:'#334155'}}>{item.entityRef}</b></span>
                  </div>
                  <div style={{ display:'flex', gap:8 }}>
                    {sameUser ? (
                      <span style={{ fontSize:11, color:'#DC2626', fontStyle:'italic' }}>⚠ Four-eyes: cannot self-approve</span>
                    ) : (
                      <>
                        <button onClick={()=>setModal({item,type:'reject'})} className="btn btn-danger" style={{ fontSize:12, padding:'6px 14px' }}>Reject</button>
                        <button onClick={()=>setModal({item,type:'approve'})} className="btn btn-success" style={{ fontSize:12, padding:'6px 14px' }}>Approve</button>
                      </>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* My requests */}
      {tab==='mine' && (
        <div className="card">
          <table className="data-table">
            <thead><tr><th>Ref</th><th>Action</th><th>Amount</th><th>Risk</th><th>Status</th><th>Checker</th><th>Remarks</th><th>Date</th></tr></thead>
            <tbody>
              {mine.map(m=>(
                <tr key={m.approvalRef}>
                  <td className="mono" style={{ fontSize:11,fontWeight:700,color:'#1D4ED8' }}>{m.approvalRef}</td>
                  <td style={{ fontSize:12 }}>{m.actionType.replace(/_/g,' ')}</td>
                  <td className="mono" style={{ fontSize:12 }}>{fmtAmt(m.amountInvolved)}</td>
                  <td><span style={{ padding:'2px 9px',borderRadius:99,fontSize:10,fontWeight:700, ...RISK_STYLE[m.riskLevel] }}>{m.riskLevel}</span></td>
                  <td><span className={`badge ${STATUS_BADGE[m.approvalStatus]}`}>{m.approvalStatus}</span></td>
                  <td style={{ fontSize:12 }}>{m.checkedBy||'—'}</td>
                  <td style={{ fontSize:11, color:'#64748B', maxWidth:200 }}>{m.checkerRemarks||'—'}</td>
                  <td style={{ fontSize:11, color:'#94A3B8' }}>{timeAgo(m.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {modal && <ActionModal item={modal.item} type={modal.type} onClose={()=>setModal(null)} onSubmit={handleAction} />}
    </div>
  );
}
