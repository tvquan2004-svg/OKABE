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
      if (result.data.requires2fa && result.data.tempToken) {
        navigate('/auth/2fa', { state: { tempToken: result.data.tempToken, redirect } });
        return;
      }

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
      setError(error.data?.message ?? 'Đăng nhập thất bại. Vui lòng thử lại.');
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
        } else if (result.data.requires2fa && result.data.tempToken) {
          navigate('/auth/2fa', { state: { tempToken: result.data.tempToken, redirect } });
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
        setError(error.data?.message ?? 'Đăng nhập Google thất bại. Vui lòng thử lại.');
      }
    },
    onError: () => setError('Đăng nhập Google thất bại'),
  });

  const handleConfirmRegistration = async () => {
    if (!regData || !username.trim()) return;
    setError('');

    try {
      const result = await googleLoginMutation({ 
        accessToken: regData.accessToken,
        username: username.trim()
      }).unwrap();

      if (result.data.requires2fa && result.data.tempToken) {
        navigate('/auth/2fa', { state: { tempToken: result.data.tempToken, redirect } });
        return;
      }

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
      setError(error.data?.message ?? 'Đăng ký thất bại. Vui lòng thử lại.');
    }
  };

  if (needsRegistration && regData) {
    return (
      <div className={styles.container}>
        <div className={styles.formWrapper}>
          <div className={styles.header}>
            <div className={styles.logo}>
              <img src="/favicon.png" className={styles.logoImg} alt="Logo" />
              <span className={styles.logoText}>OKABE</span>
            </div>
            <h1 className={styles.title}>Bước cuối cùng</h1>
            <p className={styles.subtitle}>Xác nhận tên người dùng để tạo tài khoản</p>
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
              <label className={styles.label}>Chọn tên người dùng</label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className={styles.input}
                placeholder="Nhập tên người dùng"
                required
              />
            </div>

            <button 
              className={`btn btn-primary ${styles.submitBtn}`}
              onClick={handleConfirmRegistration}
            >
              Xác nhận & Tạo tài khoản
            </button>
            
            <button 
              className="btn btn-outline" 
              style={{ width: '100%', marginTop: '10px' }}
              onClick={() => setNeedsRegistration(false)}
            >
              Hủy bỏ
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
            <img src="/favicon.png" className={styles.logoImg} alt="Logo" />
            <span className={styles.logoText}>OKABE</span>
          </Link>
          <h1 className={styles.title}>Chào mừng trở lại</h1>
          <p className={styles.subtitle}>Đăng nhập vào tài khoản để tiếp tục</p>
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
            <label htmlFor="password" className={styles.label}>Mật khẩu</label>
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
            {isLoading ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </button>
        </form>

        <div className={styles.divider}>
          <span>HOẶC</span>
        </div>

        <div className={styles.googleLoginWrapper}>
          <button className={styles.googleBtn} onClick={() => handleGoogleLogin()}>
            <span className={styles.googleIcon}><FcGoogle /></span>
            Đăng nhập với Google
          </button>
        </div>

        <p className={styles.footer}>
          Chưa có tài khoản?{' '}
          <Link to="/register" className={styles.link}>Tạo tài khoản mới</Link>
        </p>
      </div>
    </div>
  );
}

export default LoginPage;
