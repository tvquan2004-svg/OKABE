import { useState, FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useLoginMutation, useGoogleLoginMutation } from '../services/authApi';
import { setCredentials } from '../features/auth/authSlice';
import { useAppDispatch } from '../hooks/useRedux';
import { GoogleLogin } from '@react-oauth/google';
import styles from './AuthPage.module.css';

function LoginPage() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const [login, { isLoading }] = useLoginMutation();
  const [googleLogin] = useGoogleLoginMutation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      const result = await login({ email, password }).unwrap();
      dispatch(setCredentials({
        accessToken: result.data.accessToken,
        refreshToken: result.data.refreshToken,
        user: result.data.user,
      }));
      navigate('/dashboard');
    } catch (err: unknown) {
      const error = err as { data?: { message?: string } };
      setError(error.data?.message ?? 'Login failed. Please try again.');
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.formWrapper}>
        <div className={styles.header}>
          <Link to="/" className={styles.logo}>
            <span className={styles.logoIcon}>⚡</span>
            <span className={styles.logoText}>OKABE</span>
          </Link>
          <h1 className={styles.title}>Welcome back</h1>
          <p className={styles.subtitle}>Sign in to your account to continue</p>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          {error && <div className={styles.error}>{error}</div>}

          <div className={styles.field}>
            <label htmlFor="email" className={styles.label}>Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              className={styles.input}
              required
              autoComplete="email"
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="password" className={styles.label}>Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              className={styles.input}
              required
              autoComplete="current-password"
            />
          </div>

          <button
            type="submit"
            className={`btn btn-primary ${styles.submitBtn}`}
            disabled={isLoading}
          >
            {isLoading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <div className={styles.divider}>
          <span>OR</span>
        </div>

        <div className={styles.googleLoginWrapper}>
          <GoogleLogin
            onSuccess={async (credentialResponse) => {
              if (credentialResponse.credential) {
                try {
                  const result = await googleLogin({ idToken: credentialResponse.credential }).unwrap();
                  dispatch(setCredentials({
                    accessToken: result.data.accessToken,
                    refreshToken: result.data.refreshToken,
                    user: result.data.user,
                  }));
                  navigate('/dashboard');
                } catch (err: unknown) {
                  const error = err as { data?: { message?: string } };
                  setError(error.data?.message ?? 'Google Login failed. Please try again.');
                }
              }
            }}
            onError={() => {
              setError('Google Login Failed');
            }}
            useOneTap
          />
        </div>

        <p className={styles.footer}>
          Don't have an account?{' '}
          <Link to="/register" className={styles.link}>Create one</Link>
        </p>
      </div>
    </div>
  );
}

export default LoginPage;
