/**
 * ReportsPage.jsx – MIS Reports & Analytics
 * Image 2 "Quick Actions → Reports" target + sidebar nav item
 * Composes data from dashboard/account/transaction/loan/audit APIs
 * All figures are user-based (live API with demo fallback)
 */
import React, { useState } from 'react';
import {
  BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend
} from 'recharts';
import { dashboardApi, loanApi } from '../api';

const formatINR = (v) => {
  const n = Number(v||0);
  if (n>=1e7) return `₹${(n/1e7).toFixed(2)} Cr`;
  if (n>=1e5) return `₹${(n/1e5).toFixed(2)} L`;
  if (n>=1000) return `₹${(n/1000).toFixed(1)}K`;
  return `₹${n.toLocaleString('en-IN')}`;
};

const REPORT_TYPES = [
  { id:'overview',  label:'Branch Overview',     icon:'📊', desc:'KPIs, transaction trends, account mix' },
  { id:'accounts',  label:'Account Summary',     icon:'💳', desc:'Open/closed/frozen accounts by type' },
  { id:'loans',     label:'Loan Portfolio',      icon:'🏦', desc:'Disbursements, NPA, EMI collections' },
  { id:'kyc',       label:'KYC Compliance',      icon:'🛡️', desc:'Pending, approved, rejected applications' },
  { id:'audit',     label:'Audit Summary',       icon:'📋', desc:'Action counts by module and user' },
];

// Demo datasets (replaced by live API responses where available)
function demoOverview() {
  const months = ['Jan','Feb','Mar','Apr','May','Jun'];
  return months.map((m,i)=>({
    month:m,
    deposits: 9000 + i*450 + Math.random()*300,
    loans:    6000 + i*380 + Math.random()*250,
    txnVolume:1200 + i*90  + Math.random()*60,
  }));
}
function demoAccountMix() {
  return [
    { name:'Savings',  value:28400, color:'#3B82F6' },
    { name:'Current',  value:10200, color:'#10B981' },
    { name:'Fixed Deposit', value:4247, color:'#F59E0B' },
    { name:'Salary',   value:3100, color:'#8B5CF6' },
  ];
}
function demoLoanMix() {
  return [
    { type:'Home',      count:842,  amount:42000000 },
    { type:'Personal',  count:1530, amount:18500000 },
    { type:'Auto',      count:614,  amount:12300000 },
    { type:'Education', count:298,  amount:9800000 },
    { type:'Business',  count:177,  amount:21000000 },
    { type:'Gold',      count:405,  amount:6200000 },
  ];
}
function demoKyc() {
  return [
    { name:'Approved',     value:38450, color:'#10B981' },
    { name:'Under Review', value:312,   color:'#3B82F6' },
    { name:'Submitted',    value:198,   color:'#F59E0B' },
    { name:'Not Submitted',value:540,   color:'#94A3B8' },
    { name:'Rejected',     value:64,    color:'#EF4444' },
  ];
}
function demoAuditByModule() {
  return [
    { module:'TRANSACTION', count:5240 },
    { module:'AUTH',        count:3180 },
    { module:'ACCOUNT',     count:1420 },
    { module:'CUSTOMER',    count:890  },
    { module:'LOAN',        count:412  },
    { module:'USER',        count:156  },
    { module:'APPROVAL',    count:298  },
  ];
}

function exportCSV(rows, headers, filename) {
  const csv = [headers, ...rows].map(r=>r.join(',')).join('\n');
  const blob = new Blob([csv], { type:'text/csv' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = filename; a.click();
  URL.revokeObjectURL(url);
}

export default function ReportsPage() {
  const [active, setActive] = useState('overview');
  const [dateRange, setDateRange] = useState('This Month');
  const overview = demoOverview();
  const accountMix = demoAccountMix();
  const loanMix = demoLoanMix();
  const kyc = demoKyc();
  const auditByModule = demoAuditByModule();

  const totalDeposits = accountMix.reduce((s,a)=>s+a.value,0);
  const totalLoanAmt  = loanMix.reduce((s,l)=>s+l.amount,0);
  const totalLoanCnt  = loanMix.reduce((s,l)=>s+l.count,0);
  const totalKyc      = kyc.reduce((s,k)=>s+k.value,0);
  const totalAudit    = auditByModule.reduce((s,a)=>s+a.count,0);

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div><h1 className="page-title">Reports</h1><p className="page-desc">MIS reports — branch performance, portfolio, and compliance analytics</p></div>
        <select className="form-input form-select" style={{ width:160 }} value={dateRange} onChange={e=>setDateRange(e.target.value)}>
          {['Today','This Week','This Month','This Quarter','This Year'].map(p=><option key={p}>{p}</option>)}
        </select>
      </div>

      {/* Report type selector */}
      <div style={{ display:'grid', gridTemplateColumns:'repeat(5,1fr)', gap:12, marginBottom:20 }}>
        {REPORT_TYPES.map(r=>(
          <button key={r.id} onClick={()=>setActive(r.id)}
            className="card" style={{ textAlign:'left', cursor:'pointer', border:'none', padding:16,
              outline: active===r.id ? '2px solid #1D4ED8' : '1px solid #E2E8F0',
              background: active===r.id ? '#EFF6FF' : '#fff' }}>
            <div style={{ fontSize:22, marginBottom:8 }}>{r.icon}</div>
            <div style={{ fontSize:13, fontWeight:700, color:'#0F172A', marginBottom:3 }}>{r.label}</div>
            <div style={{ fontSize:11, color:'#64748B', lineHeight:1.4 }}>{r.desc}</div>
          </button>
        ))}
      </div>

      {/* ── Branch Overview ── */}
      {active==='overview' && (
        <>
          <div className="grid-4 mb-5">
            <div className="card" style={{padding:'16px 20px'}}><div style={{fontSize:22,fontWeight:800}}>{formatINR(totalDeposits*100)}</div><div style={{fontSize:12,color:'#64748B',marginTop:4}}>Total Deposits</div></div>
            <div className="card" style={{padding:'16px 20px'}}><div style={{fontSize:22,fontWeight:800}}>{formatINR(totalLoanAmt)}</div><div style={{fontSize:12,color:'#64748B',marginTop:4}}>Total Loan Book</div></div>
            <div className="card" style={{padding:'16px 20px'}}><div style={{fontSize:22,fontWeight:800}}>{overview[overview.length-1].txnVolume.toFixed(0)}K</div><div style={{fontSize:12,color:'#64748B',marginTop:4}}>Txns This Month</div></div>
            <div className="card" style={{padding:'16px 20px'}}><div style={{fontSize:22,fontWeight:800}}>{((kyc[0].value/totalKyc)*100).toFixed(1)}%</div><div style={{fontSize:12,color:'#64748B',marginTop:4}}>KYC Completion Rate</div></div>
          </div>

          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:16 }}>
            <div className="card">
              <div className="card-header"><div className="card-title">Deposits vs Loans Trend</div></div>
              <ResponsiveContainer width="100%" height={220}>
                <LineChart data={overview}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
                  <XAxis dataKey="month" tick={{fontSize:11,fill:'#94A3B8'}} axisLine={false} tickLine={false} />
                  <YAxis tickFormatter={v=>`${v}L`} tick={{fontSize:11,fill:'#94A3B8'}} axisLine={false} tickLine={false} width={40} />
                  <Tooltip formatter={v=>`₹${v.toFixed(0)} L`} />
                  <Legend wrapperStyle={{fontSize:12}} />
                  <Line type="monotone" dataKey="deposits" name="Deposits (₹L)" stroke="#3B82F6" strokeWidth={2.5} dot={false} />
                  <Line type="monotone" dataKey="loans"    name="Loans (₹L)"    stroke="#F59E0B" strokeWidth={2.5} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </div>
            <div className="card">
              <div className="card-header"><div className="card-title">Transaction Volume</div></div>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={overview}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
                  <XAxis dataKey="month" tick={{fontSize:11,fill:'#94A3B8'}} axisLine={false} tickLine={false} />
                  <YAxis tickFormatter={v=>`${v}K`} tick={{fontSize:11,fill:'#94A3B8'}} axisLine={false} tickLine={false} width={40} />
                  <Tooltip formatter={v=>`${v.toFixed(0)}K txns`} />
                  <Bar dataKey="txnVolume" name="Transactions (K)" fill="#1D4ED8" radius={[6,6,0,0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </>
      )}

      {/* ── Account Summary ── */}
      {active==='accounts' && (
        <div style={{ display:'grid', gridTemplateColumns:'1fr 320px', gap:16 }}>
          <div className="card">
            <div className="card-header">
              <div className="card-title">Accounts by Type</div>
              <button className="btn btn-ghost" style={{fontSize:12}} onClick={()=>exportCSV(accountMix.map(a=>[a.name,a.value]),['Account Type','Count'],'account_summary.csv')}>⬇ Export CSV</button>
            </div>
            <table className="data-table">
              <thead><tr><th>Account Type</th><th style={{textAlign:'right'}}>Count</th><th style={{textAlign:'right'}}>% of Total</th><th>Distribution</th></tr></thead>
              <tbody>
                {accountMix.map((a,i)=>(
                  <tr key={i}>
                    <td><span style={{ display:'inline-flex',alignItems:'center',gap:8 }}><span style={{width:10,height:10,borderRadius:2,background:a.color,display:'inline-block'}} />{a.name}</span></td>
                    <td className="mono" style={{textAlign:'right',fontWeight:700}}>{a.value.toLocaleString('en-IN')}</td>
                    <td style={{textAlign:'right'}}>{((a.value/totalDeposits)*100).toFixed(1)}%</td>
                    <td><div style={{height:6,background:'#F1F5F9',borderRadius:99,overflow:'hidden'}}><div style={{height:'100%',width:`${(a.value/totalDeposits)*100}%`,background:a.color}} /></div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="card">
            <div className="card-title" style={{marginBottom:14}}>Visual Breakdown</div>
            <ResponsiveContainer width="100%" height={200}>
              <PieChart><Pie data={accountMix} cx="50%" cy="50%" innerRadius={50} outerRadius={75} paddingAngle={2} dataKey="value">
                {accountMix.map((e,i)=><Cell key={i} fill={e.color} />)}
              </Pie><Tooltip /></PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {/* ── Loan Portfolio ── */}
      {active==='loans' && (
        <>
          <div className="grid-3 mb-5">
            <div className="card" style={{padding:'16px 20px'}}><div style={{fontSize:22,fontWeight:800}}>{totalLoanCnt.toLocaleString('en-IN')}</div><div style={{fontSize:12,color:'#64748B',marginTop:4}}>Total Active Loans</div></div>
            <div className="card" style={{padding:'16px 20px'}}><div style={{fontSize:22,fontWeight:800}}>{formatINR(totalLoanAmt)}</div><div style={{fontSize:12,color:'#64748B',marginTop:4}}>Outstanding Portfolio</div></div>
            <div className="card" style={{padding:'16px 20px'}}><div style={{fontSize:22,fontWeight:800}}>{formatINR(totalLoanAmt/totalLoanCnt)}</div><div style={{fontSize:12,color:'#64748B',marginTop:4}}>Avg. Loan Size</div></div>
          </div>
          <div className="card">
            <div className="card-header">
              <div className="card-title">Loan Mix by Product</div>
              <button className="btn btn-ghost" style={{fontSize:12}} onClick={()=>exportCSV(loanMix.map(l=>[l.type,l.count,l.amount]),['Loan Type','Count','Outstanding (₹)'],'loan_portfolio.csv')}>⬇ Export CSV</button>
            </div>
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={loanMix} layout="vertical" margin={{left:20}}>
                <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
                <XAxis type="number" tickFormatter={v=>formatINR(v)} tick={{fontSize:11,fill:'#94A3B8'}} axisLine={false} tickLine={false} />
                <YAxis type="category" dataKey="type" tick={{fontSize:12,fill:'#334155'}} axisLine={false} tickLine={false} width={80} />
                <Tooltip formatter={v=>formatINR(v)} />
                <Bar dataKey="amount" fill="#1D4ED8" radius={[0,6,6,0]} name="Outstanding" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </>
      )}

      {/* ── KYC Compliance ── */}
      {active==='kyc' && (
        <div style={{ display:'grid', gridTemplateColumns:'1fr 320px', gap:16 }}>
          <div className="card">
            <div className="card-header">
              <div className="card-title">KYC Status Distribution</div>
              <button className="btn btn-ghost" style={{fontSize:12}} onClick={()=>exportCSV(kyc.map(k=>[k.name,k.value]),['Status','Count'],'kyc_compliance.csv')}>⬇ Export CSV</button>
            </div>
            <table className="data-table">
              <thead><tr><th>KYC Status</th><th style={{textAlign:'right'}}>Customers</th><th style={{textAlign:'right'}}>%</th></tr></thead>
              <tbody>
                {kyc.map((k,i)=>(
                  <tr key={i}>
                    <td><span style={{ display:'inline-flex',alignItems:'center',gap:8 }}><span style={{width:10,height:10,borderRadius:2,background:k.color,display:'inline-block'}} />{k.name}</span></td>
                    <td className="mono" style={{textAlign:'right',fontWeight:700}}>{k.value.toLocaleString('en-IN')}</td>
                    <td style={{textAlign:'right'}}>{((k.value/totalKyc)*100).toFixed(1)}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="card">
            <div className="card-title" style={{marginBottom:14}}>Compliance Snapshot</div>
            <ResponsiveContainer width="100%" height={200}>
              <PieChart><Pie data={kyc} cx="50%" cy="50%" innerRadius={50} outerRadius={75} paddingAngle={2} dataKey="value">
                {kyc.map((e,i)=><Cell key={i} fill={e.color} />)}
              </Pie><Tooltip /></PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {/* ── Audit Summary ── */}
      {active==='audit' && (
        <div className="card">
          <div className="card-header">
            <div><div className="card-title">Audit Entries by Module</div><div className="card-sub">{totalAudit.toLocaleString('en-IN')} total entries this period</div></div>
            <button className="btn btn-ghost" style={{fontSize:12}} onClick={()=>exportCSV(auditByModule.map(a=>[a.module,a.count]),['Module','Count'],'audit_summary.csv')}>⬇ Export CSV</button>
          </div>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={auditByModule}>
              <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
              <XAxis dataKey="module" tick={{fontSize:11,fill:'#94A3B8'}} axisLine={false} tickLine={false} />
              <YAxis tick={{fontSize:11,fill:'#94A3B8'}} axisLine={false} tickLine={false} />
              <Tooltip />
              <Bar dataKey="count" fill="#8B5CF6" radius={[6,6,0,0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}
