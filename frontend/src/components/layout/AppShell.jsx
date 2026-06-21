/**
 * AppShell.jsx – Layout wrapper with TopBar matching Image 2
 * TopBar: hamburger | search | bell(3) | Main Branch dropdown | user avatar + name + role
 */
import React, { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import { useAuth } from '../../hooks/useAuth';

/* ── TopBar ── */
function TopBar({ onToggleSidebar, pendingApprovals }) {
  const { user } = useAuth();
  const [now, setNow] = useState(new Date());
  const [branch, setBranch] = useState('Main Branch');
  const [showBranch, setShowBranch] = useState(false);
  const [showUser,   setShowUser]   = useState(false);

  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 60000);
    return () => clearInterval(t);
  }, []);

  const BRANCHES = ['Main Branch', 'North Branch', 'South Branch', 'East Branch'];

  const ROLE_DISPLAY = {
    ROLE_ADMIN:    'System Admin',
    ROLE_MANAGER:  'Branch Manager',
    ROLE_TELLER:   'Senior Teller',
    ROLE_AUDITOR:  'Compliance Auditor',
    ROLE_CUSTOMER: 'Customer',
  };

  return (
    <header style={{
      height:       '64px',
      background:   '#fff',
      borderBottom: '1px solid #E2E8F0',
      display:      'flex',
      alignItems:   'center',
      padding:      '0 20px',
      gap:          12,
      flexShrink:   0,
      boxShadow:    '0 1px 3px rgba(0,0,0,0.04)',
      position:     'relative',
      zIndex:       50,
    }}>

      {/* Hamburger */}
      <button onClick={onToggleSidebar}
        style={{ background:'none', border:'none', cursor:'pointer', fontSize:18, color:'#475569', padding:'4px 6px', borderRadius:6 }}
        onMouseEnter={e => e.currentTarget.style.background='#F1F5F9'}
        onMouseLeave={e => e.currentTarget.style.background='none'}>
        ☰
      </button>

      {/* Search */}
      <div style={{
        flex:1, maxWidth:440,
        display:'flex', alignItems:'center', gap:8,
        background:'#F8FAFC', border:'1px solid #E2E8F0',
        borderRadius:8, padding:'8px 14px',
      }}>
        <span style={{ color:'#94A3B8', fontSize:14 }}>🔍</span>
        <input placeholder="Search anything..." style={{
          border:'none', background:'transparent', outline:'none',
          fontSize:13, color:'#475569', width:'100%', fontFamily:'inherit',
        }} />
      </div>

      <div style={{ flex:1 }} />

      {/* Notifications bell */}
      <div style={{ position:'relative' }}>
        <button style={{
          width:38, height:38, borderRadius:8,
          background:'#F8FAFC', border:'1px solid #E2E8F0',
          display:'flex', alignItems:'center', justifyContent:'center',
          cursor:'pointer', fontSize:17, position:'relative',
        }}>
          🔔
          {pendingApprovals > 0 && (
            <span style={{
              position:'absolute', top:-3, right:-3,
              background:'#EF4444', color:'#fff',
              fontSize:9, fontWeight:700, padding:'1px 5px', borderRadius:99,
              lineHeight:1.6, border:'1.5px solid #fff',
              minWidth:16, textAlign:'center',
            }}>{Math.min(pendingApprovals, 9)}</span>
          )}
        </button>
      </div>

      {/* Branch selector */}
      <div style={{ position:'relative' }}>
        <button onClick={() => { setShowBranch(s=>!s); setShowUser(false); }}
          style={{
            display:'flex', alignItems:'center', gap:6,
            padding:'7px 12px', borderRadius:8,
            background:'#F8FAFC', border:'1px solid #E2E8F0',
            cursor:'pointer', fontSize:13, fontWeight:500, color:'#334155',
            fontFamily:'inherit',
          }}>
          🏢 {branch} <span style={{ color:'#94A3B8' }}>▾</span>
        </button>
        {showBranch && (
          <div style={{
            position:'absolute', top:'calc(100% + 6px)', right:0, zIndex:200,
            background:'#fff', border:'1px solid #E2E8F0', borderRadius:10,
            boxShadow:'0 8px 24px rgba(0,0,0,0.1)', minWidth:160, overflow:'hidden',
          }}>
            {BRANCHES.map(b => (
              <button key={b} onClick={() => { setBranch(b); setShowBranch(false); }}
                style={{
                  display:'block', width:'100%', padding:'9px 16px',
                  background: b === branch ? '#EFF6FF' : 'transparent',
                  border:'none', cursor:'pointer', textAlign:'left',
                  fontSize:13, color: b === branch ? '#1D4ED8' : '#334155',
                  fontWeight: b === branch ? 600 : 400, fontFamily:'inherit',
                }}>
                {b}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* User */}
      <div style={{ position:'relative' }}>
        <button onClick={() => { setShowUser(s=>!s); setShowBranch(false); }}
          style={{
            display:'flex', alignItems:'center', gap:10,
            padding:'6px 10px', borderRadius:8,
            background:'transparent', border:'none', cursor:'pointer',
          }}>
          {/* Avatar */}
          <div style={{
            width:34, height:34, borderRadius:8,
            background:'linear-gradient(135deg,#1D4ED8,#3B82F6)',
            display:'flex', alignItems:'center', justifyContent:'center',
            fontSize:13, fontWeight:700, color:'#fff', flexShrink:0,
          }}>
            {(user?.fullName||user?.username||'U').charAt(0)}
          </div>
          <div style={{ textAlign:'left' }}>
            <div style={{ fontSize:13, fontWeight:600, color:'#1E293B', whiteSpace:'nowrap' }}>
              {user?.fullName || user?.username || 'User'}
            </div>
            <div style={{ fontSize:11, color:'#64748B' }}>
              {ROLE_DISPLAY[user?.role] || user?.role?.replace('ROLE_','') || 'Staff'}
            </div>
          </div>
          <span style={{ color:'#94A3B8', fontSize:11 }}>▾</span>
        </button>

        {showUser && (
          <div style={{
            position:'absolute', top:'calc(100% + 6px)', right:0, zIndex:200,
            background:'#fff', border:'1px solid #E2E8F0', borderRadius:10,
            boxShadow:'0 8px 24px rgba(0,0,0,0.1)', minWidth:200, overflow:'hidden',
          }}>
            <div style={{ padding:'12px 16px', borderBottom:'1px solid #F1F5F9' }}>
              <div style={{ fontSize:13, fontWeight:700, color:'#1E293B' }}>{user?.fullName}</div>
              <div style={{ fontSize:11, color:'#64748B' }}>{user?.email}</div>
            </div>
            {[
              { icon:'👤', label:'My Profile' },
              { icon:'🔐', label:'Change Password' },
              { icon:'⚙️', label:'Preferences' },
            ].map(item => (
              <button key={item.label} onClick={() => setShowUser(false)}
                style={{
                  display:'flex', alignItems:'center', gap:10,
                  width:'100%', padding:'9px 16px',
                  background:'none', border:'none', cursor:'pointer',
                  fontSize:13, color:'#334155', fontFamily:'inherit', textAlign:'left',
                }}
                onMouseEnter={e => e.currentTarget.style.background='#F8FAFC'}
                onMouseLeave={e => e.currentTarget.style.background='none'}>
                <span>{item.icon}</span>{item.label}
              </button>
            ))}
          </div>
        )}
      </div>

    </header>
  );
}

/* ── AppShell ── */
export default function AppShell() {
  const [collapsed, setCollapsed]   = useState(false);
  const [pendingApprovals]          = useState(5); // fetched from API in real app

  return (
    <div className="app-shell">
      <Sidebar
        collapsed={collapsed}
        onToggle={() => setCollapsed(c => !c)}
        pendingApprovals={pendingApprovals}
      />
      <div className="main-area">
        <TopBar
          onToggleSidebar={() => setCollapsed(c => !c)}
          pendingApprovals={pendingApprovals}
        />
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
