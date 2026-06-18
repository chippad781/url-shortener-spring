import axios from "axios";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

// In-memory token store. Refresh token is persisted so a reload stays logged in;
// access token is kept in memory and re-minted from the refresh token on boot.
const tokens = {
  access: null,
  refresh: localStorage.getItem("refreshToken"),
};

export function setTokens({ accessToken, refreshToken }) {
  tokens.access = accessToken;
  tokens.refresh = refreshToken;
  if (refreshToken) localStorage.setItem("refreshToken", refreshToken);
}

export function clearTokens() {
  tokens.access = null;
  tokens.refresh = null;
  localStorage.removeItem("refreshToken");
}

export function hasRefreshToken() {
  return Boolean(tokens.refresh);
}

const api = axios.create({ baseURL: `${API_URL}/api/v1` });

api.interceptors.request.use((config) => {
  if (tokens.access) {
    config.headers.Authorization = `Bearer ${tokens.access}`;
  }
  return config;
});

// On a 401, try once to refresh the access token and replay the request.
let refreshing = null;

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;

    if (status === 401 && !original._retry && tokens.refresh) {
      original._retry = true;
      try {
        refreshing =
          refreshing ||
          axios.post(`${API_URL}/api/v1/auth/refresh`, {
            refreshToken: tokens.refresh,
          });
        const { data } = await refreshing;
        refreshing = null;
        setTokens(data);
        original.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(original);
      } catch (e) {
        refreshing = null;
        clearTokens();
        window.location.href = "/login";
        return Promise.reject(e);
      }
    }
    return Promise.reject(error);
  }
);

// Used at startup to obtain a fresh access token from a persisted refresh token.
export async function bootstrapSession() {
  if (!tokens.refresh) return false;
  try {
    const { data } = await axios.post(`${API_URL}/api/v1/auth/refresh`, {
      refreshToken: tokens.refresh,
    });
    setTokens(data);
    return true;
  } catch {
    clearTokens();
    return false;
  }
}

export default api;
