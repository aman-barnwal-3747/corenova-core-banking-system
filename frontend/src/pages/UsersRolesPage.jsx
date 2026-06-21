/**
 * UsersRolesPage.jsx – PDF §4 Authentication Module: Roles
 * Roles: ADMIN | MANAGER | TELLER | CUSTOMER | AUDITOR
 * ADMIN only — create users, change roles, lock/unlock
 */
import React, { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { userApi } from '../api';

const ROLE_META = {
  ROLE_ADMIN:    {label:'Admin',    bg:'#EDE9FE', color:'#7C3AED', icon:'👑'},
  ROLE_MANAGER:  {label:'Manager',  bg:'#DBEAFE', color:'#1D4ED8', icon:'💼'},
  ROLE_TELLER:   {label:'Teller',   bg:'#D1FAE5', color:'#059669', icon:'🧑‍💼'},
  ROLE_AUDITOR:  {label:'Auditor',  bg:'#FFE4E6', color:'#BE123C', icon:'🔍'},
  ROLE_CUSTOMER: {label:'Customer', bg:'#FEF3C7', color:'#92400E', icon:'👤'},
};

const DEMO_USERS = [
  { userId:1, employeeId:'EMP00001', username:'admin',   fullName:'Arjun Mehra',  email:'admin@corenova.bank',   role:'ROLE_ADMIN',   branchCode:'MAIN001', designation:'System Administrator', isActive:true, isLocked:false, lastLoginAt:new Date(Date.now()-3600000).toISOString() },
  { userId:2, employeeId:'EMP00002', username:'manager', fullName:'Sunita Kapoor',email:'manager@corenova.bank', role:'ROLE_MANAGER', branchCode:'MAIN001', designation:'Branch Manager',         isActive:true, isLocked:false, lastLoginAt:new Date(Date.now()-7200000).toISOString() },
  { userId:3, employeeId:'EMP00003', username:'teller',  fullName:'Kiran Bose',   email:'teller@corenova.bank',  role:'ROLE_TELLER',  branchCode:'MAIN001', designation:'Senior Teller',          isActive:true, isLocked:false, lastLoginAt:new Date(Date.now()-1800000).toISOString() },
  { userId:4, employeeId:'EMP00004', username:'auditor', fullName:'Deepa Nair',   email:'auditor@corenova.bank', role:'ROLE_AUDITOR', branchCode:'MAIN001', designation:'Compliance Auditor',     isActive:true, isLocked:false, lastLoginAt:new Date(Date.now()-86400000).toISOString() },
  { userId:5, employeeId:'EMP00005', username:'rohit.t', fullName:'Rohit Tiwari', email:'rohit.t@corenova.bank', role:'ROLE_TELLER',  branchCode:'NORTH02', designation:'Teller',                 isActive:true, isLocked:true,  lastLoginAt:new Date(Date.now()-432000000).toISOString() },
];

function CreateUserModal({ onClose, onCreate }) {
  const [form,setForm] = useState({ fullName:'', username:'', email:'', employeeId:'', role:'ROLE_TELLER', branchCode:'MAIN001', designation:'' });
  const set=(k,v)=>setForm(f=>({...f,[k]:v}));
  return (
    <div style={{ position:'fixed',inset:0,zIndex:1000,background:'rgba(15,23,42,0.5)',display:'flex',alignItems:'center',justifyContent:'center',padding:20 }}>
      <div style={{ background:'#fff',borderRadius:16,width:'100%',maxWidth:480,padding:24 }}>
        <h3 style={{ fontSize:15,fontWeight:700,marginBottom:16 }}>Create New User</h3>
        <div style={{ display:'flex',flexDirection:'column',gap:12 }}>
          <div className="grid-2">
            <div className="form-group"><label className="form-label">Full Name *</label><input className="form-input" value={form.fullName} onChange={e=>set('fullName',e.target.value)} /></div>
            <div className="form-group"><label className="form-label">Employee ID</label><input className="form-input" value={form.employeeId} onChange={e=>set('employeeId',e.target.value)} placeholder="Auto if blank" /></div>
          </div>
          <div className="grid-2">
            <div className="form-group"><label className="form-label">Username *</label><input className="form-input" value={form.username} onChange={e=>set('username',e.target.value)} /></div>
            <div className="form-group"><label className="form-label">Email *</label><input className="form-input" type="email" value={form.email} onChange={e=>set('email',e.target.value)} /></div>
          </div>
          <div className="grid-2">
            <div className="form-group">
              <label className="form-label">Role *</label>
              <select className="form-input form-select" value={form.role} onChange={e=>set('role',e.target.value)}>
                {Object.entries(ROLE_META).filter(([k])=>k!=='ROLE_CUSTOMER').map(([k,v])=><option key={k} value={k}>{v.label}</option>)}
              </select>
            </div>
            <div className="form-group"><label className="form-label">Branch Code</label><input className="form-input" value={form.branchCode} onChange={e=>set('branchCode',e.target.value)} /></div>
          </div>
          <div className="form-group"><label className="form-label">Designation</label><input className="form-input" value={form.designation} onChange={e=>set('designation',e.target.value)} placeholder="e.g. Senior Teller" /></div>
          <div style={{ background:'#EFF6FF',border:'1px solid #BFDBFE',borderRadius:8,padding:'10px 14px',fontSize:12,color:'#1E40AF' }}>
            🔑 Default password: <b className="mono">Welcome@123</b> — user must change on first login.
          </div>
        </div>
        <div style={{ display:'flex',gap:10,marginTop:18 }}>
          <button onClick={onClose} className="btn btn-ghost">Cancel</button>
          <button onClick={()=>{ onCreate(form); onClose(); }} disabled={!form.fullName||!form.username||!form.email} className="btn btn-primary" style={{ flex:1,justifyContent:'center' }}>+ Create User</button>
        </div>
      </div>
    </div>
  );
}

export default function UsersRolesPage() {
  const { user: me } = useAuth();
  const [users, setUsers] = useState(DEMO_USERS);
  const [showModal, setShowModal] = useState(false);
  const [toast, setToast] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const showToast=(m)=>{setToast(m);setTimeout(()=>setToast(''),4000);};

  const isAdmin = me?.role === 'ROLE_ADMIN';

  if (!isAdmin) return (
    <div>
      <div className="page-header"><h1 className="page-title">Users & Roles</h1></div>
      <div className="card" style={{ textAlign:'center', padding:60 }}>
        <div style={{ fontSize:40, marginBottom:12 }}>🔒</div>
        <h3 style={{ fontSize:15, fontWeight:700, marginBottom:6 }}>Admin Access Required</h3>
        <p style={{ fontSize:13, color:'#64748B' }}>User and role management is restricted to <b>ADMIN</b> per PDF §4 RBAC architecture.</p>
      </div>
    </div>
  );

  const filtered = roleFilter==='ALL' ? users : users.filter(u=>u.role===roleFilter);

  const toggleLock = async (u) => {
    try { u.isLocked ? await userApi.unlock(u.userId) : await userApi.lock(u.userId); } catch {}
    setUsers(us=>us.map(x=>x.userId===u.userId?{...x,isLocked:!x.isLocked}:x));
    showToast(`${u.fullName} ${u.isLocked?'unlocked':'locked'}.`);
  };

  const changeRole = async (u, role) => {
    try { await userApi.changeRole(u.userId, role); } catch {}
    setUsers(us=>us.map(x=>x.userId===u.userId?{...x,role}:x));
    showToast(`${u.fullName}'s role changed to ${ROLE_META[role].label}.`);
  };

  const create = (form) => {
    const newUser = { userId:Date.now(), employeeId:form.employeeId||`EMP${String(users.length+1).padStart(5,'0')}`, ...form, isActive:true, isLocked:false, lastLoginAt:null };
    setUsers(u=>[newUser,...u]);
    showToast(`${form.fullName} created. Default password: Welcome@123`);
  };

  const roleCounts = Object.keys(ROLE_META).map(r=>({role:r, count:users.filter(u=>u.role===r).length}));

  return (
    <div>
      {toast && <div style={{ position:'fixed',top:80,right:24,zIndex:2000,background:'#0F2342',color:'#fff',padding:'12px 20px',borderRadius:10,fontSize:13,fontWeight:500 }}>✓ {toast}</div>}

      <div className="page-header flex items-center justify-between">
        <div><h1 className="page-title">Users & Roles</h1><p className="page-desc">PDF §4 — RBAC: ADMIN, MANAGER, TELLER, AUDITOR, CUSTOMER</p></div>
        <button className="btn btn-primary" onClick={()=>setShowModal(true)}>+ Create User</button>
      </div>

      {/* Role distribution */}
      <div className="grid-4 mb-5">
        {roleCounts.map(({role,count})=>{
          const meta = ROLE_META[role];
          return (
            <button key={role} onClick={()=>setRoleFilter(roleFilter===role?'ALL':role)}
              className="card" style={{ padding:'16px 20px', textAlign:'left', cursor:'pointer', border:'none', outline: roleFilter===role?`2px solid ${meta.color}`:'1px solid #E2E8F0' }}>
              <div style={{ display:'flex',justifyContent:'space-between',alignItems:'center' }}>
                <span style={{ fontSize:24 }}>{meta.icon}</span>
                <span style={{ fontSize:24,fontWeight:800,color:'#0F172A' }}>{count}</span>
              </div>
              <div style={{ fontSize:12,color:'#64748B',marginTop:8 }}>{meta.label}{count===1?'':'s'}</div>
            </button>
          );
        })}
      </div>

      {roleFilter!=='ALL' && (
        <div style={{ marginBottom:12 }}>
          <button className="btn btn-ghost" onClick={()=>setRoleFilter('ALL')} style={{ fontSize:12 }}>✕ Clear filter: {ROLE_META[roleFilter].label}</button>
        </div>
      )}

      {/* Users table */}
      <div className="card">
        <table className="data-table">
          <thead><tr><th>Employee</th><th>Username</th><th>Email</th><th>Role</th><th>Branch</th><th>Status</th><th>Last Login</th><th style={{textAlign:'right'}}>Actions</th></tr></thead>
          <tbody>
            {filtered.map(u=>{
              const meta = ROLE_META[u.role];
              return (
                <tr key={u.userId}>
                  <td>
                    <div style={{ display:'flex',alignItems:'center',gap:10 }}>
                      <div style={{ width:32,height:32,borderRadius:8,background:'linear-gradient(135deg,#1D4ED8,#3B82F6)',display:'flex',alignItems:'center',justifyContent:'center',fontSize:12,fontWeight:700,color:'#fff' }}>{u.fullName.charAt(0)}</div>
                      <div>
                        <div style={{ fontWeight:600,fontSize:12 }}>{u.fullName}</div>
                        <div className="mono" style={{ fontSize:10,color:'#94A3B8' }}>{u.employeeId}</div>
                      </div>
                    </div>
                  </td>
                  <td className="mono" style={{ fontSize:12 }}>{u.username}</td>
                  <td style={{ fontSize:12,color:'#64748B' }}>{u.email}</td>
                  <td>
                    <select className="form-input form-select" style={{ padding:'4px 8px', fontSize:11, width:'auto' }} value={u.role} onChange={e=>changeRole(u,e.target.value)} disabled={u.username===me?.username}>
                      {Object.entries(ROLE_META).map(([k,v])=><option key={k} value={k}>{v.label}</option>)}
                    </select>
                  </td>
                  <td className="mono" style={{ fontSize:11 }}>{u.branchCode}</td>
                  <td>
                    {u.isLocked
                      ? <span className="badge badge-danger">🔒 Locked</span>
                      : u.isActive ? <span className="badge badge-success">Active</span> : <span className="badge badge-neutral">Inactive</span>}
                  </td>
                  <td style={{ fontSize:11,color:'#94A3B8' }}>{u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleString('en-IN',{day:'2-digit',month:'short',hour:'2-digit',minute:'2-digit',hour12:true}) : 'Never'}</td>
                  <td style={{ textAlign:'right' }}>
                    {u.username !== me?.username && (
                      <button onClick={()=>toggleLock(u)} className={`btn ${u.isLocked?'btn-success':'btn-ghost'}`} style={{ fontSize:11, padding:'5px 12px' }}>
                        {u.isLocked?'Unlock':'Lock'}
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {showModal && <CreateUserModal onClose={()=>setShowModal(false)} onCreate={create} />}
    </div>
  );
}
