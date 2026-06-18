import { createContext, useContext, useEffect, useState } from "react";
import api, {
  setTokens,
  clearTokens,
  bootstrapSession,
  hasRefreshToken,
} from "../api/client";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // On first load, if we have a refresh token, mint an access token and
  // fetch the profile so the UI knows who's logged in.
  useEffect(() => {
    (async () => {
      if (hasRefreshToken() && (await bootstrapSession())) {
        try {
          const { data } = await api.get("/auth/profile");
          setUser(data);
        } catch {
          clearTokens();
        }
      }
      setLoading(false);
    })();
  }, []);

  async function login(email, password) {
    const { data } = await api.post("/auth/login", { email, password });
    setTokens(data);
    const profile = await api.get("/auth/profile");
    setUser(profile.data);
  }

  async function register(email, password, displayName) {
    const { data } = await api.post("/auth/register", {
      email,
      password,
      displayName,
    });
    setTokens(data);
    const profile = await api.get("/auth/profile");
    setUser(profile.data);
  }

  function logout() {
    clearTokens();
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
