import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import { apiRequest, setSessionIdentity } from '../api/client';
import { Button, Input } from '../components/EditorialUI';

export default function AuthPage({ onSignedIn, message = '' }) {
  const [role, setRole] = useState('OPERATIONS');
  const [companyName, setCompanyName] = useState('ColdLatch');
  const [password, setPassword] = useState('');
  const [visible, setVisible] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(message);
  const navigate = useNavigate();
  const location = useLocation();

  const signIn = async event => {
    event.preventDefault(); setBusy(true); setError('');
    try {
      const result = await apiRequest('/api/auth/login', { method: 'POST', body: JSON.stringify({ role, companyName, password }) });
      setSessionIdentity(result.role, result.displayName);
      onSignedIn(result);
      navigate(location.state?.from || '/dashboard', { replace: true });
    } catch (e) {
      setError(e.status === 401 || e.status === 429 ? 'Incorrect role or password' : (e.message || 'Unable to sign in. Try again.'));
    } finally { setBusy(false); }
  };

  return <main className="auth-page">
    <section className="auth-card">
      <div className="brand-lockup"><span className="brand-name">ColdLatch</span></div>
      <div className="auth-heading"><h1>Log in to ColdLatch</h1></div>
      <form onSubmit={signIn}>
        <label className="field-label" htmlFor="role">Role</label>
        <select id="role" className="form-select auth-input" value={role} onChange={e => setRole(e.target.value)}>
          <option value="OPERATIONS">Operations</option><option value="ANALYST">Analyst</option>
        </select>
        <label className="field-label" htmlFor="companyName">Company</label>
        <Input id="companyName" className="form-input auth-input" value={companyName} onChange={e => setCompanyName(e.target.value)} autoComplete="organization" required />
        <label className="field-label password-label" htmlFor="password">Password</label>
        <div className="password-field"><Input id="password" className="form-input auth-input" type={visible ? 'text' : 'password'} value={password} onChange={e => setPassword(e.target.value)} autoComplete="current-password" required />
          <button type="button" className="password-toggle" onClick={() => setVisible(v => !v)} aria-label={visible ? 'Hide password' : 'Show password'}>{visible ? <EyeOff size={17}/> : <Eye size={17}/>}</button>
        </div>
        <div className="login-error" role="alert" aria-live="polite">{error}</div>
        <Button className="btn-primary auth-submit" disabled={busy || !password || !companyName.trim()} type="submit">{busy ? 'Signing in…' : 'Sign in'}</Button>
        {import.meta.env.VITE_DEMO_MODE === 'true' && import.meta.env.VITE_DEMO_LOGIN_HINT && <p className="demo-login-note">{import.meta.env.VITE_DEMO_LOGIN_HINT}</p>}
      </form>
    </section>
  </main>;
}
