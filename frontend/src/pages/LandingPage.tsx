import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  FiArrowRight, FiLayout, FiUsers, FiBarChart2, FiColumns,
  FiShield, FiClock, FiLayers
} from 'react-icons/fi';
import styles from './LandingPage.module.css';

const sections = [
  { id: 'features', label: 'Tính năng' },
  { id: 'community', label: 'Về chúng tôi' },
];

const features = [
  {
    icon: <FiLayout />,
    title: 'Kanban linh hoạt',
    desc: 'Quản lý thẻ công việc trực quan với kéo thả mượt mà, nhãn tùy chỉnh và bộ lọc thông minh.',
    span: 'wide',
    color: 'navy',
  },
  {
    icon: <FiUsers />,
    title: 'Cộng tác thời gian thực',
    desc: 'Mọi thay đổi cập nhật tức thì qua WebSocket. Bình luận, đề cập, và thông báo trực tiếp.',
    span: 'small',
    color: 'navy',
  },
  {
    icon: <FiBarChart2 />,
    title: 'Phân tích chuyên sâu',
    desc: 'Biểu đồ Burndown, Heatmap hiệu suất và thống kê OKR giúp bạn luôn nắm bắt tiến độ.',
    span: 'small',
    color: 'navy',
  },
  {
    icon: <FiColumns />,
    title: 'Đa góc nhìn',
    desc: 'Chuyển đổi linh hoạt giữa Kanban, Lịch, Dòng thời gian Gantt và chế độ xem theo dự án.',
    span: 'full',
    color: 'navy',
  },
];

const benefits = [
  {
    icon: <FiLayers />,
    title: 'Nền tảng miễn phí',
    desc: 'Bất kỳ ai cũng có thể bắt đầu sử dụng và tối ưu công việc mà không mất phí.',
  },
  {
    icon: <FiShield />,
    title: 'Hỗ trợ giáo dục',
    desc: 'Case-study hoàn chỉnh về kiến trúc Microservices và React hiện đại cho cộng đồng.',
  },
  {
    icon: <FiClock />,
    title: 'Tối ưu hiệu suất',
    desc: 'Được thiết kế để chạy nhanh, nhẹ, phù hợp với cả môi trường máy chủ hạn chế.',
  },
];

const stats = [
  { value: '100%', label: 'Bảo mật', sub: 'Mã nguồn mở' },
  { value: '0₫', label: 'Phí sử dụng', sub: 'Mãi mãi' },
  { value: '24/7', label: 'Thời gian thực', sub: 'WebSocket' },
];

function useScrollReveal() {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    const revealEls = el.querySelectorAll('.reveal');
    if (revealEls.length === 0) return;

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('visible');
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.15, rootMargin: '0px 0px -40px 0px' }
    );

    revealEls.forEach((el) => observer.observe(el));
    return () => observer.disconnect();
  }, []);

  return ref;
}

const LandingPage: React.FC = () => {
  const navigate = useNavigate();
  const pageRef = useScrollReveal();

  return (
    <div className={styles.page} ref={pageRef}>
      {/* Floating Island Nav */}
      <nav className={styles.nav}>
        <div className={styles.navInner}>
          <div className={styles.logo}>
            <div className={styles.logoMark}>O</div>
            <span className={styles.logoText}>OKABE</span>
          </div>
          <div className={styles.navRight}>
            <div className={styles.navLinks}>
              {sections.map((s) => (
                <a key={s.id} href={`#${s.id}`} className={styles.navLink}>{s.label}</a>
              ))}
            </div>
            <div className={styles.navActions}>
              <button className={styles.loginBtn} onClick={() => navigate('/login')}>
                Đăng nhập
              </button>
              <button className={styles.primaryBtn} onClick={() => navigate('/register')}>
                Bắt đầu miễn phí
              </button>
            </div>
          </div>
        </div>
      </nav>

      {/* Hero */}
      <section className={styles.hero}>
        <div className={styles.heroInner}>
          <div className={styles.heroContent}>
            <div className={`reveal ${styles.badge}`}>
              <span className={styles.badgeDot} />
              Phiên bản 1.0 · Đã ra mắt
            </div>
            <h1 className={`reveal reveal-delay-1 ${styles.heroTitle}`}>
              Quản lý công việc<br />
              <span className={styles.heroTitleAccent}>Thông minh & Hiệu quả</span>
            </h1>
            <p className={`reveal reveal-delay-2 ${styles.heroDesc}`}>
              OKABE giúp đội ngũ của bạn cộng tác thời gian thực, tối ưu hóa quy trình
              và đạt được mục tiêu nhanh hơn.
            </p>
            <div className={`reveal reveal-delay-3 ${styles.heroActions}`}>
              <button className={styles.heroCta} onClick={() => navigate('/register')}>
                Dùng thử ngay
                <span className={styles.ctaIcon}>
                  <FiArrowRight />
                </span>
              </button>
            </div>
          </div>
          <div className={`reveal reveal-delay-2 ${styles.heroVisual}`}>
            <div className={styles.visualShell}>
              <div className={styles.visualInner}>
                <div className={styles.visualHeader}>
                  <div className={styles.visualDots}>
                    <span className={styles.dot} />
                    <span className={styles.dot} />
                    <span className={styles.dot} />
                  </div>
                  <span className={styles.visualHeaderText}>Giới thiệu OKABE</span>
                </div>
                <div className={styles.visualCards}>
                  <div className={styles.mockCard}>
                    <div className={styles.mockCardTop}>
                      <div className={styles.mockCardIcon} />
                      <div>
                        <div className={styles.mockCardTitle}>Hệ thống Quản lý Dự án</div>
                        <div className={styles.mockCardDesc}>Kết nối thành viên và theo dõi tiến độ</div>
                      </div>
                    </div>
                    <div className={styles.mockCardTags}>
                      <span className={styles.mockTag}>Tổng quan</span>
                      <span className={styles.mockTag}>Kế hoạch</span>
                    </div>
                  </div>
                  <div className={styles.mockCard}>
                    <div className={styles.mockCardTop}>
                      <div className={styles.mockCardIcon} />
                      <div>
                        <div className={styles.mockCardTitle}>Tối ưu hóa năng suất</div>
                        <div className={styles.mockCardDesc}>Kanban, OKR, Lịch biểu và Gantt</div>
                      </div>
                    </div>
                    <div className={styles.mockProgress}>
                      <div className={styles.mockProgressTrack}>
                        <div className={styles.mockProgressBar} style={{ width: '75%' }} />
                      </div>
                      <span className={styles.mockProgressLabel}>75%</span>
                    </div>
                  </div>
                  <div className={styles.mockCard}>
                    <div className={styles.mockCardTop}>
                      <div className={styles.mockCardIcon} />
                      <div>
                        <div className={styles.mockCardTitle}>Mã nguồn mở & Tự do</div>
                        <div className={styles.mockCardDesc}>Dành cho nhóm khởi nghiệp và cộng đồng</div>
                      </div>
                    </div>
                    <div className={styles.mockCardTags}>
                      <span className={styles.mockTag}>Giá trị</span>
                      <span className={styles.mockTag}>Cộng đồng</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="features" className={styles.features}>
        <div className={styles.sectionInner}>
          <div className={styles.sectionHeader}>
            <h2 className={`reveal ${styles.sectionTitle}`}>Tại sao nên chọn OKABE?</h2>
            <p className={`reveal reveal-delay-1 ${styles.sectionDesc}`}>
              Giải pháp toàn diện cho mọi nhu cầu quản lý dự án.
            </p>
          </div>
          <div className={styles.featureGrid}>
            {features.map((f, i) => (
              <div
                key={f.title}
                className={`reveal reveal-delay-${Math.min(i + 1, 4)} ${styles.featureCard} ${styles[`card${f.span}`]}`}
              >
                <div className={styles.featureIconWrap}>
                  {f.icon}
                </div>
                <h3 className={styles.featureTitle}>{f.title}</h3>
                <p className={styles.featureDesc}>{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Stats Strip */}
      <section className={styles.statsStrip}>
        <div className={styles.sectionInner}>
          <div className={styles.statsGrid}>
            {stats.map((s, i) => (
              <div key={s.label} className={`reveal reveal-delay-${i + 1} ${styles.statItem}`}>
                <span className={styles.statValue}>{s.value}</span>
                <div className={styles.statInfo}>
                  <span className={styles.statLabel}>{s.label}</span>
                  <span className={styles.statSub}>{s.sub}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Community */}
      <section id="community" className={styles.community}>
        <div className={styles.sectionInner}>
          <div className={styles.communityLayout}>
            <div className={styles.communityContent}>
              <h2 className={`reveal ${styles.communityTitle}`}>Giá trị cho Cộng đồng</h2>
              <p className={`reveal reveal-delay-1 ${styles.communityDesc}`}>
                OKABE không chỉ là công cụ quản lý. Chúng tôi xây dựng hệ sinh thái mở
                nhằm hỗ trợ lập trình viên trẻ, nhóm khởi nghiệp và cộng đồng mã nguồn mở.
              </p>
              <div className={styles.benefitsList}>
                {benefits.map((b, i) => (
                  <div key={b.title} className={`reveal reveal-delay-${i + 1} ${styles.benefitItem}`}>
                    <div className={styles.benefitIcon}>{b.icon}</div>
                    <div>
                      <strong className={styles.benefitTitle}>{b.title}</strong>
                      <p className={styles.benefitDesc}>{b.desc}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <div className={`reveal reveal-delay-2 ${styles.communityVisual}`}>
              <div className={styles.communityCard}>
                <div className={styles.communityCardInner}>
                  <div className={styles.communityCardHeader}>Số liệu nổi bật</div>
                  <div className={styles.communityMetrics}>
                    <div className={styles.metricItem}>
                      <span className={styles.metricValue}>1.2k+</span>
                      <span className={styles.metricLabel}>Người dùng</span>
                    </div>
                    <div className={styles.metricItem}>
                      <span className={styles.metricValue}>50+</span>
                      <span className={styles.metricLabel}>Quốc gia</span>
                    </div>
                    <div className={styles.metricItem}>
                      <span className={styles.metricValue}>99%</span>
                      <span className={styles.metricLabel}>Hài lòng</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className={styles.ctaSection}>
        <div className={styles.sectionInner}>
          <div className={`reveal ${styles.ctaBox}`}>
            <h2 className={styles.ctaTitle}>Sẵn sàng dùng thử?</h2>
            <p className={styles.ctaDesc}>
              Hoàn toàn miễn phí. Không cần thẻ tín dụng.
            </p>
            <button className={styles.heroCta} onClick={() => navigate('/register')}>
              Bắt đầu ngay
              <span className={styles.ctaIcon}>
                <FiArrowRight />
              </span>
            </button>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className={styles.footer}>
        <div className={styles.sectionInner}>
          <div className={styles.footerLayout}>
            <div className={styles.footerBrand}>
              <div className={styles.logo}>
                <div className={styles.logoMark}>O</div>
                <span className={styles.logoText}>OKABE</span>
              </div>
              <p className={styles.footerDesc}>
                Nền tảng quản lý công việc mã nguồn mở dành cho đội ngũ hiện đại.
              </p>
            </div>
            <div className={styles.footerLinks}>
              <div className={styles.footerCol}>
                <span className={styles.footerColTitle}>Sản phẩm</span>
                <a href="#features">Tính năng</a>
                <a href="#">Tải về</a>
                <a href="#">API</a>
              </div>
              <div className={styles.footerCol}>
                <span className={styles.footerColTitle}>Công ty</span>
                <a href="#community">Về chúng tôi</a>
                <a href="#">Blog</a>
                <a href="#">Tuyển dụng</a>
              </div>
              <div className={styles.footerCol}>
                <span className={styles.footerColTitle}>Pháp lý</span>
                <a href="#">Chính sách bảo mật</a>
                <a href="#">Điều khoản dịch vụ</a>
              </div>
            </div>
          </div>
          <div className={styles.footerBottom}>
            <p>&copy; 2026 OKABE Project. Mã nguồn mở dành cho cộng đồng.</p>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;
