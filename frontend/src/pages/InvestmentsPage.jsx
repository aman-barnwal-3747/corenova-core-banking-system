/**
 * InvestmentsPage.jsx – Fixed Deposits, Recurring Deposits, Mutual Funds
 * Image 2 sidebar item "Investments" — all user-based values
 */
import React, { useState } from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';

const DEMO_INVESTMENTS = [
  { id:1, type:'FIXED_DEPOSIT', label:'Fixed Deposit', icon:'🏦', principal:500000, currentValue:535000, rate:7.0,  maturityDate:'2027-03-15', tenure:'2 years', color:'#3B82F6' },
  { id:2, type:'RECURRING',     label:'Recurring Deposit', icon:'🔄', principal:60000, currentValue:64200, rate:6.5,  maturityDate:'2026-12-01', tenure:'1 year',  color:'#10B981' },
  { id:3, type:'MUTUAL_FUND',   label:'Mutual Fund - Equity', icon:'📈', principal:200000, currentValue:248500, rate:null, maturityDate:null, tenure:'Open-ended', color:'#8B5CF6' },
  { id:4, type:'FIXED_DEPOSIT', label:'Fixed Deposit', icon:'🏦', principal:1000000, currentValue:1042000, rate:6.8, maturityDate:'2026-09-20', tenure:'1 year', color:'#F59E0B' },
];

const formatINR = (v) => `₹ ${Number(v).toLocaleString('en-IN')}`;

function OpenFDModal({ onClose, onCreate }) {
  const [form,setForm]=useState({ amount:'', tenure:'12', type:'FIXED_DEPOSIT' });
  const rate = form.type==='FIXED_DEPOSIT' ? 7.0 : 6.5;
  const maturity = form.amount ? Number(form.amount) * Math.pow(1+rate/100, Number(form.tenure)/12) : 0;
  return (
    <div style={{ position:'fixed',inset:0,zIndex:1000,background:'rgba(15,23,42,0.5)',display:'flex',alignItems:'center',justifyContent:'center',padding:20 }}>
      <div style={{ background:'#fff',borderRadius:16,width:'100%',maxWidth:440,padding:24 }}>
        <h3 style={{ fontSize:15,fontWeight:700,marginBottom:16 }}>Open New Deposit</h3>
        <div style={{ display:'flex',flexDirection:'column',gap:14 }}>
          <div className="form-group">
            <label className="form-label">Deposit Type</label>
            <select className="form-input form-select" value={form.type} onChange={e=>setForm(f=>({...f,type:e.target.value}))}>
              <option value="FIXED_DEPOSIT">Fixed Deposit (7.0% p.a.)</option>
              <option value="RECURRING">Recurring Deposit (6.5% p.a.)</option>
            </select>
          </div>
          <div className="form-group"><label className="form-label">Amount (₹) *</label><input className="form-input mono" type="number" value={form.amount} onChange={e=>setForm(f=>({...f,amount:e.target.value}))} placeholder="Minimum ₹1,000" /></div>
          <div className="form-group">
            <label className="form-label">Tenure (months)</label>
            <select className="form-input form-select" value={form.tenure} onChange={e=>setForm(f=>({...f,tenure:e.target.value}))}>
              {[6,12,24,36,60].map(m=><option key={m} value={m}>{m} months</option>)}
            </select>
          </div>
          {form.amount && (
            <div style={{ background:'#EFF6FF',border:'1px solid #BFDBFE',borderRadius:10,padding:14,textAlign:'center' }}>
              <div style={{ fontSize:10,color:'#64748B' }}>Maturity Value</div>
              <div style={{ fontSize:20,fontWeight:800,color:'#1D4ED8' }}>{formatINR(maturity.toFixed(0))}</div>
              <div style={{ fontSize:10,color:'#94A3B8',marginTop:2 }}>@ {rate}% p.a. compounded annually</div>
            </div>
          )}
        </div>
        <div style={{ display:'flex',gap:10,marginTop:18 }}>
          <button onClick={onClose} className="btn btn-ghost">Cancel</button>
          <button onClick={()=>{onCreate(form,maturity);onClose();}} disabled={!form.amount} className="btn btn-primary" style={{ flex:1,justifyContent:'center' }}>Open Deposit</button>
        </div>
      </div>
    </div>
  );
}

export default function InvestmentsPage() {
  const [investments, setInvestments] = useState(DEMO_INVESTMENTS);
  const [showModal, setShowModal] = useState(false);
  const [toast, setToast] = useState('');
  const showToast=(m)=>{setToast(m);setTimeout(()=>setToast(''),3000);};

  const totalInvested = investments.reduce((s,i)=>s+i.principal,0);
  const totalCurrent  = investments.reduce((s,i)=>s+i.currentValue,0);
  const totalGain     = totalCurrent - totalInvested;

  const pieData = investments.map(i=>({ name:i.label, value:i.currentValue, color:i.color }));

  const create = (form, maturity) => {
    const icon = form.type==='FIXED_DEPOSIT'?'🏦':'🔄';
    const label = form.type==='FIXED_DEPOSIT'?'Fixed Deposit':'Recurring Deposit';
    const colors=['#3B82F6','#10B981','#8B5CF6','#F59E0B','#EF4444'];
    setInvestments(inv=>[...inv,{ id:Date.now(), type:form.type, label, icon, principal:Number(form.amount), currentValue:Number(form.amount), rate: form.type==='FIXED_DEPOSIT'?7.0:6.5, maturityDate:new Date(Date.now()+Number(form.tenure)*30*86400000).toISOString().slice(0,10), tenure:`${form.tenure} months`, color:colors[inv.length%colors.length] }]);
    showToast(`${label} of ${formatINR(form.amount)} opened successfully!`);
  };

  return (
    <div>
      {toast && <div style={{ position:'fixed',top:80,right:24,zIndex:2000,background:'#0F2342',color:'#fff',padding:'12px 20px',borderRadius:10,fontSize:13,fontWeight:500 }}>✓ {toast}</div>}
      <div className="page-header flex items-center justify-between">
        <div><h1 className="page-title">Investments</h1><p className="page-desc">Fixed deposits, recurring deposits & mutual fund portfolio</p></div>
        <button className="btn btn-primary" onClick={()=>setShowModal(true)}>+ Open New Deposit</button>
      </div>

      <div className="grid-3 mb-5">
        <div className="card" style={{padding:'18px 20px'}}>
          <div style={{fontSize:12,color:'#64748B',marginBottom:4}}>Total Invested</div>
          <div style={{fontSize:24,fontWeight:800,color:'#0F172A'}}>{formatINR(totalInvested)}</div>
        </div>
        <div className="card" style={{padding:'18px 20px'}}>
          <div style={{fontSize:12,color:'#64748B',marginBottom:4}}>Current Value</div>
          <div style={{fontSize:24,fontWeight:800,color:'#0F172A'}}>{formatINR(totalCurrent)}</div>
        </div>
        <div className="card" style={{padding:'18px 20px'}}>
          <div style={{fontSize:12,color:'#64748B',marginBottom:4}}>Total Gain</div>
          <div style={{fontSize:24,fontWeight:800,color:'#059669'}}>▲ {formatINR(totalGain)} <span style={{fontSize:12}}>({((totalGain/totalInvested)*100).toFixed(1)}%)</span></div>
        </div>
      </div>

      <div style={{ display:'grid',gridTemplateColumns:'1fr 300px',gap:16 }}>
        <div style={{ display:'flex',flexDirection:'column',gap:12 }}>
          {investments.map(inv=>{
            const gain = inv.currentValue - inv.principal;
            const gainPct = ((gain/inv.principal)*100).toFixed(1);
            return (
              <div key={inv.id} className="card">
                <div style={{ display:'flex',justifyContent:'space-between',alignItems:'flex-start' }}>
                  <div style={{ display:'flex',gap:12,alignItems:'center' }}>
                    <div style={{ width:44,height:44,borderRadius:10,background:inv.color+'18',display:'flex',alignItems:'center',justifyContent:'center',fontSize:20 }}>{inv.icon}</div>
                    <div>
                      <div style={{ fontWeight:700,fontSize:14 }}>{inv.label}</div>
                      <div style={{ fontSize:11,color:'#64748B' }}>
                        {inv.rate ? `${inv.rate}% p.a. · ` : ''}{inv.tenure}
                        {inv.maturityDate && ` · Matures ${new Date(inv.maturityDate).toLocaleDateString('en-IN',{day:'2-digit',month:'short',year:'numeric'})}`}
                      </div>
                    </div>
                  </div>
                  <div style={{ textAlign:'right' }}>
                    <div style={{ fontSize:18,fontWeight:800,color:'#0F172A' }}>{formatINR(inv.currentValue)}</div>
                    <div style={{ fontSize:11,fontWeight:600,color: gain>=0?'#059669':'#DC2626' }}>
                      {gain>=0?'▲':'▼'} {formatINR(Math.abs(gain))} ({gainPct}%)
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        <div className="card">
          <h3 className="card-title" style={{ marginBottom:14 }}>Portfolio Mix</h3>
          <ResponsiveContainer width="100%" height={180}>
            <PieChart>
              <Pie data={pieData} cx="50%" cy="50%" innerRadius={45} outerRadius={70} paddingAngle={2} dataKey="value">
                {pieData.map((e,i)=><Cell key={i} fill={e.color} />)}
              </Pie>
              <Tooltip formatter={v=>formatINR(v)} />
            </PieChart>
          </ResponsiveContainer>
          <div style={{ display:'flex',flexDirection:'column',gap:8,marginTop:8 }}>
            {pieData.map((e,i)=>(
              <div key={i} style={{ display:'flex',alignItems:'center',gap:8,fontSize:11 }}>
                <div style={{ width:10,height:10,borderRadius:2,background:e.color }} />
                <span style={{ color:'#475569',flex:1 }}>{e.name}</span>
                <span style={{ fontWeight:600 }}>{((e.value/totalCurrent)*100).toFixed(0)}%</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {showModal && <OpenFDModal onClose={()=>setShowModal(false)} onCreate={create} />}
    </div>
  );
}
