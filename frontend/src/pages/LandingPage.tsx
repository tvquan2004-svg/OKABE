import React from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  FiLayout, FiCheckCircle, FiActivity, 
  FiArrowRight, FiMonitor, FiGlobe 
} from 'react-icons/fi';
import styles from './LandingPage.module.css';

const LandingPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className={styles.container}>
      {/* Navbar */}
      <nav className={styles.navbar}>
        <div className={styles.logo}>
          <div className={styles.logoIcon}>O</div>
          <span>OKABE</span>
        </div>
        <div className={styles.navLinks}>
          <a href="#features">Tính năng</a>
          <a href="#about">Về chúng tôi</a>
          <button className={styles.loginBtn} onClick={() => navigate('/login')}>Đăng nhập</button>
          <button className={styles.signupBtn} onClick={() => navigate('/register')}>Bắt đầu miễn phí</button>
        </div>
      </nav>

      {/* Hero Section */}
      <section className={styles.hero}>
        <div className={styles.heroContent}>
          <div className={styles.badge}>Phiên bản 1.0 hiện đã ra mắt</div>
          <h1>Quản lý công việc <br /><span className={styles.gradientText}>Thông minh & Hiệu quả</span></h1>
          <p>
            OKABE giúp đội ngũ của bạn cộng tác theo thời gian thực, 
            tối ưu hóa quy trình và đạt được mục tiêu nhanh hơn bao giờ hết.
          </p>
          <div className={styles.heroActions}>
            <button className={styles.primaryBtn} onClick={() => navigate('/register')}>
              Dùng thử ngay <FiArrowRight />
            </button>
          </div>
        </div>
        <div className={styles.heroImage}>
          <div className={styles.cssIllustration}>
            <div className={styles.glassCard}></div>
            <div className={styles.glassCardSmall}></div>
            <div className={styles.circle1}></div>
            <div className={styles.circle2}></div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className={styles.features}>
        <div className={styles.sectionHeader}>
          <h2>Tại sao nên chọn OKABE?</h2>
          <p>Giải pháp toàn diện cho mọi nhu cầu quản lý dự án của bạn.</p>
        </div>
        <div className={styles.featureGrid}>
          <div className={styles.featureCard}>
            <div className={`${styles.featureIcon} ${styles.blue}`}>
              <FiLayout />
            </div>
            <h3>Kanban linh hoạt</h3>
            <p>Quản lý thẻ công việc trực quan với khả năng kéo thả mượt mà và nhãn tùy chỉnh.</p>
          </div>
          <div className={styles.featureCard}>
            <div className={`${styles.featureIcon} ${styles.green}`}>
              <FiCheckCircle />
            </div>
            <h3>Cộng tác thời gian thực</h3>
            <p>Mọi thay đổi đều được cập nhật tức thì cho toàn bộ thành viên qua WebSocket.</p>
          </div>
          <div className={styles.featureCard}>
            <div className={`${styles.featureIcon} ${styles.purple}`}>
              <FiActivity />
            </div>
            <h3>Phân tích chuyên sâu</h3>
            <p>Biểu đồ Burndown, Heatmap và thống kê hiệu suất giúp bạn luôn nắm bắt tiến độ.</p>
          </div>
          <div className={styles.featureCard}>
            <div className={`${styles.featureIcon} ${styles.orange}`}>
              <FiMonitor />
            </div>
            <h3>Đa góc nhìn</h3>
            <p>Dễ dàng chuyển đổi giữa các chế độ xem Kanban, Lịch và Dòng thời gian (Gantt).</p>
          </div>
        </div>
      </section>

      {/* Benefits for Community Section */}
      <section id="about" className={styles.community}>
        <div className={styles.communityContent}>
          <h2>Giá trị cho Cộng đồng</h2>
          <p>
            OKABE không chỉ là một công cụ quản lý. Chúng tôi xây dựng một hệ sinh thái mở 
            nhằm hỗ trợ các lập trình viên trẻ, các nhóm khởi nghiệp và cộng đồng mã nguồn mở.
          </p>
          <ul className={styles.benefitList}>
            <li>
              <strong>Nền tảng miễn phí:</strong> Bất kỳ ai cũng có thể bắt đầu sử dụng và tối ưu công việc mà không mất phí.
            </li>
            <li>
              <strong>Hỗ trợ giáo dục:</strong> Cung cấp một case-study hoàn chỉnh về kiến trúc Microservices và React hiện đại.
            </li>
            <li>
              <strong>Tối ưu hiệu suất:</strong> Được thiết kế để chạy nhanh, nhẹ, phù hợp với cả những môi trường máy chủ hạn chế.
            </li>
          </ul>
        </div>
        <div className={styles.communityStats}>
          <div className={styles.statBox}>
            <span>100%</span>
            <p>Bảo mật</p>
          </div>
          <div className={styles.statBox}>
            <span>0$</span>
            <p>Phí sử dụng</p>
          </div>
          <div className={styles.statBox}>
            <span>24/7</span>
            <p>Thời gian thực</p>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className={styles.footer}>
        <div className={styles.footerBottom}>
          <p>&copy; 2026 OKABE Project. Được tạo ra với ❤️ dành cho cộng đồng.</p>
          <div className={styles.footerLinks}>
            <a href="#"><FiGlobe /> Tiếng Việt</a>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;
