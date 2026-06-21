/**
 * SupportPage.jsx – Help Center & Support
 * Image 2 sidebar item "Support" (last item)
 */
import React, { useState } from 'react';

const FAQS = [
  { q:'How do I reset a customer\'s internet banking password?', a:'Navigate to Users & Roles (Admin) or use the Customer profile in Customers page → Security tab → Reset Credentials. The customer will receive a temporary password via registered mobile/email.' },
  { q:'What is the cooling period for new beneficiaries?', a:'Per RBI guidelines, any beneficiary added via internet/mobile banking has a mandatory 24-hour cooling period before the first transfer can be made. Branch-added beneficiaries are pre-verified and exempt.' },
  { q:'Why is RTGS unavailable for amounts below ₹2,00,000?', a:'RTGS (Real-Time Gross Settlement) is reserved for high-value transactions per RBI mandate, with a minimum of ₹2 Lakh. For smaller amounts, use NEFT (batch) or IMPS (instant 24×7).' },
  { q:'How does the Maker-Checker approval flow work?', a:'High-value or sensitive actions (transfers ≥ ₹5L, account freeze, KYC approval, loan sanction) are first created by a Maker (Teller/Officer) and must be approved by a different Checker (Manager/Admin) — enforcing the four-eyes principle. Requests auto-expire after 24 hours if unactioned.' },
  { q:'What happens when an account becomes dormant?', a:'Accounts with no customer-initiated transaction for 12 months are automatically flagged DORMANT by the nightly batch job. Dormant accounts cannot be debited until reactivated at a branch, but can still receive credits.' },
  { q:'How are NPAs classified?', a:'Following RBI IRAC norms: loans with 3+ overdue EMIs (90 days) are classified SUB_STANDARD, 12+ months overdue as DOUBTFUL, and beyond 36 months as LOSS. The nightly NPA classification job updates this automatically.' },
  { q:'Can I reverse a successful transaction?', a:'Yes — Managers/Admins can initiate a reversal from Transactions → select transaction → Reverse. This creates a new offsetting transaction; the original record is never modified, preserving the audit trail.' },
];

const QUICK_LINKS = [
  { icon:'📘', label:'API Documentation (Swagger)', desc:'Interactive API reference for developers', href:'#' },
  { icon:'🎥', label:'Video Tutorials',              desc:'Step-by-step walkthroughs of CBS modules', href:'#' },
  { icon:'📄', label:'User Manual (PDF)',            desc:'Complete operational guide', href:'#' },
  { icon:'🔧', label:'System Status',                desc:'Live uptime & incident reports', href:'#' },
];

function FaqItem({ faq, open, onToggle }) {
  return (
    <div style={{ borderBottom:'1px solid #F1F5F9' }}>
      <button onClick={onToggle} style={{
        width:'100%', display:'flex', justifyContent:'space-between', alignItems:'center',
        padding:'14px 0', background:'none', border:'none', cursor:'pointer', textAlign:'left', fontFamily:'inherit',
      }}>
        <span style={{ fontSize:13, fontWeight:600, color:'#1E293B' }}>{faq.q}</span>
        <span style={{ color:'#94A3B8', fontSize:14, flexShrink:0, marginLeft:12, transform: open?'rotate(180deg)':'none', transition:'transform 0.15s' }}>▾</span>
      </button>
      {open && <p style={{ fontSize:12, color:'#64748B', lineHeight:1.7, paddingBottom:16 }}>{faq.a}</p>}
    </div>
  );
}

export default function SupportPage() {
  const [openFaq, setOpenFaq] = useState(0);
  const [search, setSearch] = useState('');
  const [ticket, setTicket] = useState({ subject:'', category:'General', message:'' });
  const [sent, setSent] = useState(false);

  const filtered = FAQS.filter(f => !search.trim() || f.q.toLowerCase().includes(search.toLowerCase()) || f.a.toLowerCase().includes(search.toLowerCase()));

  const submitTicket = () => {
    if (!ticket.subject || !ticket.message) return;
    setSent(true);
    setTimeout(()=>{ setSent(false); setTicket({subject:'',category:'General',message:''}); }, 3000);
  };

  return (
    <div>
      <div className="page-header"><h1 className="page-title">Support</h1><p className="page-desc">Help center, FAQs, and ticket support</p></div>

      {/* Quick links */}
      <div className="grid-4 mb-5">
        {QUICK_LINKS.map((l,i)=>(
          <a key={i} href={l.href} className="card" style={{ textDecoration:'none', display:'block' }}>
            <div style={{ fontSize:24, marginBottom:10 }}>{l.icon}</div>
            <div style={{ fontSize:13, fontWeight:700, color:'#0F172A', marginBottom:4 }}>{l.label}</div>
            <div style={{ fontSize:11, color:'#64748B' }}>{l.desc}</div>
          </a>
        ))}
      </div>

      <div style={{ display:'grid', gridTemplateColumns:'1fr 340px', gap:16 }}>
        {/* FAQs */}
        <div className="card">
          <h3 className="card-title" style={{marginBottom:14}}>Frequently Asked Questions</h3>
          <div style={{ position:'relative', marginBottom:14 }}>
            <span style={{ position:'absolute',left:10,top:'50%',transform:'translateY(-50%)',color:'#94A3B8',fontSize:13 }}>🔍</span>
            <input className="form-input" style={{ paddingLeft:30 }} placeholder="Search FAQs..." value={search} onChange={e=>setSearch(e.target.value)} />
          </div>
          {filtered.map((faq,i)=>(
            <FaqItem key={i} faq={faq} open={openFaq===i} onToggle={()=>setOpenFaq(openFaq===i?-1:i)} />
          ))}
          {filtered.length===0 && <p style={{ fontSize:12, color:'#94A3B8', textAlign:'center', padding:20 }}>No FAQs match "{search}"</p>}
        </div>

        {/* Right column */}
        <div style={{ display:'flex', flexDirection:'column', gap:16 }}>
          {/* Contact card */}
          <div className="card" style={{ background:'linear-gradient(135deg,#0F2342,#1E3A6E)', color:'#fff' }}>
            <h3 style={{ fontSize:14, fontWeight:700, marginBottom:12 }}>📞 Contact Support</h3>
            {[
              {icon:'☎️', label:'1800-XXX-XXXX', sub:'Toll-free, 24×7'},
              {icon:'✉️', label:'support@corenova.bank', sub:'Response within 4 hours'},
              {icon:'💬', label:'Live Chat', sub:'Mon–Sat, 9 AM – 7 PM'},
            ].map((c,i)=>(
              <div key={i} style={{ display:'flex',gap:10,alignItems:'center',padding:'8px 0', borderBottom: i<2?'1px solid rgba(255,255,255,0.1)':'none' }}>
                <span style={{fontSize:16}}>{c.icon}</span>
                <div><div style={{fontSize:13,fontWeight:600}}>{c.label}</div><div style={{fontSize:11,opacity:0.6}}>{c.sub}</div></div>
              </div>
            ))}
          </div>

          {/* Raise ticket */}
          <div className="card">
            <h3 className="card-title" style={{marginBottom:14}}>Raise a Support Ticket</h3>
            <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
              <div className="form-group">
                <label className="form-label">Category</label>
                <select className="form-input form-select" value={ticket.category} onChange={e=>setTicket(t=>({...t,category:e.target.value}))}>
                  <option>General</option><option>Transaction Issue</option><option>Account Access</option><option>Technical Bug</option><option>Feature Request</option>
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Subject *</label>
                <input className="form-input" value={ticket.subject} onChange={e=>setTicket(t=>({...t,subject:e.target.value}))} placeholder="Brief summary" />
              </div>
              <div className="form-group">
                <label className="form-label">Description *</label>
                <textarea className="form-input" rows={4} value={ticket.message} onChange={e=>setTicket(t=>({...t,message:e.target.value}))} placeholder="Describe the issue in detail..." style={{ resize:'vertical', fontFamily:'inherit' }} />
              </div>
              {sent ? (
                <div style={{ background:'#D1FAE5', color:'#059669', padding:'10px 14px', borderRadius:8, fontSize:12, fontWeight:600, textAlign:'center' }}>
                  ✓ Ticket submitted! Reference: TKT{Date.now().toString().slice(-8)}
                </div>
              ) : (
                <button onClick={submitTicket} disabled={!ticket.subject||!ticket.message} className="btn btn-primary w-full" style={{ justifyContent:'center' }}>
                  Submit Ticket
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
