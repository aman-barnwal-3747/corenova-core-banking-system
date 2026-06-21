/**
 * AuditLogsPage.jsx – PDF §10 Audit Logging
 * Format: USER aman CREATED ACCOUNT 10001 AT 10:20 PM
 * Read-only, ADMIN/AUDITOR only
 */
import React, { useState } from 'react';
import { useAuth } from '../hooks/useAuth';

const ACTION_META = {
  LOGIN:                {icon:'🔑', color:'#3B82F6'},
  FUND_TRANSFER:        {icon:'💸', color:'#10B981'},
  UPI_PAYMENT:          {icon:'📱', color:'#8B5CF6'},
  NEFT_TRANSFER:        {icon:'🏛️', color:'#F59E0B'},
  ACCOUNT_OPEN:         {icon:'➕', color:'#10B981'},
  ACCOUNT_FREEZE:       {icon:'🔒', color:'#EF4444'},
  ACCOUNT_UNFREEZE:     {icon:'🔓', color:'#10B981'},
  KYC_STATUS_UPDATE:    {icon:'🛡️', color:'#3B82F6'},
  APPROVAL_APPROVED:    {icon:'✅', color:'#10B981'},
  APPROVAL_REJECTED:    {icon:'❌', color:'#EF4444'},
  USER_CREATE:          {icon:'👤', color:'#8B5CF6'},
  USER_ROLE_CHANGE:     {icon:'🔄', color:'#F59E0B'},
  USER_LOCK:            {icon:'🔒', color:'#EF4444'},
  PASSWORD_CHANGE:      {icon:'🔑', color:'#3B82F6'},
  BENEFICIARY_ADD:      {icon:'👥', color:'#10B981'},
  LOAN_APPLY:           {icon:'🏦', color:'#F59E0B'},
  LOAN_APPROVE:         {icon:'✅', color:'#10B981'},
};

const DEMO_LOGS = [
  { auditId:1, performedBy:'teller',  action:'FUND_TRANSFER',     module:'TRANSACTION', entityType:'ACCOUNT',  entityId:'1001000001', description:'Internal transfer ₹25,000 from 1001000001 to 1001000002', status:'SUCCESS', ipAddress:'192.168.1.45', performedAt:new Date(Date.now()-600000).toISOString() },
  { auditId:2, performedBy:'manager', action:'APPROVAL_APPROVED', module:'APPROVAL',    entityType:'ACCOUNT',  entityId:'1001000001', description:'Approved: APR2026000010 [HIGH]. Action: FUND_TRANSFER. Maker: teller', status:'SUCCESS', ipAddress:'192.168.1.12', performedAt:new Date(Date.now()-3600000).toISOString() },
  { auditId:3, performedBy:'admin',   action:'USER_CREATE',       module:'USER',        entityType:'USER',     entityId:'kiran.bose', description:'User created: Kiran Bose role=ROLE_TELLER', status:'SUCCESS', ipAddress:'192.168.1.5',  performedAt:new Date(Date.now()-7200000).toISOString() },
  { auditId:4, performedBy:'manager', action:'KYC_STATUS_UPDATE', module:'CUSTOMER',    entityType:'CUSTOMER', entityId:'CNB-CUST-20250004', description:'KYC status updated: UNDER_REVIEW → APPROVED | Remarks: All documents verified', status:'SUCCESS', ipAddress:'192.168.1.12', performedAt:new Date(Date.now()-10800000).toISOString() },
  { auditId:5, performedBy:'teller',  action:'ACCOUNT_FREEZE',    module:'ACCOUNT',     entityType:'ACCOUNT',  entityId:'1001000005', description:'Account frozen. Reason: Suspicious activity flagged', status:'SUCCESS', ipAddress:'192.168.1.45', performedAt:new Date(Date.now()-14400000).toISOString() },
  { auditId:6, performedBy:'teller',  action:'LOGIN',             module:'AUTH',        entityType:'USER',     entityId:'teller', description:'Successful login from branch: MAIN001', status:'SUCCESS', ipAddress:'192.168.1.45', performedAt:new Date(Date.now()-18000000).toISOString() },
  { auditId:7, performedBy:'unknown', action:'LOGIN',             module:'AUTH',        entityType:'USER',     entityId:'unknown', description:'Failed login attempt #3 for user [teller]', status:'FAILURE', ipAddress:'45.33.21.9',   performedAt:new Date(Date.now()-21600000).toISOString() },
  { auditId:8, performedBy:'admin',   action:'USER_ROLE_CHANGE',  module:'USER',        entityType:'USER',     entityId:'kiran.bose', description:'Role changed: ROLE_TELLER → ROLE_MANAGER', status:'SUCCESS', ipAddress:'192.168.1.5',  performedAt:new Date(Date.now()-86400000).toISOString() },
  { auditId:9, performedBy:'teller',  action:'BENEFICIARY_ADD',   module:'ACCOUNT',     entityType:'ACCOUNT',  entityId:'1001000001', description:'Beneficiary added: Sunita Patel (1001000004) type=INTERNAL cooling=24h', status:'SUCCESS', ipAddress:'192.168.1.45', performedAt:new Date(Date.now()-90000000).toISOString() },
  { auditId:10,performedBy:'manager', action:'LOAN_APPROVE',      module:'LOAN',        entityType:'LOAN',     entityId:'LNHL20260001', description:'Loan approved. Remarks: Credit assessment cleared', status:'SUCCESS', ipAddress:'192.168.1.12', performedAt:new Date(Date.now()-172800000).toISOString() },
];

const formatTime = (iso) => new Date(iso).toLocaleString('en-IN', { day:'2-digit', month:'short', year:'numeric', hour:'2-digit', minute:'2-digit', hour12:true });

export default function AuditLogsPage() {
  const { user } = useAuth();
  const canView = ['ROLE_ADMIN','ROLE_AUDITOR'].includes(user?.role);
  const [search,  setSearch]  = useState('');
  const [module,  setModule]  = useState('ALL');
  const [status,  setStatus]  = useState('ALL');

  if (!canView) return (
    <div>
      <div className="page-header"><h1 className="page-title">Audit Logs</h1></div>
      <div className="card" style={{ textAlign:'center', padding:60 }}>
        <div style={{ fontSize:40, marginBottom:12 }}>🔒</div>
        <h3 style={{ fontSize:15, fontWeight:700, marginBottom:6 }}>Access Restricted</h3>
        <p style={{ fontSize:13, color:'#64748B' }}>Audit Logs are visible to <b>ADMIN</b> and <b>AUDITOR</b> roles only — per RBI compliance segregation of duties.</p>
      </div>
    </div>
  );

  const modules = ['ALL', ...new Set(DEMO_LOGS.map(l=>l.module))];
  const filtered = DEMO_LOGS.filter(l =>
    (module==='ALL'||l.module===module) &&
    (status==='ALL'||l.status===status) &&
    (!search.trim() || l.performedBy.toLowerCase().includes(search.toLowerCase()) || l.description.toLowerCase().includes(search.toLowerCase()) || l.entityId.toLowerCase().includes(search.toLowerCase()))
  );

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Audit Logs</h1>
        <p className="page-desc">PDF §10 — Append-only compliance trail. Format: USER [x] ACTION [y] ON [entity] STATUS [z] AT [time]</p>
      </div>

      {/* KPIs */}
      <div className="grid-4 mb-5">
        {[
          {l:'Total Entries (24h)', v:DEMO_LOGS.length,                                   c:'#3B82F6'},
          {l:'Successful',          v:DEMO_LOGS.filter(l=>l.status==='SUCCESS').length,   c:'#10B981'},
          {l:'Failed',              v:DEMO_LOGS.filter(l=>l.status==='FAILURE').length,   c:'#EF4444'},
          {l:'Unique Users',        v:new Set(DEMO_LOGS.map(l=>l.performedBy)).size,      c:'#8B5CF6'},
        ].map((s,i)=>(
          <div key={i} className="card" style={{ padding:'16px 20px' }}>
            <div style={{ fontSize:24,fontWeight:800,color:'#0F172A' }}>{s.v}</div>
            <div style={{ fontSize:12,color:'#64748B',marginTop:4,display:'flex',alignItems:'center',gap:6 }}>
              <span style={{ width:8,height:8,borderRadius:'50%',background:s.c,display:'inline-block' }} />{s.l}
            </div>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="card mb-5">
        <div style={{ display:'flex', gap:12, flexWrap:'wrap', alignItems:'center' }}>
          <div style={{ flex:1, minWidth:200, position:'relative' }}>
            <span style={{ position:'absolute',left:10,top:'50%',transform:'translateY(-50%)',color:'#94A3B8',fontSize:13 }}>🔍</span>
            <input className="form-input" style={{ paddingLeft:30 }} placeholder="Search user, entity, description..." value={search} onChange={e=>setSearch(e.target.value)} />
          </div>
          <select className="form-input form-select" style={{ width:160 }} value={module} onChange={e=>setModule(e.target.value)}>
            {modules.map(m=><option key={m} value={m}>{m==='ALL'?'All Modules':m}</option>)}
          </select>
          <select className="form-input form-select" style={{ width:140 }} value={status} onChange={e=>setStatus(e.target.value)}>
            <option value="ALL">All Status</option><option value="SUCCESS">Success</option><option value="FAILURE">Failure</option>
          </select>
          <button className="btn btn-ghost" onClick={()=>{setSearch('');setModule('ALL');setStatus('ALL');}}>Clear</button>
        </div>
      </div>

      {/* Timeline */}
      <div className="card">
        <h3 className="card-title" style={{ marginBottom:16 }}>Activity Timeline</h3>
        <div style={{ position:'relative' }}>
          {filtered.map((log,i) => {
            const meta = ACTION_META[log.action] || {icon:'📋',color:'#64748B'};
            return (
              <div key={log.auditId} style={{ display:'flex', gap:14, paddingBottom: i<filtered.length-1?16:0, position:'relative' }}>
                {/* Timeline line */}
                {i<filtered.length-1 && <div style={{ position:'absolute', left:17, top:36, bottom:0, width:2, background:'#F1F5F9' }} />}
                <div style={{ width:36,height:36,borderRadius:'50%',background:meta.color+'18',display:'flex',alignItems:'center',justifyContent:'center',fontSize:16,flexShrink:0,zIndex:1 }}>
                  {meta.icon}
                </div>
                <div style={{ flex:1, paddingTop:2 }}>
                  <div style={{ display:'flex',justifyContent:'space-between',alignItems:'flex-start',marginBottom:4 }}>
                    <div style={{ fontSize:13, color:'#1E293B' }}>
                      <b style={{ color:'#1D4ED8' }}>{log.performedBy}</b>
                      {' '}<span style={{ fontWeight:600 }}>{log.action.replace(/_/g,' ')}</span>
                      {' on '}<span className="mono" style={{ background:'#F1F5F9', padding:'1px 6px', borderRadius:4, fontSize:11 }}>{log.entityType}:{log.entityId}</span>
                    </div>
                    <span className={`badge ${log.status==='SUCCESS'?'badge-success':'badge-danger'}`}>{log.status}</span>
                  </div>
                  <p style={{ fontSize:12, color:'#64748B', marginBottom:4 }}>{log.description}</p>
                  <div style={{ fontSize:11, color:'#94A3B8', display:'flex', gap:14 }}>
                    <span>📅 {formatTime(log.performedAt)}</span>
                    <span>🌐 {log.ipAddress}</span>
                    <span>📦 {log.module}</span>
                  </div>
                </div>
              </div>
            );
          })}
          {filtered.length===0 && <div style={{ textAlign:'center', padding:40, color:'#94A3B8', fontSize:13 }}>No audit entries match your filters.</div>}
        </div>
      </div>
    </div>
  );
}
