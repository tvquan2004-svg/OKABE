import { useState, FormEvent, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useValidate2faMutation } from '../services/authApi';
import { setCredentials } from '../features/auth/authSlice';
import { useAppDispatch } from '../hooks/useRedux';
import styles from './AuthPage.module.css';

function TwoFactorPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useAppDispatch();
  const [validate2fa, { isLoading }] = useValidate2faMutation();

  const tempToken = location.state?.tempToken;
  const redirect = location.state?.redirect || '/dashboard';

  const [code, setCode] = useState('');
  const [isBackupCode, setIsBackupCode] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!tempToken) {
      navigate('/login');
    }
  }, [tempToken, navigate]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');

    if (!code.trim()) return;

    try {
      const result = await validate2fa({ 
        tempToken, 
        code: code.trim(), 
        isBackupCode 
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
      setError(error.data?.message ?? 'Mã xác thực không đúng. Vui lòng thử lại.');
    }
  };

  if (!tempToken) return null;

  return (
    <div className={styles.container}>
      <div className={styles.formWrapper}>
        <div className={styles.header}>
          <div className={styles.logo}>
            <img src="/favicon.png" className={styles.logoImg} alt="Logo" />
            <span className={styles.logoText}>OKABE</span>
          </div>
          <h1 className={styles.title}>Xác thực 2 lớp</h1>
          <p className={styles.subtitle}>
            {isBackupCode 
              ? 'Nhập mã dự phòng 8 chữ số của bạn' 
              : 'Nhập mã xác thực 6 chữ số từ ứng dụng của bạn'}
          </p>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          {error && <div className={styles.error}>{error}</div>}

          <div className={styles.field}>
            <label htmlFor="code" className={styles.label}>
              {isBackupCode ? 'Mã dự phòng' : 'Mã xác thực'}
            </label>
            <input
              id="code"
              type="text"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder={isBackupCode ? '00000000' : '000000'}
              className={styles.input}
              required
              autoFocus
              autoComplete="one-time-code"
              maxLength={isBackupCode ? 8 : 6}
            />
          </div>

          <button
            type="submit"
            className={`btn btn-primary ${styles.submitBtn}`}
            disabled={isLoading}
          >
            {isLoading ? 'Đang xác thực...' : 'Xác nhận'}
          </button>
        </form>

        <div className={styles.footer} style={{ marginTop: '20px' }}>
          <button 
            className={styles.link} 
            onClick={() => {
              setIsBackupCode(!isBackupCode);
              setCode('');
              setError('');
            }}
            style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
          >
            {isBackupCode ? 'Sử dụng mã OTP' : 'Sử dụng mã dự phòng'}
          </button>
        </div>

        <p className={styles.footer}>
          <button 
            className={styles.link} 
            onClick={() => navigate('/login')}
            style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
          >
            Quay lại đăng nhập
          </button>
        </p>
      </div>
    </div>
  );
}

export default TwoFactorPage;
