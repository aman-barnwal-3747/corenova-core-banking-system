/**
 * CardsPage.jsx – Debit/Credit card management
 * Image 2 sidebar item "Cards" — user-based, no hardcoded amounts
 */
import React, { useState } from 'react';

const DEMO_CARDS = [
  { id:1, type:'DEBIT',  network:'RuPay',     last4:'4821', holder:'Aman Verma',  expiry:'08/29', status:'ACTIVE',  account:'1001000001', limit:200000,  used:34500,  color:['#0F2342','#1E3A6E'] },
  { id:2, type:'CREDIT', network:'Visa',      last4:'7734', holder:'Aman Verma',  expiry:'03/28', status:'ACTIVE',  account:'1001000001', limit:300000,  used:87200,  color:['#7C3AED','#A78BFA'] },
  { id:3, type:'DEBIT',  network:'Mastercard',last4:'5512', holder:'Priya Sharma',expiry:'11/27', status:'BLOCKED', account:'1001000002', limit:500000,  used:0,      color:['#059669','#34D399'] },
];

const NETWORK_LOGO = { Visa:'VISA', Mastercard:'●●', RuPay:'RuPay' };

function CardVisual({ card }) {
  return (
    <div style={{
      background:`linear-gradient(135deg,${card.color[0]},${card.color[1]})`,
      borderRadius:16, padding:'22px 24px', color:'#fff', position:'relative',
      minHeight:170, display:'flex', flexDirection:'column', justifyContent:'space-between',
      boxShadow:'0 8px 24px rgba(0,0,0,0.15)', overflow:'hidden',
    }}>
      <div style={{ position:'absolute', top:-30,right:-30, width:120,height:120,borderRadius:'50%',background:'rgba(255,255,255,0.08)' }} />
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start' }}>
        <div style={{ fontSize:11, opacity:0.7, fontWeight:600, letterSpacing:'0.06em' }}>{card.type} CARD</div>
        <div style={{ fontSize:14, fontWeight:800, letterSpacing:'0.05em' }}>{NETWORK_LOGO[card.network]}</div>
      </div>
      <div>
        <div style={{ fontFamily:'monospace', fontSize:17, fontWeight:600, letterSpacing:'0.12em', marginBottom:10 }}>
          •••• •••• •••• {card.last4}
        </div>
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-end' }}>
          <div>
            <div style={{ fontSize:9, opacity:0.6 }}>CARD HOLDER</div>
            <div style={{ fontSize:12, fontWeight:600, letterSpacing:'0.04em' }}>{card.holder.toUpperCase()}</div>
          </div>
          <div style={{ textAlign:'right' }}>
            <div style={{ fontSize:9, opacity:0.6 }}>EXPIRES</div>
            <div style={{ fontSize:12, fontWeight:600 }}>{card.expiry}</div>
          </div>
        </div>
      </div>
      {card.status==='BLOCKED' && (
        <div style={{ position:'absolute', inset:0, background:'rgba(15,23,42,0.55)', display:'flex', alignItems:'center', justifyContent:'center', fontSize:13, fontWeight:700, letterSpacing:'0.1em' }}>
          🔒 BLOCKED
        </div>
      )}
    </div>
  );
}

export default function CardsPage() {
  const [cards, setCards] = useState(DEMO_CARDS);
  const [toast, setToast] = useState('');
  const showToast=(m)=>{setToast(m);setTimeout(()=>setToast(''),3000);};

  const toggleBlock = (id) => {
    setCards(cs=>cs.map(c=>c.id===id?{...c,status:c.status==='ACTIVE'?'BLOCKED':'ACTIVE'}:c));
    const c = cards.find(x=>x.id===id);
    showToast(`Card ending ${c.last4} ${c.status==='ACTIVE'?'blocked':'unblocked'}.`);
  };

  return (
    <div>
      {toast && <div style={{ position:'fixed',top:80,right:24,zIndex:2000,background:'#0F2342',color:'#fff',padding:'12px 20px',borderRadius:10,fontSize:13,fontWeight:500 }}>✓ {toast}</div>}
      <div className="page-header flex items-center justify-between">
        <div><h1 className="page-title">Cards</h1><p className="page-desc">Manage debit and credit cards linked to your accounts</p></div>
        <button className="btn btn-primary">+ Request New Card</button>
      </div>

      <div className="grid-4 mb-5">
        <div className="card" style={{padding:'16px 20px'}}><div style={{fontSize:22,fontWeight:800}}>{cards.length}</div><div style={{fontSize:12,color:'#64748B',marginTop:4}}>Total Cards</div></div>
        <div className="card" style={{padding:'16px 20px'}}><div style={{fontSize:22,fontWeight:800,color:'#059669'}}>{cards.filter(c=>c.status==='ACTIVE').length}</div><div style={{fontSize:12,color:'#64748B',marginTop:4}}>Active</div></div>
        <div className="card" style={{padding:'16px 20px'}}><div style={{fontSize:22,fontWeight:800,color:'#DC2626'}}>{cards.filter(c=>c.status==='BLOCKED').length}</div><div style={{fontSize:12,color:'#64748B',marginTop:4}}>Blocked</div></div>
        <div className="card" style={{padding:'16px 20px'}}><div style={{fontSize:22,fontWeight:800}}>{cards.filter(c=>c.type==='CREDIT').length}</div><div style={{fontSize:12,color:'#64748B',marginTop:4}}>Credit Cards</div></div>
      </div>

      <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill,minmax(320px,1fr))', gap:18 }}>
        {cards.map(card=>{
          const pct = card.limit ? Math.round((card.used/card.limit)*100) : 0;
          return (
            <div key={card.id}>
              <CardVisual card={card} />
              <div className="card" style={{ marginTop:12, padding:16 }}>
                <div style={{ display:'flex',justifyContent:'space-between',fontSize:12,marginBottom:8 }}>
                  <span style={{ color:'#64748B' }}>Linked Account</span>
                  <span className="mono" style={{ fontWeight:600 }}>{card.account}</span>
                </div>
                {card.type==='CREDIT' && (
                  <>
                    <div style={{ display:'flex',justifyContent:'space-between',fontSize:11,color:'#64748B',marginBottom:4 }}>
                      <span>Used: ₹{card.used.toLocaleString('en-IN')}</span><span>Limit: ₹{card.limit.toLocaleString('en-IN')}</span>
                    </div>
                    <div style={{ height:6,background:'#F1F5F9',borderRadius:99,overflow:'hidden',marginBottom:12 }}>
                      <div style={{ height:'100%',width:`${pct}%`,background: pct>80?'#EF4444':pct>50?'#F59E0B':'#10B981',borderRadius:99 }} />
                    </div>
                  </>
                )}
                <div style={{ display:'flex',gap:8 }}>
                  <button onClick={()=>toggleBlock(card.id)} className={`btn ${card.status==='ACTIVE'?'btn-danger':'btn-success'}`} style={{ flex:1,justifyContent:'center',fontSize:12 }}>
                    {card.status==='ACTIVE'?'🔒 Block Card':'🔓 Unblock'}
                  </button>
                  <button className="btn btn-ghost" style={{ flex:1,justifyContent:'center',fontSize:12 }}>⚙️ Manage PIN</button>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
