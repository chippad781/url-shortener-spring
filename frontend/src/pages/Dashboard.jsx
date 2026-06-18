import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { useUrls } from "../hooks/useUrls";
import { useAnalytics } from "../hooks/useAnalytics";

export default function Dashboard() {
  const { user, logout } = useAuth();
  const { urls, loading, error, createUrl, deleteUrl } = useUrls();
  const analytics = useAnalytics();

  const [originalUrl, setOriginalUrl] = useState("");
  const [customAlias, setCustomAlias] = useState("");
  const [formError, setFormError] = useState(null);
  const [creating, setCreating] = useState(false);

  async function onCreate(e) {
    e.preventDefault();
    setCreating(true);
    setFormError(null);
    try {
      await createUrl({
        originalUrl,
        customAlias: customAlias.trim() || undefined,
      });
      setOriginalUrl("");
      setCustomAlias("");
    } catch (err) {
      const data = err.response?.data;
      setFormError(
        data?.fields ? Object.values(data.fields).join(", ") : data?.message || "Could not create link"
      );
    } finally {
      setCreating(false);
    }
  }

  return (
    <div className="dashboard">
      <header className="topbar">
        <strong>LinkSnip</strong>
        <div className="spacer" />
        <span className="muted">{user?.displayName || user?.email}</span>
        <button className="ghost" onClick={logout}>Log out</button>
      </header>

      <section className="panel">
        <h2>Shorten a URL</h2>
        <form onSubmit={onCreate} className="create-form">
          <input
            type="url"
            placeholder="https://example.com/some/long/link"
            value={originalUrl}
            onChange={(e) => setOriginalUrl(e.target.value)}
            required
          />
          <input
            placeholder="custom-alias (optional)"
            value={customAlias}
            onChange={(e) => setCustomAlias(e.target.value)}
          />
          <button type="submit" disabled={creating}>{creating ? "Creating…" : "Shorten"}</button>
        </form>
        {formError && <p className="error">{formError}</p>}
      </section>

      <section className="panel">
        <h2>Your links</h2>
        {loading && <p className="muted">Loading…</p>}
        {error && <p className="error">{error}</p>}
        {!loading && urls.length === 0 && <p className="muted">No links yet.</p>}
        <ul className="url-list">
          {urls.map((u) => (
            <li key={u.id}>
              <div className="url-main">
                <a href={u.shortUrl} target="_blank" rel="noreferrer">{u.shortUrl}</a>
                <span className="orig">{u.originalUrl}</span>
              </div>
              <div className="url-meta">
                <span className="clicks">{u.clickCount} clicks</span>
                <button className="ghost" onClick={() => navigator.clipboard.writeText(u.shortUrl)}>Copy</button>
                <button className="ghost" onClick={() => analytics.load(u.id)}>Stats</button>
                <button className="danger" onClick={() => deleteUrl(u.id)}>Delete</button>
              </div>
            </li>
          ))}
        </ul>
      </section>

      {(analytics.data || analytics.loading) && (
        <section className="panel">
          <div className="panel-head">
            <h2>Analytics</h2>
            <button className="ghost" onClick={analytics.clear}>Close</button>
          </div>
          {analytics.loading && <p className="muted">Loading…</p>}
          {analytics.error && <p className="error">{analytics.error}</p>}
          {analytics.data && (
            <>
              <p>
                <strong>/{analytics.data.shortCode}</strong> — {analytics.data.totalClicks} total ·{" "}
                {analytics.data.clicksLast7Days} (7d) · {analytics.data.clicksLast30Days} (30d)
              </p>
              <div className="bars">
                {analytics.data.dailySeries.length === 0 && <span className="muted">No clicks in the last 30 days.</span>}
                {analytics.data.dailySeries.map((d) => (
                  <div className="bar-row" key={d.date}>
                    <span className="bar-date">{d.date}</span>
                    <span className="bar" style={{ width: `${Math.min(d.clicks * 16, 240)}px` }} />
                    <span className="bar-val">{d.clicks}</span>
                  </div>
                ))}
              </div>
            </>
          )}
        </section>
      )}
    </div>
  );
}
