import { useState, FormEvent } from 'react';
import { useSearchParams, Link, useNavigate } from 'react-router-dom';
import { useLoginMutation, useGoogleLoginMutation } from '../services/authApi';
import { setCredentials } from '../features/auth/authSlice';
import { useAppDispatch } from '../hooks/useRedux';
import { useGoogleLogin } from '@react-oauth/google';
import { FcGoogle } from 'react-icons/fc';
import styles from './AuthPage.module.css';

function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const dispatch = useAppDispatch();
  const redirect = searchParams.get('redirect') || '/dashboard';
  const [login, { isLoading }] = useLoginMutation();
  const [googleLoginMutation] = useGoogleLoginMutation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  
  // Registration confirmation state
  const [needsRegistration, setNeedsRegistration] = useState(false);
  const [regData, setRegData] = useState<{ email: string; avatarUrl: string; googleName: string; accessToken: string } | null>(null);
  const [username, setUsername] = useState('');

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      const result = await login({ email, password }).unwrap();
      if (result.data.accessToken && result.data.refreshToken && result.data.user) {
        dispatch(setCredentials({
          accessToken: result.data.accessToken,
          refreshToken: result.data.refreshToken,
          user: result.data.user,
        }));
        navigate(redirect);
      }
    } catch (err: unknown) {
      const error = err as { data?: { message?: string } };
      setError(error.data?.message ?? 'Login failed. Please try again.');
    }
  };

  const handleGoogleLogin = useGoogleLogin({
    onSuccess: async (tokenResponse) => {
      try {
        const result = await googleLoginMutation({ accessToken: tokenResponse.access_token }).unwrap();
        
        if (result.data.needsRegistration) {
          setNeedsRegistration(true);
          setRegData({
            email: result.data.email || '',
            avatarUrl: result.data.avatarUrl || '',
            googleName: result.data.googleName || '',
            accessToken: tokenResponse.access_token
          });
          setUsername(result.data.googleName || '');
        } else if (result.data.accessToken && result.data.refreshToken && result.data.user) {
          dispatch(setCredentials({
            accessToken: result.data.accessToken,
            refreshToken: result.data.refreshToken,
            user: result.data.user,
          }));
          navigate(redirect);
        }
      } catch (err: unknown) {
        const error = err as { data?: { message?: string } };
        setError(error.data?.message ?? 'Google Login failed. Please try again.');
      }
    },
    onError: () => setError('Google Login Failed'),
  });

  const handleConfirmRegistration = async () => {
    if (!regData || !username.trim()) return;
    setError('');

    try {
      const result = await googleLoginMutation({ 
        accessToken: regData.accessToken,
        username: username.trim()
      }).unwrap();

      if (result.data.accessToken && result.data.refreshToken && result.data.user) {
        dispatch(setCredentials({
          accessToken: result.data.accessToken,
          refreshToken: result.data.refreshToken,
          user: result.data.user,
        }));
        navigate(redirect);
      }
    } catch (err: unknown) {
      const error = err as { data?: { message?: string } };
      setError(error.data?.message ?? 'Registration failed. Please try again.');
    }
  };

  if (needsRegistration && regData) {
    return (
      <div className={styles.container}>
        <div className={styles.formWrapper}>
          <div className={styles.header}>
            <div className={styles.logo}>
              <span className={styles.logoIcon}>⚡</span>
              <span className={styles.logoText}>OKABE</span>
            </div>
            <h1 className={styles.title}>Final Step</h1>
            <p className={styles.subtitle}>Confirm your username to create your account</p>
          </div>

          <div className={styles.confirmBox}>
            <div className={styles.googleUserBadge}>
              <img src={regData.avatarUrl} alt="Google Avatar" className={styles.googleAvatar} />
              <div className={styles.googleUserInfo}>
                <strong>{regData.googleName}</strong>
                <span>{regData.email}</span>
              </div>
            </div>

            <div className={styles.field} style={{ marginTop: '20px' }}>
              <label className={styles.label}>Choose Username</label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className={styles.input}
                placeholder="Enter username"
                required
              />
            </div>

            <button 
              className={`btn btn-primary ${styles.submitBtn}`}
              onClick={handleConfirmRegistration}
            >
              Verify & Create Account
            </button>
            
            <button 
              className="btn btn-outline" 
              style={{ width: '100%', marginTop: '10px' }}
              onClick={() => setNeedsRegistration(false)}
            >
              Cancel
            </button>
          </div>
        </div>
      </div>
    );
  }

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
          <button className={styles.googleBtn} onClick={() => handleGoogleLogin()}>
            <span className={styles.googleIcon}><FcGoogle /></span>
            Sign in with Google
          </button>
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
