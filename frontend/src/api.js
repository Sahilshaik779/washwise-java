import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api/v1";

const api = axios.create({
  baseURL: API_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Automatically add token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("access_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Catch expired tokens automatically and redirect to login
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      console.error("Session expired. Logging out.");
      localStorage.removeItem("access_token");
      
      // Redirect to login page
      window.location.href = "/";
    }
    return Promise.reject(error);
  }
);

// --- HARDCODED DEFAULTS (Fallbacks until backend fetch works) ---
export const SERVICE_PRICES = {
  "wash_and_fold": { name: "Wash and Fold", price: 10 },
  "wash_and_iron": { name: "Wash and Iron", price: 15 },
  "premium_wash": { name: "Premium Wash", price: 25 },
  "dry_cleaning": { name: "Dry Cleaning", price: 50 },
  "steam_iron": { name: "Steam Iron", price: 12 },
};

export const SERVICE_WORKFLOWS = {
    "wash_and_fold": ["pending", "started", "washing", "folding", "ready_for_pickup", "picked_up"],
    "wash_and_iron": ["pending", "started", "washing", "ironing", "ready_for_pickup", "picked_up"],
    "premium_wash": ["pending", "started", "inspection", "pre_treatment", "washing", "drying", "quality_check", "ready_for_pickup", "picked_up"],
    "dry_cleaning": ["pending", "started", "tagging", "pre_treatment", "dry_cleaning", "pressing", "finishing", "ready_for_pickup", "picked_up"],
    "steam_iron": ["pending", "started", "steaming", "pressing", "finishing", "ready_for_pickup", "picked_up"]
};

// --- AUTHENTICATION ---
export const loginUser = async (username, password) => {
  const response = await api.post("/auth/login", { username, password });
  return response.data;
};

// Added staffSecret for the dual-portal registration
export const registerUser = (username, email, password, role, staffSecret = null) => 
  api.post("/auth/register", { username, email, password, role, staffSecret });

export const requestPasswordReset = (email) => 
  api.post("/auth/forgot-password", { email });

export const resetPassword = (token, newPassword) => 
  api.post("/auth/reset-password", { token, new_password: newPassword });

export const changePassword = (currentPassword, newPassword) => 
  api.put("/users/me/password", { current_password: currentPassword, new_password: newPassword });


// --- USERS & ACCOUNTS ---
export const getAccountDetails = () => api.get("/users/me");

export const getAllUsers = () => api.get("/users"); // Admin only (Removed trailing slash)

export const getActiveOrdersForUser = (userId) => 
  api.get(`/users/${userId}/active-orders`);

export const getMyStaticQRCodes = () => api.get("/users/me/qrcodes");

// --- SUBSCRIPTIONS ---
export const purchaseSubscription = (plan) => 
  api.put("/users/me/subscribe", { plan });


// --- ORDERS ---
export const getOrders = () => api.get("/orders"); // Staff only (Removed trailing slash)

// Added specific endpoint for customers to see their own orders
export const getMyOrders = () => api.get("/orders/me"); 

export const createOrder = (orderData) => 
  api.post("/orders", orderData); // Removed trailing slash

export const getOrderByQr = (orderId) => 
  api.get(`/orders/qr/${orderId}`);

export const payForOrder = (orderId) => 
  api.put(`/orders/${orderId}/pay`);

export const updateOrderItemStatus = (itemId, status) => 
  api.put(`/orders/items/${itemId}/status`, { status });


// --- SYSTEM CONFIG ---
export const fetchSystemConfig = () => api.get("/system/config");

export default api;