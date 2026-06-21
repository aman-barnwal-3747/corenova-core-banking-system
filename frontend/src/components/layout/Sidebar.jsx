/**
 * Sidebar.jsx – Matches Image 2 exactly
 * Dark navy, blue active highlight, all nav items, Approvals badge
 */
import React, { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

const NAV = [
  { to:'/',              icon:'🏠', label:'Dashboard',    exact:true  },
  { to:'/accounts',      icon:'💳', label:'Accounts'                  },
  { to:'/transactions',  icon:'↔️', label:'Transactions'              },
  { to:'/fund-transfer', icon:'💸', label:'Fund Transfer'             },
  { to:'/beneficiaries', icon:'👥', label:'Beneficiaries'             },
  { to:'/payments',      icon:'💰', label:'Payments',     sub:true    },
  { to:'/loans',         icon:'🏦', label:'Loans'                     },
  { to:'/cards',         icon:'💳', label:'Cards'                     },
  { to:'/investments',   icon:'📈', label:'Investments'               },
  { to:'/reports',       icon:'📊', label:'Reports'                   },
  { to:'/approvals',     icon:'✅', label:'Approvals',    badge:true  },
  { to:'/users',         icon:'👤', label:'Users & Roles'             },
  { to:'/settings',      icon:'⚙️', label:'Settings'                  },
  { to:'/audit-logs',    icon:'📋', label:'Audit Logs'                },
  { to:'/support',       icon:'❓', label:'Support'                   },
];

export default function Sidebar({ collapsed, onToggle, pendingApprovals = 0 }) {
  const { user, logout } = useAuth();
  const location = useLocation();

  return (
    <aside style={{
      width:         collapsed ? '64px' : '210px',
      minWidth:      collapsed ? '64px' : '210px',
      background:    '#0D1B2E',
      height:        '100vh',
      display:       'flex',
      flexDirection: 'column',
      transition:    'width 0.22s ease, min-width 0.22s ease',
      overflow:      'hidden',
      flexShrink:    0,
    }}>

      {/* ── Logo ── */}
      <div style={{
        height:          '64px',
        display:         'flex',
        alignItems:      'center',
        gap:             12,
        padding:         collapsed ? '0 14px' : '0 18px',
        borderBottom:    '1px solid rgba(255,255,255,0.06)',
        flexShrink:      0,
        justifyContent:  collapsed ? 'center' : 'flex-start',
      }}>
        <div style={{
          width:36, height:36, borderRadius:8, flexShrink:0,
          background:'rgba(255,255,255,0.1)',
          display:'flex', alignItems:'center', justifyContent:'center', fontSize:18,
        }}>🏛️</div>
        {!collapsed && (
          <div>
            <div style={{ color:'#fff', fontWeight:700, fontSize:13, lineHeight:1.2 }}>CoreNova Bank</div>
            <div style={{ color:'rgba(255,255,255,0.35)', fontSize:9, letterSpacing:'0.07em', textTransform:'uppercase' }}>Core Banking System</div>
          </div>
        )}
      </div>

      {/* ── Nav items ── */}
      <nav style={{ flex:1, overflowY:'auto', overflowX:'hidden', padding:'10px 0' }}>
        {NAV.map(item => {
          const active = item.exact
            ? location.pathname === '/'
            : location.pathname.startsWith(item.to);
          const count = item.badge ? pendingApprovals : 0;

          return (
            <NavLink
              key={item.to}
              to={item.to}
              title={collapsed ? item.label : undefined}
              style={{ textDecoration:'none' }}
            >
              <div style={{
                display:        'flex',
                alignItems:     'center',
                gap:            10,
                padding:        collapsed ? '10px 0' : '9px 14px',
                margin:         '1px 8px',
                borderRadius:   8,
                background:     active ? '#1D4ED8' : 'transparent',
                cursor:         'pointer',
                justifyContent: collapsed ? 'center' : 'flex-start',
                position:       'relative',
                transition:     'background 0.12s',
              }}
              onMouseEnter={e => { if (!active) e.currentTarget.style.background='rgba(255,255,255,0.07)'; }}
              onMouseLeave={e => { if (!active) e.currentTarget.style.background='transparent'; }}>
                {/* Icon */}
                <span style={{ fontSize:16, flexShrink:0, lineHeight:1 }}>{item.icon}</span>

                {/* Label */}
                {!collapsed && (
                  <span style={{
                    flex:1, fontSize:13, fontWeight: active ? 600 : 400,
                    color: active ? '#fff' : 'rgba(255,255,255,0.6)',
                    whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis',
                  }}>
                    {item.label}
                  </span>
                )}

                {/* Expand arrow for Payments */}
                {!collapsed && item.sub && (
                  <span style={{ color:'rgba(255,255,255,0.3)', fontSize:10 }}>▾</span>
                )}

                {/* Badge for Approvals */}
                {!collapsed && item.badge && count > 0 && (
                  <span style={{
                    background:'#EF4444', color:'#fff',
                    fontSize:10, fontWeight:700,
                    padding:'1px 6px', borderRadius:99, lineHeight:1.6,
                  }}>{count}</span>
                )}

                {/* Collapsed badge dot */}
                {collapsed && item.badge && count > 0 && (
                  <span style={{
                    position:'absolute', top:4, right:4,
                    width:8, height:8, borderRadius:'50%',
                    background:'#EF4444', border:'1.5px solid #0D1B2E',
                  }} />
                )}
              </div>
            </NavLink>
          );
        })}
      </nav>

      {/* ── Logout + user ── */}
      <div style={{ borderTop:'1px solid rgba(255,255,255,0.06)', padding:'10px 8px', flexShrink:0 }}>
        <button onClick={logout}
          style={{
            display:'flex', alignItems:'center', gap:10, width:'100%',
            padding: collapsed ? '10px 0' : '9px 14px',
            background:'transparent', border:'none', borderRadius:8,
            cursor:'pointer', justifyContent: collapsed ? 'center' : 'flex-start',
            transition:'background 0.12s',
          }}
          onMouseEnter={e => e.currentTarget.style.background='rgba(239,68,68,0.12)'}
          onMouseLeave={e => e.currentTarget.style.background='transparent'}>
          <span style={{ fontSize:16 }}>🚪</span>
          {!collapsed && <span style={{ fontSize:13, color:'rgba(255,255,255,0.5)', fontWeight:500 }}>Logout</span>}
        </button>

        {!collapsed && user && (
          <div style={{ display:'flex', alignItems:'center', gap:10, padding:'8px 12px', borderRadius:8, background:'rgba(255,255,255,0.04)', marginTop:6 }}>
            <div style={{
              width:30, height:30, borderRadius:8, flexShrink:0,
              background:'linear-gradient(135deg,#1D4ED8,#3B82F6)',
              display:'flex', alignItems:'center', justifyContent:'center',
              fontSize:12, fontWeight:700, color:'#fff',
            }}>
              {(user.fullName||user.username||'U').charAt(0)}
            </div>
            <div style={{ overflow:'hidden' }}>
              <div style={{ fontSize:12, fontWeight:600, color:'#fff', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>
                {user.fullName || user.username}
              </div>
              <div style={{ fontSize:10, color:'rgba(255,255,255,0.35)' }}>
                {(user.role||'').replace('ROLE_','')}
              </div>
            </div>
          </div>
        )}
      </div>
    </aside>
  );
}
