/**
 * LoginPage.jsx – Matches Image 1 exactly
 * Left: dark navy panel with building bg, features grid
 * Right: clean white login form with all elements from image
 */
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

const FEATURES = [
  { icon: '🛡️', title: 'Secure & Trusted',    desc: 'Bank-grade security to protect your data' },
  { icon: '⚡', title: 'Fast & Reliable',      desc: 'Seamless banking anytime, anywhere' },
  { icon: '👥', title: 'Customer Centric',     desc: 'Designed for customers and efficient operations' },
  { icon: '📊', title: 'Smart Analytics',      desc: 'Insights that help you make better decisions' },
];

const COMPLIANCE = [
  { icon: '🏛️', label: 'RBI Compliant',    sub: 'Secure & Regulated' },
  { icon: '🔒', label: '256-bit SSL',      sub: 'Bank Grade Security' },
  { icon: '✅', label: 'ISO 27001',        sub: 'Certified' },
  { icon: '🎧', label: '24/7 Support',     sub: "We're here to help" },
];

export default function LoginPage() {
  const [username,   setUsername]   = useState('');
  const [password,   setPassword]   = useState('');
  const [remember,   setRemember]   = useState(false);
  const [showPass,   setShowPass]   = useState(false);
  const [err,        setErr]        = useState('');
  const { login, loading, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => { if (isAuthenticated) navigate('/'); }, [isAuthenticated, navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) { setErr('Please enter username and password.'); return; }
    setErr('');
    try { await login(username.trim(), password); navigate('/'); }
    catch (er) { setErr(er.message || 'Invalid credentials. Please try again.'); }
  };

  return (
    <div style={{ display: 'flex', minHeight: '100vh', fontFamily: "'Inter',sans-serif" }}>

      {/* ── LEFT: Brand panel ── */}
      <div style={{
        flex: '0 0 47%',
        background: 'linear-gradient(160deg, #0D1B2E 0%, #0F2342 40%, #0a1628 100%)',
        position: 'relative', overflow: 'hidden',
        display: 'flex', flexDirection: 'column', padding: '36px 48px',
      }}>
        {/* Building silhouette overlay */}
        <div style={{
          position: 'absolute', inset: 0, zIndex: 0,
          background: `
            linear-gradient(160deg, rgba(13,27,46,0.85) 0%, rgba(15,35,66,0.7) 100%),
            repeating-linear-gradient(
              90deg,
              transparent, transparent 60px,
              rgba(255,255,255,0.015) 60px, rgba(255,255,255,0.015) 61px
            ),
            repeating-linear-gradient(
              0deg,
              transparent, transparent 80px,
              rgba(255,255,255,0.015) 80px, rgba(255,255,255,0.015) 81px
            )
          `,
        }} />

        {/* Glow accents */}
        <div style={{ position:'absolute', top:'30%', right:'-10%', width:300, height:300, borderRadius:'50%', background:'rgba(37,99,235,0.08)', filter:'blur(60px)', zIndex:0 }} />
        <div style={{ position:'absolute', bottom:'20%', left:'-5%',  width:200, height:200, borderRadius:'50%', background:'rgba(16,185,129,0.06)', filter:'blur(40px)', zIndex:0 }} />

        {/* Content */}
        <div style={{ position:'relative', zIndex:1, display:'flex', flexDirection:'column', height:'100%' }}>

          {/* Logo */}
          <div style={{ display:'flex', alignItems:'center', gap:14, marginBottom:'auto' }}>
            <div style={{
              width:44, height:44, borderRadius:10,
              background:'rgba(255,255,255,0.12)',
              border:'1px solid rgba(255,255,255,0.15)',
              display:'flex', alignItems:'center', justifyContent:'center', fontSize:22,
            }}>🏛️</div>
            <div>
              <div style={{ color:'#fff', fontWeight:800, fontSize:18, letterSpacing:'-0.01em' }}>CoreNova Bank</div>
              <div style={{ color:'rgba(255,255,255,0.45)', fontSize:11, letterSpacing:'0.06em' }}>Core Banking System</div>
            </div>
          </div>

          {/* Headline */}
          <div style={{ margin:'48px 0 40px' }}>
            <h1 style={{ fontSize:38, fontWeight:800, color:'#fff', lineHeight:1.15, letterSpacing:'-0.03em', marginBottom:16 }}>
              Banking Solutions<br />
              Built for{' '}
              <span style={{ color:'#10B981' }}>Tomorrow</span>
            </h1>
            {/* Green divider */}
            <div style={{ width:48, height:3, background:'#10B981', borderRadius:99, marginBottom:20 }} />
            <p style={{ color:'rgba(255,255,255,0.5)', fontSize:13, lineHeight:1.7, maxWidth:320 }}>
              Experience secure, reliable and innovative banking with our next-generation core banking platform.
            </p>
          </div>

          {/* Features 2×2 grid */}
          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:16, marginBottom:40 }}>
            {FEATURES.map((f,i) => (
              <div key={i} style={{ display:'flex', alignItems:'flex-start', gap:12 }}>
                <div style={{
                  width:36, height:36, borderRadius:8, flexShrink:0,
                  background:'rgba(255,255,255,0.08)',
                  border:'1px solid rgba(255,255,255,0.1)',
                  display:'flex', alignItems:'center', justifyContent:'center', fontSize:16,
                }}>
                  {f.icon}
                </div>
                <div>
                  <div style={{ color:'#fff', fontWeight:600, fontSize:12, marginBottom:2 }}>{f.title}</div>
                  <div style={{ color:'rgba(255,255,255,0.4)', fontSize:11, lineHeight:1.5 }}>{f.desc}</div>
                </div>
              </div>
            ))}
          </div>

          {/* Footer */}
          <div style={{ color:'rgba(255,255,255,0.3)', fontSize:11, display:'flex', alignItems:'center', gap:6 }}>
            🔒 © 2025 CoreNova Bank. All rights reserved.
          </div>
        </div>
      </div>

      {/* ── RIGHT: Login form ── */}
      <div style={{
        flex:1, display:'flex', flexDirection:'column',
        alignItems:'center', justifyContent:'center',
        background:'#fff', padding:'40px 60px',
      }}>
        <div style={{ width:'100%', maxWidth:400 }}>

          {/* Heading */}
          <div style={{ textAlign:'center', marginBottom:32 }}>
            <h2 style={{ fontSize:26, fontWeight:800, color:'#0F172A', letterSpacing:'-0.02em', marginBottom:8 }}>
              Welcome Back!
            </h2>
            <p style={{ fontSize:13, color:'#64748B' }}>
              Sign in to continue to CoreNova Banking System
            </p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit}>

            {/* Username */}
            <div style={{ marginBottom:18 }}>
              <label style={{ display:'block', fontSize:13, fontWeight:600, color:'#334155', marginBottom:6 }}>
                Username / Employee ID
              </label>
              <div style={{ position:'relative' }}>
                <span style={{ position:'absolute', left:12, top:'50%', transform:'translateY(-50%)', fontSize:16 }}>👤</span>
                <input
                  type="text"
                  placeholder="Enter your username or employee ID"
                  value={username}
                  onChange={e => setUsername(e.target.value)}
                  autoFocus
                  style={{
                    width:'100%', padding:'11px 12px 11px 38px',
                    border:'1.5px solid #E2E8F0', borderRadius:8,
                    fontSize:13, fontFamily:'inherit', outline:'none',
                    transition:'border-color 0.15s',
                  }}
                  onFocus={e => e.target.style.borderColor='#2563EB'}
                  onBlur={e  => e.target.style.borderColor='#E2E8F0'}
                />
              </div>
            </div>

            {/* Password */}
            <div style={{ marginBottom:18 }}>
              <label style={{ display:'block', fontSize:13, fontWeight:600, color:'#334155', marginBottom:6 }}>
                Password
              </label>
              <div style={{ position:'relative' }}>
                <span style={{ position:'absolute', left:12, top:'50%', transform:'translateY(-50%)', fontSize:15 }}>🔒</span>
                <input
                  type={showPass ? 'text' : 'password'}
                  placeholder="Enter your password"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  style={{
                    width:'100%', padding:'11px 40px 11px 38px',
                    border:'1.5px solid #E2E8F0', borderRadius:8,
                    fontSize:13, fontFamily:'inherit', outline:'none',
                  }}
                  onFocus={e => e.target.style.borderColor='#2563EB'}
                  onBlur={e  => e.target.style.borderColor='#E2E8F0'}
                />
                <button type="button" onClick={() => setShowPass(s => !s)}
                  style={{ position:'absolute', right:12, top:'50%', transform:'translateY(-50%)', background:'none', border:'none', cursor:'pointer', fontSize:15, color:'#94A3B8', padding:0 }}>
                  {showPass ? '🙈' : '👁️'}
                </button>
              </div>
            </div>

            {/* Remember me + Forgot */}
            <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:24 }}>
              <label style={{ display:'flex', alignItems:'center', gap:8, cursor:'pointer', fontSize:13, color:'#475569' }}>
                <input type="checkbox" checked={remember} onChange={e => setRemember(e.target.checked)}
                  style={{ width:14, height:14, accentColor:'#2563EB', cursor:'pointer' }} />
                Remember me
              </label>
              <button type="button" style={{ background:'none', border:'none', cursor:'pointer', color:'#2563EB', fontSize:13, fontWeight:500, padding:0 }}>
                Forgot Password?
              </button>
            </div>

            {/* Error */}
            {err && (
              <div style={{ background:'#FEE2E2', color:'#DC2626', padding:'10px 14px', borderRadius:8, marginBottom:16, fontSize:13, border:'1px solid rgba(220,38,38,0.2)' }}>
                ⚠️ {err}
              </div>
            )}

            {/* Sign In button */}
            <button type="submit" disabled={loading}
              style={{
                width:'100%', padding:'13px', borderRadius:8,
                background: loading ? '#93C5FD' : '#1D4ED8',
                color:'#fff', fontWeight:700, fontSize:14,
                border:'none', cursor: loading ? 'not-allowed' : 'pointer',
                display:'flex', alignItems:'center', justifyContent:'center', gap:8,
                transition:'background 0.15s', letterSpacing:'0.01em',
              }}>
              {loading ? <><div className="spinner" style={{width:16,height:16}} /> Signing in...</>
                       : <><span>🔐</span> Sign In</>}
            </button>

            {/* OR divider */}
            <div style={{ display:'flex', alignItems:'center', gap:12, margin:'20px 0' }}>
              <div style={{ flex:1, height:1, background:'#E2E8F0' }} />
              <span style={{ fontSize:12, color:'#94A3B8', fontWeight:500 }}>or</span>
              <div style={{ flex:1, height:1, background:'#E2E8F0' }} />
            </div>

            {/* SSO button */}
            <button type="button"
              style={{
                width:'100%', padding:'12px', borderRadius:8,
                background:'#fff', color:'#1D4ED8', fontWeight:600, fontSize:13,
                border:'1.5px solid #2563EB', cursor:'pointer',
                display:'flex', alignItems:'center', justifyContent:'center', gap:8,
                transition:'background 0.15s',
              }}
              onMouseEnter={e => e.currentTarget.style.background='#EFF6FF'}
              onMouseLeave={e => e.currentTarget.style.background='#fff'}>
              🛡️ Sign in with SSO
            </button>

            {/* Contact admin */}
            <p style={{ textAlign:'center', marginTop:20, fontSize:12, color:'#94A3B8' }}>
              New to CoreNova Bank?{' '}
              <button type="button" style={{ background:'none', border:'none', color:'#2563EB', cursor:'pointer', fontSize:12, fontWeight:500, padding:0 }}>
                Contact Administrator
              </button>
            </p>
          </form>
        </div>

        {/* ── Compliance badges row (bottom) ── */}
        <div style={{
          display:'flex', gap:28, marginTop:40,
          paddingTop:24, borderTop:'1px solid #F1F5F9', width:'100%', maxWidth:400,
          justifyContent:'center', flexWrap:'wrap',
        }}>
          {COMPLIANCE.map((c,i) => (
            <div key={i} style={{ display:'flex', alignItems:'center', gap:8 }}>
              <span style={{ fontSize:18 }}>{c.icon}</span>
              <div>
                <div style={{ fontSize:11, fontWeight:700, color:'#334155' }}>{c.label}</div>
                <div style={{ fontSize:10, color:'#94A3B8' }}>{c.sub}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
