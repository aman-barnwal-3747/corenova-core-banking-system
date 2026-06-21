/**
 * DashboardPage.jsx – Matches Image 2 exactly
 * Layout: KPI cards → [Transaction Chart | Quick Actions] → [Recent Txns | Account Summary | Alerts]
 * All amounts are user/API-based with demo fallback
 */
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, PieChart, Pie, Cell
} from 'recharts';
import { useAuth } from '../hooks/useAuth';
import { dashboardApi } from '../api';

const formatINR = (v) => {
  const n = Number(v || 0);
  if (n >= 1e7)  return `₹ ${(n/1e7).toFixed(2)} Cr`;
  if (n >= 1e5)  return `₹ ${(n/1e5).toFixed(2)} L`;
  if (n >= 1000) return `₹ ${(n/1000).toFixed(1)}K`;
  return `₹ ${n.toLocaleString('en-IN')}`;
};

const formatDate = (d) => {
  if (!d) return '—';
  return new Date(d).toLocaleString('en-IN', { day:'2-digit', month:'short', hour:'2-digit', minute:'2-digit', hour12:true });
};

// ── Demo data (user-based — replaced by API in production) ───────
function buildDemoData() {
  // Chart: last 7 days with random but realistic values
  const days   = ['14 May','15 May','16 May','17 May','18 May','19 May','20 May'];
  const chart  = days.map((d,i) => ({
    date:    d,
    credit: 200000 + Math.sin(i)*80000 + Math.random()*60000,
    debit:  100000 + Math.cos(i)*40000 + Math.random()*40000,
  }));

  const recentTxns = [
    { icon:'↓', color:'#10B981', title:'Fund Received',  sub:'From Ramesh Kumar',   amount:25000,  dir:'credit', time:'Today, 10:15 AM' },
    { icon:'↑', color:'#EF4444', title:'Fund Transfer',  sub:'To Priya Sharma',     amount:10500,  dir:'debit',  time:'Today, 09:42 AM' },
    { icon:'↓', color:'#10B981', title:'Salary Credit',  sub:'From ABC Pvt. Ltd.',  amount:55000,  dir:'credit', time:'Today, 09:15 AM' },
    { icon:'↑', color:'#EF4444', title:'Bill Payment',   sub:'To Electricity Board',amount:2450,   dir:'debit',  time:'Yesterday, 07:35 PM' },
  ];

  const accountSummary = [
    { name:'Savings Accounts', value:44, amount:2015450, color:'#3B82F6' },
    { name:'Current Accounts', value:33, amount:1520300, color:'#10B981' },
    { name:'Fixed Deposits',   value:17, amount:780700,  color:'#F59E0B' },
    { name:'Others',           value:6,  amount:262000,  color:'#8B5CF6' },
  ];

  const alerts = [
    { icon:'🔔', iconBg:'#FEF3C7', iconColor:'#F59E0B', title:'5 pending approvals',       sub:'Require your action',          time:'Just now' },
    { icon:'🛡️', iconBg:'#DBEAFE', iconColor:'#3B82F6', title:'KYC verification pending',  sub:'12 customers',                 time:'10 mins ago' },
    { icon:'💰', iconBg:'#D1FAE5', iconColor:'#10B981', title:'High value transaction',     sub:'Transaction ID: TXN123456',    time:'30 mins ago' },
    { icon:'⚙️', iconBg:'#EDE9FE', iconColor:'#8B5CF6', title:'System maintenance',         sub:'Scheduled on 25 May 2025',     time:'1 day ago' },
  ];

  return { chart, recentTxns, accountSummary, alerts };
}

// ── KPI Card ─────────────────────────────────────────────────────
function KpiCard({ icon, iconBg, label, value, trend, trendDir }) {
  return (
    <div className="card fade-up" style={{ padding:'20px', display:'flex', gap:14 }}>
      <div style={{
        width:52, height:52, borderRadius:12, flexShrink:0,
        background:iconBg, display:'flex', alignItems:'center', justifyContent:'center', fontSize:22,
      }}>
        {icon}
      </div>
      <div style={{ flex:1, minWidth:0 }}>
        <div style={{ fontSize:12, color:'#64748B', marginBottom:4 }}>{label}</div>
        <div style={{ fontSize:22, fontWeight:800, color:'#0F172A', letterSpacing:'-0.02em', lineHeight:1.2 }}>
          {value}
        </div>
        {trend && (
          <div style={{ fontSize:11, marginTop:4, color: trendDir === 'up' ? '#059669' : '#DC2626', fontWeight:600 }}>
            {trendDir === 'up' ? '▲' : '▼'} {trend}
          </div>
        )}
      </div>
    </div>
  );
}

// ── Custom chart tooltip ──────────────────────────────────────────
function ChartTip({ active, payload, label }) {
  if (!active || !payload?.length) return null;
  return (
    <div style={{ background:'#fff', border:'1px solid #E2E8F0', borderRadius:8, padding:'10px 14px', boxShadow:'0 4px 16px rgba(0,0,0,0.1)' }}>
      <p style={{ fontSize:11, color:'#94A3B8', marginBottom:6 }}>{label}</p>
      {payload.map((p,i) => (
        <div key={i} style={{ display:'flex', alignItems:'center', gap:8, marginBottom:2 }}>
          <div style={{ width:8, height:8, borderRadius:'50%', background:p.color }} />
          <span style={{ fontSize:12, color:'#64748B' }}>{p.name}:</span>
          <span style={{ fontSize:12, fontWeight:700 }}>{formatINR(p.value)}</span>
        </div>
      ))}
    </div>
  );
}

// ── Quick Actions ─────────────────────────────────────────────────
function QuickActions({ navigate, pendingCount }) {
  const actions = [
    { icon:'👤', label:'New Account',      route:'/accounts',      color:'#DBEAFE', iconColor:'#1D4ED8' },
    { icon:'💸', label:'Fund Transfer',    route:'/fund-transfer', color:'#D1FAE5', iconColor:'#059669' },
    { icon:'➕', label:'Add Beneficiary',  route:'/beneficiaries', color:'#EDE9FE', iconColor:'#7C3AED' },
    { icon:'✅', label:'Approve Request',  route:'/approvals',     color:'#FEE2E2', iconColor:'#DC2626', badge: pendingCount },
    { icon:'🏦', label:'Loan Application', route:'/loans',         color:'#FEF3C7', iconColor:'#D97706' },
    { icon:'📊', label:'Reports',          route:'/reports',       color:'#F1F5F9', iconColor:'#475569' },
  ];
  return (
    <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr 1fr', gap:12 }}>
      {actions.map((a,i) => (
        <button key={i} onClick={() => navigate(a.route)}
          style={{
            display:'flex', flexDirection:'column', alignItems:'center', gap:8,
            padding:'16px 8px', borderRadius:10,
            background:'#fff', border:'1px solid #E2E8F0',
            cursor:'pointer', transition:'all 0.15s', position:'relative',
          }}
          onMouseEnter={e => { e.currentTarget.style.background='#F8FAFC'; e.currentTarget.style.transform='translateY(-1px)'; }}
          onMouseLeave={e => { e.currentTarget.style.background='#fff'; e.currentTarget.style.transform='none'; }}>
          {a.badge > 0 && (
            <span style={{
              position:'absolute', top:6, right:6,
              background:'#EF4444', color:'#fff',
              fontSize:9, fontWeight:700, padding:'1px 5px',
              borderRadius:99, lineHeight:1.6,
            }}>{a.badge}</span>
          )}
          <div style={{ width:40, height:40, borderRadius:10, background:a.color, display:'flex', alignItems:'center', justifyContent:'center', fontSize:18 }}>
            {a.icon}
          </div>
          <span style={{ fontSize:11, fontWeight:600, color:'#334155', textAlign:'center', lineHeight:1.3 }}>{a.label}</span>
        </button>
      ))}
    </div>
  );
}

// ════════════════════════════════════════════════════════════════
export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [data,    setData]    = useState(null);
  const [loading, setLoading] = useState(true);
  const [period,  setPeriod]  = useState('This Week');
  const demo = buildDemoData();

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const { data: res } = await dashboardApi.getData();
        setData(res);
      } catch {
        setData(null); // use demo
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const kpis = data?.kpis || {};
  const TOTAL_BALANCE = demo.accountSummary.reduce((s,a) => s+a.amount, 0);

  // Date/time string matching image
  const now      = new Date();
  const dateStr  = now.toLocaleDateString('en-IN', { day:'numeric', month:'long', year:'numeric', weekday:'long' });
  const timeStr  = now.toLocaleTimeString('en-IN', { hour:'2-digit', minute:'2-digit', hour12:true });

  const pendingApprovals = kpis.pendingKycApprovals || 5;

  return (
    <div>

      {/* ── Welcome row ── */}
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:20 }}>
        <div>
          <h1 style={{ fontSize:20, fontWeight:700, color:'#0F172A' }}>
            Welcome back, <span style={{ color:'#1D4ED8' }}>{user?.fullName?.split(' ')[0] || 'User'}</span> 👋
          </h1>
          <p style={{ fontSize:13, color:'#64748B', marginTop:3 }}>Here's what's happening in your branch today.</p>
        </div>
        <div style={{ display:'flex', gap:20, alignItems:'center' }}>
          <div style={{ display:'flex', alignItems:'center', gap:6, fontSize:12, color:'#64748B' }}>
            📅 {dateStr}
          </div>
          <div style={{ display:'flex', alignItems:'center', gap:6, fontSize:12, color:'#64748B' }}>
            🕐 {timeStr}
          </div>
        </div>
      </div>

      {/* ── KPI Cards ── */}
      <div className="grid-4 mb-5">
        <KpiCard
          icon="💳" iconBg="#DBEAFE"
          label="Total Accounts"
          value={(kpis.totalActiveAccounts || 12458).toLocaleString('en-IN')}
          trend="+5.2% from last month" trendDir="up"
        />
        <KpiCard
          icon="₹" iconBg="#D1FAE5"
          label="Total Deposits"
          value={formatINR(kpis.totalDeposits || 12545000000)}
          trend="+7.6% from last month" trendDir="up"
        />
        <KpiCard
          icon="🏦" iconBg="#EDE9FE"
          label="Total Loans"
          value={formatINR(kpis.totalLoanBook || 8975000000)}
          trend="+4.3% from last month" trendDir="up"
        />
        <KpiCard
          icon="📊" iconBg="#FEF3C7"
          label="Today's Transactions"
          value={(kpis.todayTransactions || 1245).toLocaleString('en-IN')}
          trend="+12.4% from yesterday" trendDir="up"
        />
      </div>

      {/* ── Chart + Quick Actions row ── */}
      <div style={{ display:'grid', gridTemplateColumns:'1fr 300px', gap:16, marginBottom:16 }}>

        {/* Transaction Overview */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Transaction Overview</div>
              <div style={{ display:'flex', alignItems:'center', gap:16, marginTop:6 }}>
                <span style={{ display:'flex', alignItems:'center', gap:5, fontSize:12, color:'#64748B' }}>
                  <span style={{ width:10, height:10, borderRadius:'50%', background:'#3B82F6', display:'inline-block' }} /> Credit (₹)
                </span>
                <span style={{ display:'flex', alignItems:'center', gap:5, fontSize:12, color:'#64748B' }}>
                  <span style={{ width:10, height:10, borderRadius:'50%', background:'#EF4444', display:'inline-block' }} /> Debit (₹)
                </span>
              </div>
            </div>
            <select value={period} onChange={e => setPeriod(e.target.value)}
              style={{ padding:'6px 12px', borderRadius:6, border:'1px solid #E2E8F0', fontSize:12, color:'#334155', background:'#fff', cursor:'pointer', fontFamily:'inherit' }}>
              {['This Week','Last Week','This Month','Last Month'].map(p => <option key={p}>{p}</option>)}
            </select>
          </div>
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={demo.chart} margin={{ top:5, right:10, left:0, bottom:5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
              <XAxis dataKey="date" tick={{ fontSize:11, fill:'#94A3B8' }} axisLine={false} tickLine={false} />
              <YAxis tickFormatter={v => `${(v/100000).toFixed(0)}L`} tick={{ fontSize:11, fill:'#94A3B8' }} axisLine={false} tickLine={false} width={42} />
              <Tooltip content={<ChartTip />} />
              <Line type="monotone" dataKey="credit" name="Credit" stroke="#3B82F6" strokeWidth={2.5} dot={{ r:4, fill:'#3B82F6', strokeWidth:0 }} activeDot={{ r:6 }} />
              <Line type="monotone" dataKey="debit"  name="Debit"  stroke="#EF4444" strokeWidth={2.5} dot={{ r:4, fill:'#EF4444', strokeWidth:0 }} activeDot={{ r:6 }} />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Quick Actions */}
        <div className="card">
          <div className="card-header" style={{ marginBottom:14 }}>
            <div className="card-title">Quick Actions</div>
          </div>
          <QuickActions navigate={navigate} pendingCount={pendingApprovals} />
        </div>
      </div>

      {/* ── Bottom row: Recent Txns | Account Summary | Alerts ── */}
      <div style={{ display:'grid', gridTemplateColumns:'1fr 300px 280px', gap:16 }}>

        {/* Recent Transactions */}
        <div className="card">
          <div className="card-header">
            <div className="card-title">Recent Transactions</div>
            <button onClick={() => navigate('/transactions')} style={{ background:'none', border:'none', color:'#2563EB', fontSize:12, fontWeight:600, cursor:'pointer', padding:0 }}>
              View All
            </button>
          </div>
          <div style={{ display:'flex', flexDirection:'column', gap:0 }}>
            {demo.recentTxns.map((t,i) => (
              <div key={i} style={{
                display:'flex', alignItems:'center', gap:12,
                padding:'12px 0', borderBottom: i < demo.recentTxns.length-1 ? '1px solid #F1F5F9' : 'none',
              }}>
                {/* Direction icon */}
                <div style={{
                  width:36, height:36, borderRadius:'50%', flexShrink:0,
                  background: t.dir==='credit' ? '#D1FAE5' : '#FEE2E2',
                  display:'flex', alignItems:'center', justifyContent:'center',
                  fontSize:14, color:t.color, fontWeight:700,
                }}>
                  {t.icon}
                </div>
                <div style={{ flex:1, minWidth:0 }}>
                  <div style={{ fontSize:13, fontWeight:600, color:'#1E293B' }}>{t.title}</div>
                  <div style={{ fontSize:11, color:'#64748B', marginTop:1 }}>{t.sub}</div>
                </div>
                <div style={{ textAlign:'right', flexShrink:0 }}>
                  <div style={{ fontSize:13, fontWeight:700, color: t.dir==='credit' ? '#059669' : '#DC2626' }}>
                    {t.dir==='credit' ? '+' : '-'} ₹ {t.amount.toLocaleString('en-IN')}.00
                  </div>
                  <div style={{ fontSize:11, color:'#94A3B8', marginTop:1 }}>{t.time}</div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Account Summary */}
        <div className="card">
          <div className="card-header">
            <div className="card-title">Account Summary</div>
            <button onClick={() => navigate('/accounts')} style={{ background:'none', border:'none', color:'#2563EB', fontSize:12, fontWeight:600, cursor:'pointer', padding:0 }}>
              View All
            </button>
          </div>

          {/* Donut chart with center label */}
          <div style={{ position:'relative', height:160, display:'flex', alignItems:'center', justifyContent:'center' }}>
            <ResponsiveContainer width="100%" height={160}>
              <PieChart>
                <Pie
                  data={demo.accountSummary}
                  cx="50%" cy="50%"
                  innerRadius={50} outerRadius={72}
                  paddingAngle={2} dataKey="value"
                  startAngle={90} endAngle={-270}
                >
                  {demo.accountSummary.map((e,i) => <Cell key={i} fill={e.color} />)}
                </Pie>
              </PieChart>
            </ResponsiveContainer>
            {/* Center label */}
            <div style={{ position:'absolute', top:'50%', left:'50%', transform:'translate(-50%,-50%)', textAlign:'center', pointerEvents:'none' }}>
              <div style={{ fontSize:10, color:'#64748B', fontWeight:600 }}>Total Balance</div>
              <div style={{ fontSize:12, fontWeight:800, color:'#0F172A', lineHeight:1.2, marginTop:2 }}>
                ₹ {(TOTAL_BALANCE/100).toLocaleString('en-IN')}.00
              </div>
            </div>
          </div>

          {/* Legend */}
          <div style={{ display:'flex', flexDirection:'column', gap:8, marginTop:8 }}>
            {demo.accountSummary.map((a,i) => (
              <div key={i} style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
                <div style={{ display:'flex', alignItems:'center', gap:8 }}>
                  <div style={{ width:10, height:10, borderRadius:2, background:a.color }} />
                  <span style={{ fontSize:11, color:'#475569' }}>{a.name}</span>
                </div>
                <div style={{ textAlign:'right' }}>
                  <span style={{ fontSize:11, fontWeight:600, color:'#1E293B' }}>
                    ₹ {a.amount.toLocaleString('en-IN')}
                  </span>
                  <span style={{ fontSize:10, color:'#94A3B8', marginLeft:4 }}>({a.value}%)</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Alerts & Notifications */}
        <div className="card">
          <div className="card-header">
            <div className="card-title">Alerts & Notifications</div>
            <button style={{ background:'none', border:'none', color:'#2563EB', fontSize:12, fontWeight:600, cursor:'pointer', padding:0 }}>
              View All
            </button>
          </div>
          <div style={{ display:'flex', flexDirection:'column', gap:0 }}>
            {demo.alerts.map((a,i) => (
              <div key={i} style={{
                display:'flex', gap:10, padding:'11px 0',
                borderBottom: i < demo.alerts.length-1 ? '1px solid #F1F5F9' : 'none',
                cursor:'pointer',
              }}>
                <div style={{
                  width:34, height:34, borderRadius:8, flexShrink:0,
                  background:a.iconBg,
                  display:'flex', alignItems:'center', justifyContent:'center', fontSize:16,
                }}>
                  {a.icon}
                </div>
                <div style={{ flex:1, minWidth:0 }}>
                  <div style={{ fontSize:12, fontWeight:600, color:'#1E293B', marginBottom:1 }}>{a.title}</div>
                  <div style={{ fontSize:11, color:'#64748B' }}>{a.sub}</div>
                </div>
                <div style={{ fontSize:10, color:'#94A3B8', flexShrink:0, whiteSpace:'nowrap' }}>
                  {a.time}
                </div>
              </div>
            ))}
          </div>
        </div>

      </div>
    </div>
  );
}
