const API_BASE_URL = 'http://localhost:8080';

export const apiClient = {
  getToken() {
    return localStorage.getItem('logmonito_token');
  },

  setToken(token) {
    if (token) {
      localStorage.setItem('logmonito_token', token);
    } else {
      localStorage.removeItem('logmonito_token');
    }
  },

  async request(endpoint, options = {}) {
    const token = this.getToken();
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      ...(options.headers || {}),
    };

    const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;

    try {
      const response = await fetch(url, {
        ...options,
        headers,
      });

      if (response.status === 401) {
        // Token expired or invalid
        if (!endpoint.includes('/api/v1/auth/login')) {
          this.setToken(null);
          localStorage.removeItem('logmonito_user');
          window.dispatchEvent(new Event('auth:unauthorized'));
        }
      }

      const text = await response.text();
      let data = null;
      try {
        data = text ? JSON.parse(text) : null;
      } catch (e) {
        data = text;
      }

      if (!response.ok) {
        const errorMsg = (data && data.detail) || (typeof data === 'string' ? data : 'API Request Failed');
        throw new Error(errorMsg);
      }

      return data;
    } catch (err) {
      console.error(`API error on ${endpoint}:`, err);
      throw err;
    }
  },

  // Auth Endpoints
  async login(credentials) {
    return this.request('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    });
  },

  async register(userData) {
    return this.request('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify(userData),
    });
  },

  // Project & API Key Endpoints
  async generateApiKey(appName) {
    return this.request(`/api/v1/auth/generate?appName=${encodeURIComponent(appName)}`, {
      method: 'POST',
    });
  },

  // Telemetry Dashboard Endpoints
  async getMetrics(appName) {
    return this.request(`/api/v1/dashboard/metrics?appName=${encodeURIComponent(appName)}`);
  },

  async getErrors(appName) {
    return this.request(`/api/v1/dashboard/logs/errors?appName=${encodeURIComponent(appName)}`);
  },

  // AI Diagnostic Endpoints
  async analyzeError(logId, appName) {
    return this.request(`/api/v1/dashboard/ai/analyze/error/${encodeURIComponent(logId)}?appName=${encodeURIComponent(appName)}`);
  },

  async analyzeWindow(appName, minutes = 15) {
    return this.request(`/api/v1/dashboard/ai/analyze/window?appName=${encodeURIComponent(appName)}&minutes=${minutes}`);
  },
};
