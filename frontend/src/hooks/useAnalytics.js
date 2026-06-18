import { useState } from "react";
import api from "../api/client";

export function useAnalytics() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  async function load(urlId) {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get(`/analytics/${urlId}`);
      setData(res.data);
    } catch (e) {
      setError(e.response?.data?.message || "Failed to load analytics");
    } finally {
      setLoading(false);
    }
  }

  function clear() {
    setData(null);
    setError(null);
  }

  return { data, loading, error, load, clear };
}
