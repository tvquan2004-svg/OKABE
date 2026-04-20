import { useState, FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useRegisterMutation, useGoogleLoginMutation } from '../services/authApi';
import { setCredentials } from '../features/auth/authSlice';
import { useAppDispatch } from '../hooks/useRedux';
import { GoogleLogin } from '@react-oauth/google';
import styles from './AuthPage.module.css';

function RegisterPage() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const [register, { isLoading }] = useRegisterMutation();
  const [googleLogin] = useGoogleLoginMutation();

  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');

    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    try {
      const result = await register({ username, email, password }).unwrap();
      dispatch(setCredentials({
        accessToken: result.data.accessToken,
        refreshToken: result.data.refreshToken,
        user: result.data.user,
      }));
      navigate('/dashboard');
    } catch (err: unknown) {
      const error = err as { data?: { message?: string } };
      setError(error.data?.message ?? 'Registration failed. Please try again.');
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
          <h1 className={styles.title}>Create your account</h1>
          <p className={styles.subtitle}>Start managing your tasks in seconds</p>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          {error && <div className={styles.error}>{error}</div>}

          <div className={styles.field}>
            <label htmlFor="username" className={styles.label}>Username</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="johndoe"
              className={styles.input}
              required
              minLength={3}
              autoComplete="username"
            />
          </div>

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
              minLength={6}
              autoComplete="new-password"
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="confirmPassword" className={styles.label}>Confirm Password</label>
            <input
              id="confirmPassword"
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="••••••••"
              className={styles.input}
              required
              minLength={6}
              autoComplete="new-password"
            />
          </div>

          <button
            type="submit"
            className={`btn btn-primary ${styles.submitBtn}`}
            disabled={isLoading}
          >
            {isLoading ? 'Creating account...' : 'Create Account'}
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
          />
        </div>

        <p className={styles.footer}>
          Already have an account?{' '}
          <Link to="/login" className={styles.link}>Sign in</Link>
        </p>
      </div>
    </div>
  );
}

export default RegisterPage;
