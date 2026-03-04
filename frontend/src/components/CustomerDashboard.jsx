// src/components/CustomerDashboard.jsx
import React, { useEffect, useState, useMemo } from "react";
import QRCode from "react-qr-code"; 

import {
  getOrders,
  changePassword,
  getAccountDetails,
  purchaseSubscription,
  SERVICE_PRICES,
  getMyStaticQRCodes,
  SERVICE_WORKFLOWS,
  payForOrder,
} from "../api";

// Import Shared Components
import { 
  WashWiseLogo, IconOrders, IconHistory, IconAccount, 
  IconSettings, IconCheck, IconCross, IconMail, IconShield 
} from "./common/Icons";
import { PaymentModal, ImageModal } from "./common/Modals";

export default function CustomerDashboard({ onLogout }) {
  const [isSidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [activeTab, setActiveTab] = useState("my-orders");
  const [orders, setOrders] = useState([]);
  const [accountInfo, setAccountInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  
  // Modal States
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalImageSrc, setModalImageSrc] = useState('');
  const [isPaymentModalOpen, setPaymentModalOpen] = useState(false);
  const [paymentDetails, setPaymentDetails] = useState(null);
  
  // New State for Enlarged QR Code
  const [selectedQrValue, setSelectedQrValue] = useState(null);

  // Settings State
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [myQrCodes, setMyQrCodes] = useState(null);

  const API_URL = import.meta.env.VITE_API_URL || "http://127.0.0.1:8000";

  const fetchData = async () => {
    try {
      if (!accountInfo) setLoading(true);
      const [{ data: ordersData }, { data: accountData }, { data: qrCodesData }] = await Promise.all([
        getOrders(),
        getAccountDetails(),
        getMyStaticQRCodes()
      ]);
      setOrders(ordersData);
      setAccountInfo(accountData);
      setMyQrCodes(qrCodesData);
    } catch (err) {
      console.error(err);
      if (!accountInfo) setMessage("Failed to load dashboard data.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 30000); 
    return () => clearInterval(interval);
  }, []);

  const activateSubscription = async (planName) => {
    try {
      await purchaseSubscription(planName);
      setMessage("Subscription updated successfully!");
      fetchData();
    } catch (error) {
      setMessage(`Error: ${error.response?.data?.detail || error.message}`);
    }
  };

  const handleConfirmOrderPayment = async (orderId) => {
    try {
      await payForOrder(orderId);
      setMessage("Payment successful!");
      fetchData();
    } catch (error) {
      setMessage(`Payment failed: ${error.response?.data?.detail || error.message}`);
    }
  };

  const handleOpenPaymentModal = (type, data) => {
    if (type === 'subscription') {
      setPaymentDetails({ 
        type: 'subscription', 
        plan: {
          name: data,
          label: data === 'standard' ? 'Standard' : 'Premium',
          price: data === 'standard' ? '5,000' : '10,000'
        }
      });
    } else {
      setPaymentDetails({ type: 'order', order: data });
    }
    setPaymentModalOpen(true);
  };

  const handleChangePasswordSubmit = async (e) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) return setMessage("New passwords do not match.");
    try {
      await changePassword(currentPassword, newPassword);
      setMessage("Password updated successfully!");
      setCurrentPassword(""); setNewPassword(""); setConfirmPassword("");
    } catch (error) {
      setMessage(`Error: ${error.response?.data?.detail || error.message}`);
    }
  };

  // Helpers
  const getStatusColor = (status) => ({ pending: "#f39c12", started: "#3498db", washing: "#8e44ad", picked_up: "#95a5a6", ready_for_pickup: "#27ae60" })[status] || "#7f8c8d";
  const getPlanColor = (plan) => ({ none: "#95a5a6", standard: "#48C9B0", premium: "#9B59B6" })[plan] || "#6c757d";
  const getPlanLabel = (plan) => ({ none: "No Plan", standard: "Standard", premium: "Premium" })[plan] || plan;
  
  const formatDate = (d) => {
    if (!d) return 'N/A';
    if (Array.isArray(d)) {
      const [year, month, day] = d;
      return new Date(year, month - 1, day).toLocaleDateString();
    }
    const dateObj = new Date(d);
    return isNaN(dateObj) ? 'N/A' : dateObj.toLocaleDateString();
  };
  
  const calculateProgress = (serviceName, currentStatus) => {
    const workflow = SERVICE_WORKFLOWS[serviceName] || [];
    const idx = workflow.indexOf(currentStatus);
    return idx === -1 ? 0 : (idx / (workflow.length - 1)) * 100;
  };

  const activeOrders = useMemo(() => orders.filter(o => !o.items.every(i => i.status === 'picked_up')), [orders]);
  const completedOrders = useMemo(() => orders.filter(o => o.items.every(i => i.status === 'picked_up')), [orders]);
  
  const flattenedActiveItems = useMemo(() => activeOrders.flatMap(o => o.items.map(i => ({
    ...i, 
    orderId: o.id, 
    orderTotalCost: o.total_cost || o.totalCost || 0,
    orderCreatedAt: o.created_at || o.createdAt, 
    paymentStatus: o.payment_status || o.paymentStatus || 'unpaid', 
    orderQrCodeUrl: o.qr_code_url || o.qrCodeUrl
  }))), [activeOrders]);
  
  const totalServicesUsed = useMemo(() => {
    if (!accountInfo || !accountInfo.monthly_services_used) return 0;
    return Object.values(accountInfo.monthly_services_used).reduce((sum, count) => sum + count, 0);
  }, [accountInfo]);

  if (loading && !accountInfo) return <div className="loading-container"><div className="spinner"></div><p>Loading...</p></div>;

  const TABS = [
    { id: "my-orders", label: "My Orders", icon: <IconOrders />, count: flattenedActiveItems.length },
    { id: "order-history", label: "Order History", icon: <IconHistory /> },
    { id: "my-account", label: "My Account", icon: <IconAccount /> },
    { id: "settings", label: "Settings", icon: <IconSettings /> },
  ];

  // FIX: Normalize the plan to lowercase to handle Spring Boot's uppercase Enums safely
  const userPlan = (accountInfo?.membership_plan || 'none').toLowerCase();

  return (
    <div className="app-container">
      {isModalOpen && <ImageModal src={modalImageSrc} onClose={() => setIsModalOpen(false)} />}
      
      <PaymentModal 
        isOpen={isPaymentModalOpen}
        onClose={() => setPaymentModalOpen(false)}
        paymentDetails={paymentDetails}
        onConfirmPayment={paymentDetails?.type === 'subscription' ? activateSubscription : handleConfirmOrderPayment}
      />

      {selectedQrValue && (
        <div className="qr-modal-overlay" onClick={() => setSelectedQrValue(null)}>
          <div className="qr-modal-content" onClick={e => e.stopPropagation()}>
            <button className="qr-modal-close" onClick={() => setSelectedQrValue(null)}>×</button>
            <h3 style={{ marginTop: 0, marginBottom: '20px', color: '#2A2F45' }}>Scan Code</h3>
            <div style={{ background: 'white', padding: '10px', borderRadius: '10px', display: 'inline-block' }}>
              <QRCode size={250} value={selectedQrValue} viewBox={`0 0 256 256`} />
            </div>
            <p style={{ marginTop: '15px', color: '#666', fontSize: '0.9rem' }}>Show this to the serviceman for quick scanning.</p>
          </div>
        </div>
      )}
      
      <header className="top-bar">
        <div className="logo-section"><WashWiseLogo small /><span className="brand-name">WashWise</span></div>
        <div className="user-section"><span className="user-role customer">Customer</span><button className="btn-logout" onClick={onLogout}>Logout</button></div>
      </header>

      <div className="dashboard-layout">
        <nav className={`sidebar ${isSidebarCollapsed ? 'collapsed' : ''}`}>
          <div className="sidebar-header">
            <label className="toggle-switch">
              <input type="checkbox" checked={!isSidebarCollapsed} onChange={() => setSidebarCollapsed(!isSidebarCollapsed)} />
              <span className="slider round"></span>
            </label>
          </div>
          <ul className="sidebar-nav">
            {TABS.map(tab => (
              <li key={tab.id}>
                <button onClick={() => setActiveTab(tab.id)} className={activeTab === tab.id ? "active" : ""}>
                  <span className="nav-icon-wrapper"><span className="nav-icon">{tab.icon}</span></span>
                  <span className="nav-label">{tab.label}</span>
                  {tab.id === 'my-orders' && tab.count > 0 && <span className="nav-count">{tab.count}</span>}
                </button>
              </li>
            ))}
          </ul>
        </nav>

        <main className="content-area">
          <h2 className="content-title">{TABS.find(t => t.id === activeTab)?.label}</h2>
          {message && <div className={`message ${message.includes("success") ? 'success' : 'error'}`}>{message}<button onClick={() => setMessage("")}>×</button></div>}
          
          {/* --- TAB: ACTIVE ORDERS --- */}
          {activeTab === "my-orders" && (
             <div className="orders-grid">
               {flattenedActiveItems.length === 0 ? <div className="card no-data-card"><h3>No Active Services</h3><p>You currently don't have any active services.</p></div> : 
               flattenedActiveItems.map(item => (
                 <div className="card order-card" key={item.id}>
                    <div className="card-header">
                      <div>
                        <h3>{(item.service_name || '').replace(/_/g, ' ')}</h3>
                        <p className="order-date">#{(item.orderId || '').substring(0,8)} • {formatDate(item.orderCreatedAt)}</p>
                      </div>
                      <span className="status-badge" style={{backgroundColor: getStatusColor(item.status)}}>{(item.status || '').replace(/_/g, ' ')}</span>
                    </div>
                    <div className="card-body">
                      <div className="item-row">
                        <span className="item-qty">{item.quantity}x items</span>
                        <span className="item-cost">₹{item.cost}</span>
                      </div>
                      <div className="progress-bar"><div className="progress-bar-inner" style={{width: `${calculateProgress(item.service_name, item.status)}%`}}></div></div>
                      
                      <div className="card-actions" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '15px' }}>
                        
                        <div className="payment-status-area">
                          {['paid', 'completed'].includes((item.paymentStatus || '').toLowerCase()) || item.orderTotalCost === 0 ? (
                            <span className="paid-badge" style={{ background: '#d4edda', color: '#155724', padding: '6px 12px', borderRadius: '6px', fontWeight: 'bold', fontSize: '0.85rem' }}>✓ Order Paid</span>
                          ) : (
                            <button className="btn-primary pay-now-btn" onClick={() => handleOpenPaymentModal('order', { id: item.orderId, total_cost: item.orderTotalCost })}>
                              Pay Order (₹{item.orderTotalCost})
                            </button>
                          )}
                        </div>

                        <div 
                          className="qr-container" 
                          style={{ background: 'white', padding: '6px', borderRadius: '6px', border: '1px solid #ddd', cursor: 'pointer', transition: 'transform 0.2s' }}
                          title="Click to Enlarge"
                          onClick={() => setSelectedQrValue(JSON.stringify({ order_id: item.orderId }))}
                          onMouseEnter={(e) => e.currentTarget.style.transform = 'scale(1.05)'}
                          onMouseLeave={(e) => e.currentTarget.style.transform = 'scale(1)'}
                        >
                            <QRCode
                                size={54}
                                style={{ height: "auto", maxWidth: "100%", width: "100%" }}
                                value={JSON.stringify({ order_id: item.orderId })}
                                viewBox={`0 0 256 256`}
                            />
                        </div>
                      </div>
                    </div>
                 </div>
               ))}
             </div>
          )}

          {/* --- TAB: HISTORY --- */}
          {activeTab === "order-history" && (
             <div className="card">
                <div className="table-container">
                  <table className="data-table">
                    <thead><tr><th>Order ID</th><th>Date</th><th>Cost</th><th>Status</th></tr></thead>
                    <tbody>
                      {completedOrders.length === 0 ? <tr><td colSpan="4" style={{textAlign: 'center'}}>No history found.</td></tr> :
                      completedOrders.map(o => (
                        <tr key={o.id}>
                          <td>{(o.id || '').substring(0,8)}...</td>
                          <td>{formatDate(o.created_at || o.createdAt)}</td>
                          <td>₹{o.total_cost || o.totalCost}</td>
                          <td><span className="status-badge" style={{backgroundColor: '#95a5a6'}}>Completed</span></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
             </div>
          )}

          {/* --- TAB: MY ACCOUNT --- */}
          {activeTab === "my-account" && accountInfo && (
            <div className="account-container">
                <div className="account-grid">
                    <div className="card profile-details-card">
                        <h3 className="card-title">Profile Details</h3>
                        <div className="detail-item"><div className="detail-icon"><IconAccount/></div><div className="detail-text"><span className="detail-label">Username</span><span className="detail-value">{accountInfo.username}</span></div></div>
                        <div className="detail-item"><div className="detail-icon"><IconMail/></div><div className="detail-text"><span className="detail-label">Email</span><span className="detail-value">{accountInfo.email}</span></div></div>
                        <div className="detail-item"><div className="detail-icon"><IconShield/></div><div className="detail-text"><span className="detail-label">Role</span><span className="detail-value role-tag">{accountInfo.role}</span></div></div>
                    </div>
                    <div className="card qr-card">
                        <h3 className="card-title">Your Static QR Code</h3>
                        {accountInfo.id ? (
                            <div 
                              className="static-qr-container" 
                              style={{ background: 'white', padding: '16px', display: 'inline-block', borderRadius: '8px', cursor: 'pointer', border: '1px solid #eee' }}
                              onClick={() => setSelectedQrValue(JSON.stringify({ user_id: accountInfo.id }))}
                              title="Click to Enlarge"
                            >
                                <QRCode
                                    size={150}
                                    style={{ height: "auto", maxWidth: "100%", width: "100%" }}
                                    value={JSON.stringify({ user_id: accountInfo.id })}
                                    viewBox={`0 0 256 256`}
                                />
                                <p style={{textAlign: 'center', fontSize: '0.8rem', color: '#888', marginTop: '10px', marginBottom: 0}}>Click to enlarge</p>
                            </div>
                        ) : <p>Loading QR...</p>}
                    </div>
                </div>

                <div className="card subscription-card" style={{'--plan-color': getPlanColor(userPlan)}}>
                    <h3 className="card-title">Subscription Status</h3>
                    <div className="plan-badge">{getPlanLabel(userPlan)} Plan</div>
                    {userPlan !== 'none' ? (
                        <>
                            <p>Your plan is active and renews on <strong>{formatDate(new Date(new Date().setFullYear(new Date().getFullYear() + 1)))}</strong>.</p>
                            <div className="usage-meter">
                                <div className="usage-label"><span>Total Monthly Services Used</span><strong>{totalServicesUsed}</strong></div>
                                <p style={{fontSize: '0.8rem', color: '#666', marginTop: '10px'}}>Your plan includes up to 4 uses per service type each month.</p>
                            </div>
                        </>
                    ) : (
                        <p>You are not subscribed to any plan. Choose a plan below for exclusive benefits!</p>
                    )}
                </div>

                <div className="card plans-section-card">
                    <h3 className="card-title">Upgrade Your Plan</h3>
                    <div className="plans-container">
                        
                        {/* STANDARD PLAN CARD */}
                        <div className={`plan-card standard ${userPlan === 'standard' ? 'active' : ''} ${userPlan === 'premium' ? 'unavailable' : ''}`}>
                            {userPlan === 'standard' && <div className="current-plan-banner">Current Plan</div>}
                            <div className="plan-header"><h4>Standard Plan</h4><p className="plan-price"><span>₹5,000</span>/year</p></div>
                            <div className="plan-body">
                                <ul className="plan-features">
                                    <li className="plan-feature-item"><IconCheck /><span>4 uses per service, per month</span></li>
                                    <li className="plan-feature-item"><IconCheck /><span>Covers Wash & Fold</span></li>
                                    <li className="plan-feature-item"><IconCheck /><span>Covers Wash & Iron</span></li>
                                    <li className="plan-feature-item"><IconCross /><span>No Premium Services</span></li>
                                </ul>
                                <button 
                                  className="btn-primary btn-purchase" 
                                  onClick={() => handleOpenPaymentModal('subscription', 'standard')} 
                                  disabled={loading || userPlan === 'standard' || userPlan === 'premium'}
                                >
                                    {userPlan === 'standard' ? 'Plan Active' : userPlan === 'premium' ? 'Unavailable' : 'Choose Standard'}
                                </button>
                            </div>
                        </div>

                        {/* PREMIUM PLAN CARD */}
                        <div className={`plan-card premium ${userPlan === 'premium' ? 'active' : ''}`}>
                            {userPlan === 'premium' && <div className="current-plan-banner">Current Plan</div>}
                            <div className="plan-header"><h4>Premium Plan</h4><p className="plan-price"><span>₹10,000</span>/year</p></div>
                            <div className="plan-body">
                                <ul className="plan-features">
                                    <li className="plan-feature-item"><IconCheck /><span>4 uses per service, per month</span></li>
                                    <li className="plan-feature-item"><IconCheck /><span>Covers Wash & Fold</span></li>
                                    <li className="plan-feature-item"><IconCheck /><span>Covers All Premium Services</span></li>
                                    <li className="plan-feature-item"><IconCheck /><span>Priority Support</span></li>
                                </ul>
                                <button 
                                  className="btn-primary btn-purchase" 
                                  onClick={() => handleOpenPaymentModal('subscription', 'premium')} 
                                  disabled={loading || userPlan === 'premium'}
                                >
                                    {userPlan === 'premium' ? 'Plan Active' : userPlan === 'standard' ? 'Upgrade to Premium' : 'Choose Premium'}
                                </button>
                            </div>
                        </div>

                    </div>
                </div>
            </div>
          )}

          {/* --- TAB: SETTINGS --- */}
          {activeTab === "settings" && (
             <div className="card">
               <h3 className="card-title">Change Password</h3>
               <form onSubmit={handleChangePasswordSubmit}>
                 <div className="form-group"><label>Current Password</label><input type="password" value={currentPassword} onChange={e=>setCurrentPassword(e.target.value)} required /></div>
                 <div className="form-group"><label>New Password</label><input type="password" value={newPassword} onChange={e=>setNewPassword(e.target.value)} required /></div>
                 <div className="form-group"><label>Confirm New Password</label><input type="password" value={confirmPassword} onChange={e=>setConfirmPassword(e.target.value)} required /></div>
                 <button className="btn-primary">Update Password</button>
               </form>
             </div>
          )}
        </main>
      </div>
      
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;700&display=swap');
        @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@700&display=swap');
        body { margin: 0; font-family: 'Inter', sans-serif; background-color: #f4f7f9; }
        * { box-sizing: border-box; }
        
        .qr-modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 2000; backdrop-filter: blur(3px); }
        .qr-modal-content { background: white; padding: 30px 40px; border-radius: 16px; text-align: center; position: relative; max-width: 90%; box-shadow: 0 10px 30px rgba(0,0,0,0.2); animation: scaleIn 0.2s ease-out; }
        .qr-modal-close { position: absolute; top: 12px; right: 15px; background: none; border: none; font-size: 1.8rem; cursor: pointer; color: #888; transition: color 0.2s; }
        .qr-modal-close:hover { color: #333; }
        @keyframes scaleIn { from { transform: scale(0.9); opacity: 0; } to { transform: scale(1); opacity: 1; } }
      `}</style>
      <style>{`
        .app-container { position: fixed; top: 0; left: 0; width: 100%; height: 100%; }
        .top-bar { position: absolute; top: 0; left: 0; right: 0; height: 65px; background-color: #ffffff; display: flex; align-items: center; justify-content: space-between; padding: 0 30px; border-bottom: 1px solid #dee2e6; z-index: 1000; }
        .logo-section { display: flex; align-items: center; gap: 15px; }
        .brand-name { font-family: 'Playfair Display', serif; font-size: 1.5rem; font-weight: 700; color: #2A2F45; }
        .user-section { display: flex; align-items: center; gap: 15px; }
        .user-role { font-weight: 600; padding: 6px 12px; border-radius: 6px; background-color: #d4edda; color: #155724; }
        .btn-logout { padding: 8px 15px; background: #f8f9fa; color: #dc3545; border: 1px solid #dee2e6; border-radius: 8px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
        .btn-logout:hover { background: #dc3545; color: white; }

        .dashboard-layout { display: flex; height: 100%; padding-top: 65px; }
        
        .sidebar { width: 260px; background-color: #2A2F45; color: #fff; padding: 15px 10px; transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1); display: flex; flex-direction: column; overflow: hidden; flex-shrink: 0; }
        .sidebar.collapsed { width: 80px; }
        .sidebar-header { display: flex; justify-content: flex-end; padding: 0 5px; margin-bottom: 20px; min-height: 30px; }
        .sidebar.collapsed .sidebar-header { justify-content: center; padding: 0; }
        
        .toggle-switch { position: relative; display: inline-block; width: 50px; height: 28px; flex-shrink: 0; }
        .toggle-switch input { opacity: 0; width: 0; height: 0; }
        .slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #556b8d; transition: .4s; border-radius: 34px; }
        .slider:before { position: absolute; content: ""; height: 20px; width: 20px; left: 4px; bottom: 4px; background-color: white; transition: .4s; border-radius: 50%; }
        input:checked + .slider { background-color: #48C9B0; }
        input:checked + .slider:before { transform: translateX(22px); }

        .sidebar-nav { list-style: none; padding: 0; margin: 0; }
        .sidebar-nav li { margin-bottom: 8px; }
        .sidebar-nav button { display: flex; align-items: center; gap: 15px; width: 100%; padding: 12px 15px; border-radius: 8px; background: transparent; border: none; color: rgba(255, 255, 255, 0.7); font-size: 1rem; font-weight: 500; cursor: pointer; text-align: left; transition: all 0.2s ease; white-space: nowrap; }
        .sidebar-nav button:hover { background-color: rgba(255, 255, 255, 0.1); color: #fff; }
        .sidebar-nav button.active { color: #fff; font-weight: 600; background-color: rgba(255, 255, 255, 0.05); }
        .sidebar-nav button.active .nav-icon-wrapper { background-color: #48C9B0; color: white; }
        .nav-icon-wrapper { display: grid; place-items: center; width: 32px; height: 32px; border-radius: 50%; transition: background 0.2s; flex-shrink: 0; }
        .nav-icon { width: 20px; height: 20px; }
        .sidebar.collapsed .nav-label, .sidebar.collapsed .nav-count { display: none; }
        .sidebar.collapsed .sidebar-nav button { justify-content: center; padding: 0; width: 48px; height: 48px; border-radius: 50%; margin: 0 auto; }

        .content-area { flex-grow: 1; padding: 30px; overflow-y: auto; background-color: #f4f7f9; }
        .content-title { margin-bottom: 30px; font-size: 2rem; color: #2A2F45; }
        
        .orders-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 20px; }
        .account-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(350px, 1fr)); gap: 20px; margin-bottom: 25px; }
        
        .card { background: white; border-radius: 12px; padding: 25px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); transition: transform 0.2s; margin-bottom: 20px; }
        .order-card:hover { transform: translateY(-3px); box-shadow: 0 8px 25px rgba(0,0,0,0.1); }
        .card-header { display: flex; justify-content: space-between; align-items: start; margin-bottom: 15px; }
        .card-header h3 { margin: 0; font-size: 1.2rem; color: #333; text-transform: capitalize; }
        .order-date { font-size: 0.85rem; color: #888; margin-top: 5px; }
        .status-badge { padding: 5px 12px; border-radius: 20px; font-size: 0.75rem; font-weight: bold; color: white; text-transform: uppercase; }
        .card-body { display: flex; flex-direction: column; gap: 15px; }
        .item-row { display: flex; justify-content: space-between; font-weight: 500; color: #555; }
        .item-cost { font-weight: 700; color: #2A2F45; }
        .progress-bar { width: 100%; height: 8px; background: #eee; border-radius: 4px; overflow: hidden; }
        .progress-bar-inner { height: 100%; background: linear-gradient(90deg, #48C9B0, #3498db); transition: width 0.5s; }
        .btn-primary { padding: 10px 20px; background: #48C9B0; color: white; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; transition: background 0.2s; }
        .btn-primary:hover:not(:disabled) { background: #40B39E; transform: translateY(-1px); }
        .btn-primary:disabled { background: #ccc; cursor: not-allowed; }
        
        .profile-details-card .detail-item { display: flex; align-items: center; gap: 20px; padding: 15px 0; border-bottom: 1px solid #f1f3f5; }
        .profile-details-card .detail-item:last-child { border-bottom: none; }
        .detail-icon { display: grid; place-items: center; width: 40px; height: 40px; border-radius: 50%; background-color: #e8f8f5; color: #48C9B0; flex-shrink: 0; }
        .detail-icon svg { width: 22px; height: 22px; }
        .detail-text { display: flex; flex-direction: column; gap: 2px; }
        .detail-label { font-weight: 500; color: #666; font-size: 0.85rem; }
        .detail-value { font-weight: 600; color: #333; }
        .role-tag { text-transform: capitalize; background-color: #e8f8f5; color: #48C9B0; padding: 4px 10px; border-radius: 6px; font-size: 0.9rem; align-self: flex-start; }
        
        .subscription-card { background-image: linear-gradient(135deg, #fff 70%, color-mix(in srgb, var(--plan-color) 20%, transparent)); }
        .subscription-card p { color: #666; line-height: 1.6; margin-top: 0; }
        .subscription-card p strong { color: #2A2F45; }
        .plan-badge { display: inline-block; padding: 10px 20px; border-radius: 8px; font-size: 1.2rem; font-weight: 700; color: #fff; background-color: var(--plan-color); margin-bottom: 20px; }
        .usage-meter { margin-top: 25px; }
        .usage-label { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 0.9rem; color: #555; }
        
        .plans-section-card { background-color: #f8f9fa; border: 1px solid #e9ecef; }
        .plans-container { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 25px; }
        .plan-card { position: relative; border: 1px solid #dee2e6; border-radius: 16px; overflow: hidden; transition: all 0.3s ease; display: flex; flex-direction: column; background-color: #fff; }
        .plan-card:not(.unavailable):hover { transform: translateY(-5px); box-shadow: 0 8px 25px rgba(0,0,0,0.08); }
        .plan-card.active { border-color: transparent; box-shadow: 0 0 0 3px #48C9B0, 0 8px 25px rgba(0,0,0,0.1); }
        .plan-card.standard.active { box-shadow: 0 0 0 3px #48C9B0; }
        .plan-card.premium.active { box-shadow: 0 0 0 3px #9B59B6; }
        .plan-card.unavailable { opacity: 0.6; pointer-events: none; }
        
        .plan-card.standard .plan-header { background: linear-gradient(135deg, #48C9B0, #76D7C4); }
        .plan-card.premium .plan-header { background: linear-gradient(135deg, #9B59B6, #C39BD3); }
        .plan-header { padding: 25px; color: white; text-align: center; }
        .plan-header h4 { margin: 0; font-size: 1.5rem; font-weight: 700; }
        .plan-price span { font-size: 2.5rem; font-weight: 800; }
        .plan-body { padding: 25px; display: flex; flex-direction: column; flex-grow: 1; }
        .plan-features { list-style: none; padding: 0; margin: 0 0 25px 0; flex-grow: 1; }
        .plan-feature-item { display: flex; align-items: center; gap: 12px; margin-bottom: 15px; color: #495057; font-size: 0.95rem; }
        .plan-feature-item svg { width: 20px; height: 20px; flex-shrink: 0; }
        .current-plan-banner { position: absolute; top: 15px; right: -45px; background-color: #48C9B0; color: white; padding: 6px 40px; transform: rotate(45deg); font-size: 0.75rem; font-weight: bold; text-transform: uppercase; z-index: 2; }
        .plan-card.premium .current-plan-banner { background-color: #9B59B6; }
        .btn-purchase { width: 100%; margin-top: auto; }
        .plan-card.standard .btn-purchase { background-color: #48C9B0; }
        .plan-card.premium .btn-purchase { background-color: #9B59B6; }
        :global(.icon-check) { stroke: #28a745; }
        :global(.icon-cross) { stroke: #dc3545; }

        .table-container { overflow-x: auto; }
        .data-table { width: 100%; border-collapse: collapse; }
        .data-table th { text-align: left; padding: 15px; background: #f8f9fa; color: #555; font-weight: 600; border-bottom: 2px solid #eee; }
        .data-table td { padding: 15px; border-bottom: 1px solid #eee; color: #333; }
        
        .form-group { margin-bottom: 20px; }
        .form-group label { display: block; margin-bottom: 8px; font-weight: 500; color: #555; }
        .form-group input { width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 1rem; }
        .form-group input:focus { outline: none; border-color: #48C9B0; box-shadow: 0 0 0 3px rgba(72, 201, 176, 0.2); }
        
        .loading-container { height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 20px; color: #666; }
        .spinner { width: 40px; height: 40px; border: 4px solid #eee; border-top: 4px solid #48C9B0; border-radius: 50%; animation: spin 1s linear infinite; }
        @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
        
        .message { padding: 12px 20px; margin-bottom: 20px; border-radius: 8px; display: flex; justify-content: space-between; align-items: center; }
        .message.success { background: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
        .message.error { background: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        .message button { background: none; border: none; font-size: 1.2rem; cursor: pointer; color: inherit; }
      `}</style>
    </div>
  );
}