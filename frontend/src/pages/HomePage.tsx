<<<<<<< HEAD
import { useNavigate } from 'react-router-dom';
import styles from './HomePage.module.css';

function HomePage() {
  const navigate = useNavigate();

=======
import styles from './HomePage.module.css';

function HomePage() {
>>>>>>> ff3a2ec6328dbe8306d27b4f0eb9a549f3c65124
  return (
    <div className={styles.container}>
      <nav className={styles.nav}>
        <div className={styles.logo}>
          <span className={styles.logoIcon}>⚡</span>
          <span className={styles.logoText}>OKABE</span>
        </div>
        <div className={styles.navLinks}>
<<<<<<< HEAD
          <button className="btn btn-outline" onClick={() => navigate('/login')}>
            Sign In
          </button>
          <button className="btn btn-primary" onClick={() => navigate('/register')}>
            Get Started Free
          </button>
=======
          <button className="btn btn-outline">Sign In</button>
          <button className="btn btn-primary">Get Started Free</button>
>>>>>>> ff3a2ec6328dbe8306d27b4f0eb9a549f3c65124
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
          Organize your work with boards, lists, and cards — powered by real-time collaboration.
        </p>
        <div className={styles.heroCta}>
<<<<<<< HEAD
          <button className="btn btn-primary" onClick={() => navigate('/register')}>
            Start Building →
          </button>
          <button className="btn btn-outline" onClick={() => navigate('/login')}>
            Sign In
          </button>
=======
          <button className="btn btn-primary">Start Building →</button>
          <button className="btn btn-outline">View Demo</button>
>>>>>>> ff3a2ec6328dbe8306d27b4f0eb9a549f3c65124
        </div>

        <div className={styles.featureGrid}>
          <div className={styles.featureCard}>
            <div className={styles.featureIcon}>📋</div>
            <h3>Kanban Boards</h3>
            <p>Drag-and-drop cards between lists to visualize your workflow.</p>
          </div>
          <div className={styles.featureCard}>
            <div className={styles.featureIcon}>👥</div>
            <h3>Team Collaboration</h3>
            <p>Invite members, assign tasks, and communicate in real-time.</p>
          </div>
          <div className={styles.featureCard}>
            <div className={styles.featureIcon}>🔒</div>
            <h3>Secure & Fast</h3>
            <p>Enterprise-grade security with JWT auth and role-based access.</p>
          </div>
        </div>
      </main>

      <footer className={styles.footer}>
        <p>© 2026 OKABE — Built with Spring Boot & React</p>
      </footer>
    </div>
  );
}

export default HomePage;
