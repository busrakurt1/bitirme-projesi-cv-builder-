import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { API_BASE_URL } from '../utils/constants';

// ================= USER MANAGER =================
export const userManager = {
  async setUser(userData) {
    await AsyncStorage.setItem('user', JSON.stringify(userData));
    if (userData.token) {
      await AsyncStorage.setItem('token', userData.token);
    }
  },

  async getUser() {
    try {
      const userStr = await AsyncStorage.getItem('user');
      return userStr ? JSON.parse(userStr) : null;
    } catch {
      return null;
    }
  },

  async removeUser() {
    await AsyncStorage.removeItem('user');
    await AsyncStorage.removeItem('token');
  },

  async isLoggedIn() {
    const token = await AsyncStorage.getItem('token');
    return !!token;
  },

  async getToken() {
    const user = await this.getUser();
    return user?.token || null;
  },

  async getUserId() {
    const user = await this.getUser();
    return user?.id ?? null;
  },

  async logout() {
    await this.removeUser();
  },
};

// ================= AXIOS INSTANCE =================
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 300000, // 5 dakika (CV optimizasyonu uzun sürebilir)
});

// ================= INTERCEPTORS =================
api.interceptors.request.use(
  async (config) => {
    const token = await userManager.getToken();
    if (token) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      await userManager.removeUser();
      // Navigation will be handled by AppNavigator
    }
    return Promise.reject(error);
  }
);

// ================= PROFILE API =================
export const profileAPI = {
  getMe: async () => {
    const userId = await userManager.getUserId();
    return api.get('/profile/me', {
      headers: {
        'X-USER-ID': userId,
      },
    });
  },

  updateMe: async (data) => {
    const userId = await userManager.getUserId();
    return api.put('/profile/me', data, {
      headers: {
        'X-USER-ID': userId,
      },
    });
  },
};

// ================= CV GENERATOR API =================
export const cvAPI = {
  generateCV: async (userId = null, jobId = null) => {
    const uid = userId || (await userManager.getUserId());
    if (!uid) {
      return Promise.reject(new Error("Kullanıcı ID'si bulunamadı"));
    }

    const params = jobId ? { jobId } : {};
    // CV optimizasyonu için özel timeout (5 dakika)
    return api.post('/cv-generator/create', null, {
      params,
      timeout: 300000, // 5 dakika
      headers: {
        'X-USER-ID': String(uid), // String'e çevir
      },
    });
  },

  generateCVWithOptions: async (userId = null, options = {}) => {
    const uid = userId || (await userManager.getUserId());
    return api.post('/cv-generator/create', options, {
      headers: {
        'X-USER-ID': uid,
      },
    });
  },

  getMyCVs: async () => {
    const userId = await userManager.getUserId();
    return api.get('/cv-generator/my-cvs', {
      headers: {
        'X-USER-ID': userId,
      },
    });
  },

  downloadCV: (cvId) =>
    api.get(`/cv-generator/download/${cvId}`, {
      responseType: 'blob',
    }),
};

// ================= AUTH API =================
// authAPI is exported from authAPI.js, but we can also export it here for convenience
export { default as authAPI } from './authAPI';

export default api;
