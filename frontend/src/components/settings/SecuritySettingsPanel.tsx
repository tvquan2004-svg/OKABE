import { useState } from 'react';
import { 
  useSetup2faMutation, 
  useVerifySetup2faMutation, 
  useDisable2faMutation,
  useGetMeQuery
} from '../../services/authApi';
import { QRCodeSVG } from 'qrcode.react';
import styles from './SecuritySettingsPanel.module.css';

const SecuritySettingsPanel = () => {
  const { data: userData, refetch } = useGetMeQuery();
  const [setup2fa, { isLoading: isSettingUp }] = useSetup2faMutation();
  const [verifySetup, { isLoading: isVerifying }] = useVerifySetup2faMutation();
  const [disable2fa, { isLoading: isDisabling }] = useDisable2faMutation();

  const [setupData, setSetupData] = useState<{ secret: string; qrCodeUri: string } | null>(null);
  const [code, setCode] = useState('');
  const [backupCodes, setBackupCodes] = useState<string[] | null>(null);
  const [error, setError] = useState('');
  const [showDisableForm, setShowDisableForm] = useState(false);

  const handleStartSetup = async () => {
    try {
      const result = await setup2fa().unwrap();
      const dataToSet = (result as any).secret ? result : (result as any).data;
      if (dataToSet && dataToSet.secret) {
        setSetupData(dataToSet);
        setError('');
      } else {
        setError('Dữ liệu thiết lập không hợp lệ.');
      }
    } catch (_err: unknown) {
      setError('Không thể khởi tạo 2FA. Vui lòng thử lại.');
    }
  };

  const handleVerifySetup = async () => {
    if (!setupData || !code) return;
    try {
      const result = await verifySetup({ 
        secret: setupData.secret, 
        code: parseInt(code) 
      }).unwrap();
      
      const codes = Array.isArray(result) ? result : (result as any).data;
      
      if (Array.isArray(codes)) {
        setBackupCodes(codes);
        setSetupData(null);
        setCode('');
        setError('');
        refetch();
      } else {
        setError('Không nhận được mã dự phòng. Vui lòng thử lại.');
      }
    } catch (err: unknown) {
      const error = err as { data?: { message?: string } };
      setError(error.data?.message ?? 'Mã xác thực không hợp lệ.');
    }
  };

  const handleDisable = async () => {
    if (!code) return;
    try {
      await disable2fa({ code: parseInt(code) }).unwrap();
      setShowDisableForm(false);
      setCode('');
      setError('');
      refetch();
    } catch (err: unknown) {
      const error = err as { data?: { message?: string } };
      setError(error.data?.message ?? 'Mã xác thực không hợp lệ.');
    }
  };

  const is2faEnabled = userData?.data?.is2faEnabled;

  return (
    <div className={styles.panel}>
      <h3 className={styles.title}>Bảo mật tài khoản</h3>
      <p className={styles.description}>
        Tăng cường bảo mật cho tài khoản của bạn bằng cách yêu cầu mã xác thực mỗi khi đăng nhập.
      </p>

      {error && <div className={styles.error}>{error}</div>}

      {is2faEnabled === false && setupData === null && backupCodes === null && (
        <div className={styles.actionBox}>
          <div className={styles.statusOff}>
            <span>Xác thực 2 lớp (2FA) hiện đang <strong>TẮT</strong></span>
          </div>
          <button 
            className="btn btn-primary" 
            onClick={handleStartSetup}
            disabled={isSettingUp}
            style={{ position: 'relative', zIndex: 10, cursor: 'pointer', pointerEvents: 'auto' }}
          >
            {isSettingUp ? 'Đang chuẩn bị...' : 'Bật xác thực 2 lớp'}
          </button>
        </div>
      )}

      {setupData !== null && (
        <div className={styles.setupBox}>
          <h4>Thiết lập xác thực 2 lớp</h4>
          <p>Quét mã QR bên dưới bằng ứng dụng xác thực (như Google Authenticator hoặc Authy):</p>
          
          <div className={styles.qrContainer}>
            <QRCodeSVG value={setupData.qrCodeUri} size={200} />
          </div>
          
          <div className={styles.secretBox}>
            <small>Hoặc nhập mã này thủ công:</small>
            <code>{setupData.secret}</code>
          </div>

          <div className={styles.verifyStep}>
            <p>Nhập mã 6 chữ số từ ứng dụng để xác nhận:</p>
            <div className={styles.inputGroup}>
              <input 
                type="text" 
                value={code} 
                onChange={(e) => setCode(e.target.value)}
                placeholder="000000"
                maxLength={6}
                className={styles.otpInput}
              />
              <button 
                className="btn btn-primary"
                onClick={handleVerifySetup}
                disabled={isVerifying || code.length !== 6}
              >
                {isVerifying ? 'Đang xác nhận...' : 'Xác nhận & Kích hoạt'}
              </button>
            </div>
            <button className="btn btn-link" onClick={() => setSetupData(null)}>Hủy bỏ</button>
          </div>
        </div>
      )}

      {Array.isArray(backupCodes) && (
        <div className={styles.backupBox}>
          <h4>⚠️ Lưu mã dự phòng của bạn</h4>
          <p>Nếu bạn mất quyền truy cập vào ứng dụng xác thực, bạn có thể sử dụng các mã này để đăng nhập. Mỗi mã chỉ sử dụng được một lần.</p>
          
          <div className={styles.codesGrid}>
            {backupCodes.map((c, i) => <code key={i}>{c}</code>)}
          </div>

          <div className={styles.backupActions}>
            <button 
              className="btn btn-outline"
              onClick={() => {
                const text = backupCodes.join('\n');
                const blob = new Blob([text], { type: 'text/plain' });
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'okabe-backup-codes.txt';
                a.click();
              }}
            >
              Tải xuống mã dự phòng
            </button>
            <button className="btn btn-primary" onClick={() => setBackupCodes(null)}>Tôi đã lưu các mã này</button>
          </div>
        </div>
      )}

      {is2faEnabled === true && backupCodes === null && (
        <div className={styles.enabledBox}>
          <div className={styles.statusOn}>
            <span>✅ Xác thực 2 lớp (2FA) hiện đang <strong>BẬT</strong></span>
          </div>
          
          {!showDisableForm ? (
            <button className="btn btn-danger-outline" onClick={() => setShowDisableForm(true)}>
              Tắt xác thực 2 lớp
            </button>
          ) : (
            <div className={styles.disableForm}>
              <p>Để tắt 2FA, vui lòng nhập mã xác thực hiện tại:</p>
              <div className={styles.inputGroup}>
                <input 
                  type="text" 
                  value={code} 
                  onChange={(e) => setCode(e.target.value)}
                  placeholder="000000"
                  maxLength={6}
                  className={styles.otpInput}
                />
                <button 
                  className="btn btn-danger"
                  onClick={handleDisable}
                  disabled={isDisabling || code.length !== 6}
                >
                  {isDisabling ? 'Đang tắt...' : 'Xác nhận Tắt'}
                </button>
              </div>
              <button className="btn btn-link" onClick={() => { setShowDisableForm(false); setCode(''); }}>Hủy bỏ</button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default SecuritySettingsPanel;
