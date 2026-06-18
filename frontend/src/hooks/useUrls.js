import { useCallback, useEffect, useState } from "react";
import api from "../api/client";

export function useUrls() {
  const [urls, setUrls] = useState([]);
  const [count, setCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get("/urls", { params: { page: 1, pageSize: 50 } });
      setUrls(data.results);
      setCount(data.count);
      setError(null);
    } catch (e) {
      setError(e.response?.data?.message || "Failed to load URLs");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function createUrl(payload) {
    const { data } = await api.post("/urls", payload);
    await load();
    return data;
  }

  async function deleteUrl(id) {
    await api.delete(`/urls/${id}`);
    setUrls((prev) => prev.filter((u) => u.id !== id));
  }

  return { urls, count, loading, error, reload: load, createUrl, deleteUrl };
}
