import { useState, FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useRegisterMutation, useGoogleLoginMutation } from '../services/authApi';
import { setCredentials } from '../features/auth/authSlice';
import { useAppDispatch } from '../hooks/useRedux';
import { useGoogleLogin } from '@react-oauth/google';
import { FcGoogle } from 'react-icons/fc';
import styles from './AuthPage.module.css';

function RegisterPage() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const [register, { isLoading }] = useRegisterMutation();
  const [googleLoginMutation] = useGoogleLoginMutation();

  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  // Google registration state
  const [needsRegistration, setNeedsRegistration] = useState(false);
  const [regData, setRegData] = useState<{ email: string; avatarUrl: string; googleName: string; accessToken: string } | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');

    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    try {
      await register({ username, email, password }).unwrap();
      setSuccess(true);
    } catch (err: unknown) {
      const error = err as { data?: { message?: string } };
      setError(error.data?.message ?? 'Registration failed. Please try again.');
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
          // Already registered, just log in
          dispatch(setCredentials({
            accessToken: result.data.accessToken,
            refreshToken: result.data.refreshToken,
            user: result.data.user,
          }));
          navigate('/dashboard');
        }
      } catch (err: unknown) {
        const error = err as { data?: { message?: string } };
        setError(error.data?.message ?? 'Google Login failed. Please try again.');
      }
    },
    onError: () => setError('Google Login Failed'),
  });

  const handleConfirmGoogleRegistration = async () => {
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
        navigate('/dashboard');
      }
    } catch (err: unknown) {
      const error = err as { data?: { message?: string } };
      setError(error.data?.message ?? 'Registration failed. Please try again.');
    }
  };

  if (success) {
    return (
      <div className={styles.container}>
        <div className={styles.formWrapper} style={{ textAlign: 'center' }}>
          <div className={styles.header}>
            <div className={styles.logo}>
              <span className={styles.logoIcon}>⚡</span>
              <span className={styles.logoText}>OKABE</span>
            </div>
            <h1 className={styles.title}>Check your email</h1>
            <p className={styles.subtitle}>
              We've sent a verification link to <strong>{email}</strong>.
              Please check your inbox and click the link to activate your account.
            </p>
          </div>
          <Link to="/login" className="btn btn-primary" style={{ marginTop: '20px', display: 'inline-block', textDecoration: 'none' }}>
            Go to Login
          </Link>
        </div>
      </div>
    );
  }

  if (needsRegistration && regData) {
    return (
      <div className={styles.container}>
        <div className={styles.formWrapper}>
          <div className={styles.header}>
            <div className={styles.logo}>
              <span className={styles.logoIcon}>⚡</span>
              <span className={styles.logoText}>OKABE</span>
            </div>
            <h1 className={styles.title}>Confirm Registration</h1>
            <p className={styles.subtitle}>Last step to create your account with Google</p>
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
              onClick={handleConfirmGoogleRegistration}
            >
              Confirm & Register
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
          <button className={styles.googleBtn} onClick={() => handleGoogleLogin()}>
            <span className={styles.googleIcon}><FcGoogle /></span>
            Sign up with Google
          </button>
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
