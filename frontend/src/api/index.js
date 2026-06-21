/**
 * ================================================================
 *  api/index.js – Centralized Axios API Client
 *
 *  Handles all HTTP communication with the Spring Boot backend.
 *
 *  Features:
 *   • Base URL from environment (default: /api)
 *   • Request interceptor: injects Bearer JWT on every call
 *   • Response interceptor: auto-refreshes token on 401
 *   • Typed service modules: auth, accounts, transactions, dashboard
 * ================================================================
 */
import axios from 'axios';

// ── Axios base instance ──────────────────────────────────────────
const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
});

// ── Request interceptor: attach JWT token ────────────────────────
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('cnb_access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ── Response interceptor: handle 401 token expiry ───────────────
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // If 401 and not already retrying, attempt token refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      const refreshToken = localStorage.getItem('cnb_refresh_token');
      if (refreshToken) {
        try {
          const { data } = await axios.post('/api/auth/refresh-token', { refreshToken });
          localStorage.setItem('cnb_access_token', data.accessToken);
          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
          return api(originalRequest);
        } catch (_) {
          // Refresh failed — force logout
          localStorage.clear();
          window.location.href = '/login';
        }
      } else {
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);

// ════════════════════════════════════════════════════════════════
//  AUTH SERVICE
// ════════════════════════════════════════════════════════════════
export const authApi = {

  /**
   * Authenticates user and stores JWT tokens in localStorage.
   * @param {string} username
   * @param {string} password
   * @returns {object} AuthResponse with user info and role
   */
  login: async (username, password) => {
    const { data } = await api.post('/auth/login', { username, password });
    // Store tokens and user info
    localStorage.setItem('cnb_access_token',  data.accessToken);
    localStorage.setItem('cnb_refresh_token', data.refreshToken);
    localStorage.setItem('cnb_user',          JSON.stringify({
      username:    data.username,
      fullName:    data.fullName,
      role:        data.role,
      branchCode:  data.branchCode,
      employeeId:  data.employeeId,
      email:       data.email,
    }));
    return data;
  },

  logout: () => {
    localStorage.clear();
    window.location.href = '/login';
  },

  getCurrentUser: () => {
    const stored = localStorage.getItem('cnb_user');
    return stored ? JSON.parse(stored) : null;
  },

  isAuthenticated: () => !!localStorage.getItem('cnb_access_token'),

  changePassword: (oldPassword, newPassword) =>
    api.post('/auth/change-password', { oldPassword, newPassword }),
};

// ════════════════════════════════════════════════════════════════
//  DASHBOARD SERVICE
// ════════════════════════════════════════════════════════════════
export const dashboardApi = {
  /** Full dashboard data: KPIs + charts + recent transactions */
  getData:     () => api.get('/dashboard'),

  /** Lightweight quick-stats for the header bar */
  getQuickStats: () => api.get('/dashboard/quick-stats'),
};

// ════════════════════════════════════════════════════════════════
//  ACCOUNT SERVICE
// ════════════════════════════════════════════════════════════════
export const accountApi = {
  /** Open a new account for a customer CIF */
  open: (cifNumber, accountType, initialDeposit, branchCode) =>
    api.post('/accounts/open', { cifNumber, accountType, initialDeposit, branchCode }),

  /** Get account details by account number */
  getAccount: (accountNumber) => api.get(`/accounts/${accountNumber}`),

  /** Get all accounts for a customer CIF */
  getCustomerAccounts: (cifNumber) => api.get(`/accounts/customer/${cifNumber}`),

  /** Freeze an account */
  freeze: (accountNumber, reason) =>
    api.post(`/accounts/${accountNumber}/freeze`, null, { params: { reason } }),

  /** Unfreeze an account */
  unfreeze: (accountNumber, reason) =>
    api.post(`/accounts/${accountNumber}/unfreeze`, null, { params: { reason } }),

  /** Close an account */
  close: (accountNumber, reason) =>
    api.post(`/accounts/${accountNumber}/close`, null, { params: { reason } }),
};

// ════════════════════════════════════════════════════════════════
//  TRANSACTION SERVICE
// ════════════════════════════════════════════════════════════════
export const transactionApi = {
  /** Internal transfer between two CoreNova accounts */
  internalTransfer: (fromAccountNumber, toAccountNumber, amount, remarks) =>
    api.post('/transactions/transfer', { fromAccountNumber, toAccountNumber, amount, remarks }),

  /** UPI payment */
  upiPayment: (accountNumber, counterpartyUpiId, amount, remarks) =>
    api.post('/transactions/upi', { accountNumber, counterpartyUpiId, amount, remarks }),

  /** NEFT transfer */
  neftTransfer: (payload) => api.post('/transactions/neft', payload),

  /** RTGS transfer */
  rtgsTransfer: (payload) => api.post('/transactions/rtgs', payload),

  /** IMPS transfer */
  impsTransfer: (payload) => api.post('/transactions/imps', payload),

  /** Get paginated transaction history */
  getHistory: (accountNumber, page = 0, size = 20) =>
    api.get(`/transactions/${accountNumber}/history`, { params: { page, size } }),

  /** Get transaction by reference number */
  getByRef: (referenceNumber) => api.get(`/transactions/ref/${referenceNumber}`),

  /** Reverse a transaction (MANAGER/ADMIN only) */
  reverse: (referenceNumber, remarks) =>
    api.post(`/transactions/${referenceNumber}/reverse`, null, { params: { remarks } }),
};

// ════════════════════════════════════════════════════════════════
//  CUSTOMER SERVICE
// ════════════════════════════════════════════════════════════════
export const customerApi = {
  search: (query, page = 0) =>
    api.get('/customers/search', { params: { query, page } }),

  getByCif: (cifNumber) => api.get(`/customers/${cifNumber}`),

  create: (customerData) => api.post('/customers', customerData),

  updateKycStatus: (cifNumber, status, remarks) =>
    api.post(`/customers/${cifNumber}/kyc`, { status, remarks }),
};

export default api;

// ════════════════════════════════════════════════════════════════
//  BENEFICIARY SERVICE — PDF §8 Payment Gateway
// ════════════════════════════════════════════════════════════════
export const beneficiaryApi = {
  add:    (payload) => api.post('/beneficiaries', payload),
  list:   (accountNumber) => api.get(`/beneficiaries/account/${accountNumber}`),
  listByType: (accountNumber, type) => api.get(`/beneficiaries/account/${accountNumber}/type/${type}`),
  remove: (id) => api.delete(`/beneficiaries/${id}`),
};

// ════════════════════════════════════════════════════════════════
//  LOAN SERVICE — Image 2 sidebar "Loans"
// ════════════════════════════════════════════════════════════════
export const loanApi = {
  apply:      (payload) => api.post('/loans/apply', payload),
  approve:    (loanNumber, remarks) => api.post(`/loans/${loanNumber}/approve`, null, { params:{remarks} }),
  reject:     (loanNumber, rejectionReason) => api.post(`/loans/${loanNumber}/reject`, null, { params:{rejectionReason} }),
  disburse:   (loanNumber, creditAccountNumber) => api.post(`/loans/${loanNumber}/disburse`, null, { params:{creditAccountNumber} }),
  getByNumber:(loanNumber) => api.get(`/loans/${loanNumber}`),
  getByCustomer: (cifNumber) => api.get(`/loans/customer/${cifNumber}`),
  getByStatus:(status, page=0, size=20) => api.get(`/loans/status/${status}`, { params:{page,size} }),
  portfolio:  () => api.get('/loans/portfolio/summary'),
  calcEmi:    (principal, annualRate, tenureMonths) => api.get('/loans/emi/calculate', { params:{principal, annualRate, tenureMonths} }),
};

// ════════════════════════════════════════════════════════════════
//  APPROVAL (MAKER-CHECKER) SERVICE — PDF §9
// ════════════════════════════════════════════════════════════════
export const approvalApi = {
  create:     (payload) => api.post('/approvals/create', payload),
  pending:    (page=0, size=20) => api.get('/approvals/pending', { params:{page,size} }),
  myRequests: (page=0, size=20) => api.get('/approvals/my-requests', { params:{page,size} }),
  approve:    (ref, remarks)  => api.post(`/approvals/${ref}/approve`, null, { params:{remarks} }),
  reject:     (ref, rejectionReason) => api.post(`/approvals/${ref}/reject`, null, { params:{rejectionReason} }),
  returnToMaker: (ref, remarks) => api.post(`/approvals/${ref}/return`, null, { params:{remarks} }),
  getByRef:   (ref) => api.get(`/approvals/${ref}`),
  stats:      () => api.get('/approvals/stats'),
};

// ════════════════════════════════════════════════════════════════
//  USER & ROLE SERVICE — PDF §4 Roles (ADMIN/MANAGER/TELLER/CUSTOMER)
// ════════════════════════════════════════════════════════════════
export const userApi = {
  list:       (page=0, size=20) => api.get('/users', { params:{page,size} }),
  create:     (payload) => api.post('/users', payload),
  getById:    (id) => api.get(`/users/${id}`),
  changeRole: (id, role) => api.put(`/users/${id}/role`, null, { params:{role} }),
  lock:       (id) => api.post(`/users/${id}/lock`),
  unlock:     (id) => api.post(`/users/${id}/unlock`),
  deactivate: (id) => api.delete(`/users/${id}`),
  stats:      () => api.get('/users/stats'),
};

// ════════════════════════════════════════════════════════════════
//  AUDIT LOG SERVICE — PDF §10
// ════════════════════════════════════════════════════════════════
export const auditApi = {
  all:        (page=0, size=50) => api.get('/audit-logs', { params:{page,size} }),
  byUser:     (username, page=0, size=50) => api.get(`/audit-logs/user/${username}`, { params:{page,size} }),
  byEntity:   (entityType, entityId, page=0, size=50) => api.get(`/audit-logs/entity/${entityType}/${entityId}`, { params:{page,size} }),
  byDateRange:(from, to, page=0, size=50) => api.get('/audit-logs/date-range', { params:{from,to,page,size} }),
};
