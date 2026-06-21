/**
 * SettingsPage.jsx – User & Bank Configuration
 * Image 2 sidebar item "Settings"
 * Tabs: Profile | Security | Notifications | Bank Config (Admin only)
 */
import React, { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { authApi } from '../api';

function Toggle({ checked, onChange }) {
  return (
    <button onClick={()=>onChange(!checked)} style={{
      width:44, height:24, borderRadius:99, border:'none', cursor:'pointer',
      background: checked ? '#1D4ED8' : '#E2E8F0', position:'relative', transition:'background 0.15s',
    }}>
      <span style={{
        position:'absolute', top:2, left: checked?22:2, width:20, height:20,
        borderRadius:'50%', background:'#fff', transition:'left 0.15s', boxShadow:'0 1px 3px rgba(0,0,0,0.2)',
      }} />
    </button>
  );
}

function ProfileTab({ user }) {
  const [form, setForm] = useState({ fullName:user?.fullName||'', email:user?.email||'', phone:'9876543210', designation:'Branch Manager' });
  const [saved, setSaved] = useState(false);
  const set=(k,v)=>setForm(f=>({...f,[k]:v}));
  return (
    <div className="card">
      <h3 className="card-title" style={{marginBottom:18}}>Profile Information</h3>
      <div style={{ display:'flex', alignItems:'center', gap:16, marginBottom:24 }}>
        <div style={{ width:64,height:64,borderRadius:14, background:'linear-gradient(135deg,#1D4ED8,#3B82F6)', display:'flex',alignItems:'center',justifyContent:'center',fontSize:24,fontWeight:800,color:'#fff' }}>
          {(form.fullName||'U').charAt(0)}
        </div>
        <div>
          <button className="btn btn-ghost" style={{fontSize:12}}>📷 Change Photo</button>
          <div style={{ fontSize:11, color:'#94A3B8', marginTop:6 }}>{user?.employeeId} · {user?.branchCode}</div>
        </div>
      </div>
      <div className="grid-2">
        <div className="form-group"><label className="form-label">Full Name</label><input className="form-input" value={form.fullName} onChange={e=>set('fullName',e.target.value)} /></div>
        <div className="form-group"><label className="form-label">Email</label><input className="form-input" value={form.email} onChange={e=>set('email',e.target.value)} /></div>
        <div className="form-group"><label className="form-label">Phone</label><input className="form-input" value={form.phone} onChange={e=>set('phone',e.target.value)} /></div>
        <div className="form-group"><label className="form-label">Designation</label><input className="form-input" value={form.designation} disabled style={{background:'#F8FAFC'}} /></div>
      </div>
      <div style={{ marginTop:18, display:'flex', gap:10, alignItems:'center' }}>
        <button onClick={()=>{setSaved(true);setTimeout(()=>setSaved(false),2000);}} className="btn btn-primary">Save Changes</button>
        {saved && <span style={{ fontSize:12, color:'#059669', fontWeight:600 }}>✓ Profile updated</span>}
      </div>
    </div>
  );
}

function SecurityTab() {
  const [form, setForm] = useState({ old:'', next:'', confirm:'' });
  const [twoFA, setTwoFA] = useState(false);
  const [msg, setMsg] = useState('');
  const set=(k,v)=>setForm(f=>({...f,[k]:v}));

  const submit = async () => {
    if (!form.old || !form.next) { setMsg('All fields required.'); return; }
    if (form.next !== form.confirm) { setMsg('Passwords do not match.'); return; }
    if (form.next.length < 8) { setMsg('Password must be at least 8 characters.'); return; }
    try { await authApi.changePassword(form.old, form.next); } catch {}
    setMsg('✓ Password changed successfully.');
    setForm({old:'',next:'',confirm:''});
  };

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:16 }}>
      <div className="card">
        <h3 className="card-title" style={{marginBottom:18}}>Change Password</h3>
        <div className="grid-2" style={{marginBottom:14}}>
          <div className="form-group"><label className="form-label">Current Password</label><input type="password" className="form-input" value={form.old} onChange={e=>set('old',e.target.value)} /></div>
          <div></div>
          <div className="form-group"><label className="form-label">New Password</label><input type="password" className="form-input" value={form.next} onChange={e=>set('next',e.target.value)} placeholder="Min 8 characters" /></div>
          <div className="form-group"><label className="form-label">Confirm New Password</label><input type="password" className="form-input" value={form.confirm} onChange={e=>set('confirm',e.target.value)} /></div>
        </div>
        {msg && <div style={{ fontSize:12, color: msg.startsWith('✓')?'#059669':'#DC2626', marginBottom:12 }}>{msg}</div>}
        <button onClick={submit} className="btn btn-primary">Update Password</button>
      </div>

      <div className="card">
        <h3 className="card-title" style={{marginBottom:6}}>Two-Factor Authentication</h3>
        <p className="card-sub" style={{marginBottom:16}}>Add an extra layer of security with OTP-based 2FA</p>
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'12px 16px', background:'#F8FAFC', borderRadius:10 }}>
          <div>
            <div style={{ fontSize:13, fontWeight:600 }}>SMS / Email OTP</div>
            <div style={{ fontSize:11, color:'#64748B' }}>{twoFA ? 'Enabled — OTP required at login' : 'Disabled'}</div>
          </div>
          <Toggle checked={twoFA} onChange={setTwoFA} />
        </div>
      </div>

      <div className="card">
        <h3 className="card-title" style={{marginBottom:14}}>Active Sessions</h3>
        {[
          { device:'Chrome on Windows', location:'New Delhi, India', current:true,  time:'Active now' },
          { device:'Mobile App - Android', location:'New Delhi, India', current:false, time:'2 hours ago' },
        ].map((s,i)=>(
          <div key={i} style={{ display:'flex',justifyContent:'space-between',alignItems:'center',padding:'10px 0', borderBottom: i===0?'1px solid #F1F5F9':'none' }}>
            <div>
              <div style={{ fontSize:13, fontWeight:600 }}>{s.device} {s.current && <span className="badge badge-success" style={{marginLeft:6}}>Current</span>}</div>
              <div style={{ fontSize:11, color:'#94A3B8' }}>{s.location} · {s.time}</div>
            </div>
            {!s.current && <button className="btn btn-ghost" style={{fontSize:11, color:'#DC2626'}}>Revoke</button>}
          </div>
        ))}
      </div>
    </div>
  );
}

function NotificationsTab() {
  const [prefs, setPrefs] = useState({
    txnSms:true, txnEmail:true, lowBalance:true, loginAlerts:true,
    promotions:false, statementEmail:true, approvalAlerts:true,
  });
  const toggle = (k) => setPrefs(p=>({...p,[k]:!p[k]}));
  const items = [
    { k:'txnSms',          l:'Transaction SMS Alerts',    d:'Get SMS for every debit/credit' },
    { k:'txnEmail',        l:'Transaction Email Alerts',  d:'Email receipt for transactions ≥ ₹5,000' },
    { k:'lowBalance',      l:'Low Balance Alerts',        d:'Notify when balance falls below minimum' },
    { k:'loginAlerts',     l:'Login Notifications',       d:'Alert on login from new device/location' },
    { k:'approvalAlerts',  l:'Approval Queue Alerts',     d:'Notify managers of pending approvals' },
    { k:'statementEmail',  l:'Monthly e-Statement',       d:'Receive account statement via email' },
    { k:'promotions',      l:'Promotional Offers',        d:'Updates on new products and offers' },
  ];
  return (
    <div className="card">
      <h3 className="card-title" style={{marginBottom:18}}>Notification Preferences</h3>
      {items.map((item,i)=>(
        <div key={item.k} style={{ display:'flex',justifyContent:'space-between',alignItems:'center', padding:'12px 0', borderBottom: i<items.length-1?'1px solid #F1F5F9':'none' }}>
          <div>
            <div style={{ fontSize:13, fontWeight:600, color:'#1E293B' }}>{item.l}</div>
            <div style={{ fontSize:11, color:'#64748B' }}>{item.d}</div>
          </div>
          <Toggle checked={prefs[item.k]} onChange={()=>toggle(item.k)} />
        </div>
      ))}
    </div>
  );
}

function BankConfigTab() {
  const [config, setConfig] = useState({
    bankName:'CoreNova Bank', bankCode:'CNB', ifscPrefix:'CNB0', branchCode:'MAIN001',
    dailyLimit:'100000', upiLimit:'100000', rtgsMinimum:'200000', currency:'INR', timezone:'Asia/Kolkata',
  });
  const set=(k,v)=>setConfig(c=>({...c,[k]:v}));
  return (
    <div className="card">
      <h3 className="card-title" style={{marginBottom:4}}>Bank Configuration</h3>
      <p className="card-sub" style={{marginBottom:18}}>System-wide settings — Admin access only</p>
      <div className="grid-2">
        <div className="form-group"><label className="form-label">Bank Name</label><input className="form-input" value={config.bankName} onChange={e=>set('bankName',e.target.value)} /></div>
        <div className="form-group"><label className="form-label">Bank Code</label><input className="form-input mono" value={config.bankCode} onChange={e=>set('bankCode',e.target.value)} /></div>
        <div className="form-group"><label className="form-label">IFSC Prefix</label><input className="form-input mono" value={config.ifscPrefix} onChange={e=>set('ifscPrefix',e.target.value)} /></div>
        <div className="form-group"><label className="form-label">Default Branch Code</label><input className="form-input mono" value={config.branchCode} onChange={e=>set('branchCode',e.target.value)} /></div>
        <div className="form-group"><label className="form-label">Default Daily Txn Limit (₹)</label><input className="form-input mono" type="number" value={config.dailyLimit} onChange={e=>set('dailyLimit',e.target.value)} /></div>
        <div className="form-group"><label className="form-label">UPI Per-Txn Limit (₹)</label><input className="form-input mono" type="number" value={config.upiLimit} onChange={e=>set('upiLimit',e.target.value)} /></div>
        <div className="form-group"><label className="form-label">RTGS Minimum (₹)</label><input className="form-input mono" type="number" value={config.rtgsMinimum} onChange={e=>set('rtgsMinimum',e.target.value)} /></div>
        <div className="form-group">
          <label className="form-label">Timezone</label>
          <select className="form-input form-select" value={config.timezone} onChange={e=>set('timezone',e.target.value)}>
            <option value="Asia/Kolkata">Asia/Kolkata (IST)</option>
            <option value="UTC">UTC</option>
          </select>
        </div>
      </div>
      <button className="btn btn-primary" style={{marginTop:18}}>Save Configuration</button>
    </div>
  );
}

export default function SettingsPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ROLE_ADMIN';
  const [tab, setTab] = useState('profile');
  const TABS = [
    { k:'profile', l:'👤 Profile' },
    { k:'security', l:'🔐 Security' },
    { k:'notifications', l:'🔔 Notifications' },
    ...(isAdmin ? [{ k:'bank', l:'🏦 Bank Config' }] : []),
  ];

  return (
    <div>
      <div className="page-header"><h1 className="page-title">Settings</h1><p className="page-desc">Manage your profile, security, and preferences</p></div>
      <div style={{ display:'flex', gap:4, marginBottom:20, borderBottom:'1px solid #E2E8F0' }}>
        {TABS.map(t=>(
          <button key={t.k} onClick={()=>setTab(t.k)} style={{
            padding:'10px 18px', background:'none', border:'none', cursor:'pointer', fontSize:13, fontWeight:600, fontFamily:'inherit',
            color: tab===t.k?'#1D4ED8':'#64748B', borderBottom: tab===t.k?'2px solid #1D4ED8':'2px solid transparent', marginBottom:-1,
          }}>{t.l}</button>
        ))}
      </div>
      <div style={{ maxWidth:720 }}>
        {tab==='profile' && <ProfileTab user={user} />}
        {tab==='security' && <SecurityTab />}
        {tab==='notifications' && <NotificationsTab />}
        {tab==='bank' && isAdmin && <BankConfigTab />}
      </div>
    </div>
  );
}
