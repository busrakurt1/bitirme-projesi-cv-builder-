import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api";

// ================= USER MANAGER =================
export const userManager = {
  setUser: (u) => localStorage.setItem("user", JSON.stringify(u)),

  getUser: () => {
    try {
      const userStr = localStorage.getItem("user");
      return userStr ? JSON.parse(userStr) : null;
    } catch {
      return null;
    }
  },

  removeUser: () => localStorage.removeItem("user"),
  isLoggedIn: () => !!localStorage.getItem("user"),

  getToken: () => userManager.getUser()?.token,
  getUserId: () => userManager.getUser()?.id ?? null,
};

// ================= AXIOS INSTANCE =================
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
  timeout: 300000, // 5 dakika (CV optimizasyonu uzun sürebilir)
});

// ================= INTERCEPTORS =================
api.interceptors.request.use(
  (config) => {
    const token = userManager.getToken();
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
  (error) => {
    if (error.response?.status === 401) {
      userManager.removeUser();
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

// ================= AUTH API =================
export const authAPI = {
  login: (email, password) => api.post("/auth/login", { email, password }),
  register: (userData) => api.post("/auth/register", userData),
  healthCheck: () => api.get("/auth/health"),
};

// ================= JOB API =================
export const jobAPI = {
  // İlan analiz et
  analyzePosting: (userId, url) => {
    const uid = userId || userManager.getUserId();
    return api.post(
      "/job/analyze",
      { url },
      {
        headers: {
          "X-USER-ID": uid,
        },
      }
    );
  },

  // ✅ MARKET ANALYSIS – DOĞRU HAL
  performMarketAnalysis: (area, userId) => {
    const uid = userId || userManager.getUserId();

    return api.post(
      "/job/market-analysis",
      {
        area: (area ?? "").trim(), // BODY: SADECE area
      },
      {
        headers: {
          "X-USER-ID": uid, // HEADER: userId BURADA
        },
      }
    );
  },

  getUserJobs: (userId) => {
    const uid = userId || userManager.getUserId();
    return api.get(`/job/user/${uid}`);
  },

  getJobById: (jobId) => {
    return api.get(`/job/${jobId}`);
  },

  getLatestJob: (userId) => {
    const uid = userId || userManager.getUserId();
    return api.get(`/job/latest/${uid}`);
  },
};

// ================= USER API =================
export const userAPI = {
  getAllUsers: () => api.get("/users"),
  getUserById: (id) => api.get(`/users/${id}`),
  createUser: (data) => api.post("/users", data),
  updateUser: (id, data) => api.put(`/users/${id}`, data),
  deleteUser: (id) => api.delete(`/users/${id}`),
  checkEmail: (email) => api.get("/users/check-email", { params: { email } }),
  healthCheck: () => api.get("/users/health"),
};

// ================= PROFILE API =================
export const profileAPI = {
  getMe: () =>
    api.get("/profile/me", {
      headers: {
        "X-USER-ID": userManager.getUserId(),
      },
    }),

  updateMe: (data) =>
    api.put("/profile/me", data, {
      headers: {
        "X-USER-ID": userManager.getUserId(),
      },
    }),
};

// ================= CV GENERATOR API =================
export const cvAPI = {
  generateCV: (userId, jobId = null) => {
    const uid = userId || userManager.getUserId();
    if (!uid) {
      return Promise.reject(new Error("Kullanıcı ID'si bulunamadı"));
    }
    
    const params = jobId ? { jobId } : {};
    // CV optimizasyonu için özel timeout (5 dakika)
    return api.post("/cv-generator/create", null, {
      params,
      timeout: 300000, // 5 dakika
      headers: {
        "X-USER-ID": String(uid), // String'e çevir
      },
    });
  },

  generateCVWithOptions: (userId, options = {}) => {
    const uid = userId || userManager.getUserId();
    return api.post("/cv-generator/create", options, {
      headers: {
        "X-USER-ID": uid,
      },
    });
  },

  getMyCVs: () =>
    api.get("/cv-generator/my-cvs", {
      headers: {
        "X-USER-ID": userManager.getUserId(),
      },
    }),

  downloadCV: (cvId) =>
    api.get(`/cv-generator/download/${cvId}`, {
      responseType: "blob",
    }),
};

export const cvGeneratorAPI = cvAPI;

// ================= TEMPLATE API =================
export const templateAPI = {
  getAll: () => api.get("/templates"),
  getById: (id) => api.get(`/templates/${id}`),
  create: (data) => api.post("/templates", data),
  update: (id, data) => api.put(`/templates/${id}`, data),
  delete: (id) => api.delete(`/templates/${id}`),
};

// ================= ANALYSIS API =================
export const analysisAPI = {
  analyzeJobMatch: (data) => api.post("/analysis/job-match", data),
  getAnalysisHistory: (userId) => {
    const uid = userId || userManager.getUserId();
    return api.get(`/analysis/history/${uid}`);
  },
  getAnalysisById: (id) => api.get(`/analysis/${id}`),
  healthCheck: () => api.get("/analysis/health"),
};

// ================= CHATBOT API =================
export const chatbotAPI = {
  sendMessage: (message, userId) => {
    const uid = userId || userManager.getUserId();
    if (!uid) {
      return Promise.reject(new Error("Kullanıcı ID bulunamadı. Lütfen giriş yapın."));
    }
    return api.post(
      "/chatbot/message",
      { message, userId: uid },
      {
        headers: {
          "X-USER-ID": uid,
        },
      }
    );
  },
  getHistory: () => {
    const userId = userManager.getUserId();
    if (!userId) {
      return Promise.reject(new Error("Kullanıcı ID bulunamadı. Lütfen giriş yapın."));
    }
    return api.get("/chatbot/history", {
      headers: {
        "X-USER-ID": userId,
      },
    });
  },
  clearHistory: () => {
    const userId = userManager.getUserId();
    if (!userId) {
      return Promise.reject(new Error("Kullanıcı ID bulunamadı. Lütfen giriş yapın."));
    }
    return api.delete("/chatbot/history", {
      headers: {
        "X-USER-ID": userId,
      },
    });
  },
  healthCheck: () => api.get("/chatbot/health"),
};

// ================= HELPERS =================
export const handleApiError = (error) => {
  console.error("API Error:", error);
  const msg =
    error.response?.data?.message || error.message || "Sunucuya ulaşılamadı";
  const status = error.response?.status;

  if (error.code === "ERR_NETWORK" || status === 0) {
    return {
      success: false,
      message: "Backend erişilemiyor.",
      status: "NETWORK_ERROR",
    };
  }
  return { success: false, message: msg, status: status || "UNKNOWN_ERROR" };
};

export const handleApiSuccess = (data, msg = "İşlem başarılı") => ({
  success: true,
  data,
  message: msg,
  timestamp: new Date().toISOString(),
});

// ================= BACKEND TEST =================
export const testConnection = async () => {
  try {
    const res = await api.get("/health");
    return { connected: true, data: res.data };
  } catch (err) {
    return { connected: false, error: handleApiError(err) };
  }
};

export default api;
