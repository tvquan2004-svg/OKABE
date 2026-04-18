import { useNavigate } from 'react-router-dom';
import styles from './HomePage.module.css';

function HomePage() {
  const navigate = useNavigate();

  return (
    <div className={styles.container}>
      <nav className={styles.nav}>
        <div className={styles.logo}>
          <span className={styles.logoIcon}>OK</span>
          <span className={styles.logoText}>OKABE</span>
        </div>
        <div className={styles.navLinks}>
          <button className="btn btn-outline" onClick={() => navigate('/login')}>
            Sign In
          </button>
          <button className="btn btn-primary" onClick={() => navigate('/register')}>
            Get Started Free
          </button>
        </div>
      </nav>

      <main className={styles.hero}>
        <div className={styles.heroGlow} />
        <h1 className={styles.heroTitle}>
          Manage Tasks.
          <br />
          <span className={styles.gradient}>Ship Faster.</span>
        </h1>
        <p className={styles.heroSubtitle}>
          A modern, beautiful task management platform for teams.
          <br />
          Organize your work with boards, lists, and cards powered by real-time collaboration.
        </p>
        <div className={styles.heroCta}>
          <button className="btn btn-primary" onClick={() => navigate('/register')}>
            Start Building
          </button>
          <button className="btn btn-outline" onClick={() => navigate('/login')}>
            Sign In
          </button>
        </div>

        <div className={styles.featureGrid}>
          <div className={styles.featureCard}>
            <div className={styles.featureIcon}>KB</div>
            <h3>Kanban Boards</h3>
            <p>Drag and drop cards between lists to visualize your workflow.</p>
          </div>
          <div className={styles.featureCard}>
            <div className={styles.featureIcon}>TM</div>
            <h3>Team Collaboration</h3>
            <p>Invite members, assign tasks, and communicate in real time.</p>
          </div>
          <div className={styles.featureCard}>
            <div className={styles.featureIcon}>SF</div>
            <h3>Secure and Fast</h3>
            <p>Enterprise-grade security with JWT auth and role-based access.</p>
          </div>
        </div>
      </main>

      <footer className={styles.footer}>
        <p>Copyright 2026 OKABE. Built with Spring Boot and React.</p>
      </footer>
    </div>
  );
}

export default HomePage;
