import styles from './HomePage.module.css';

function HomePage() {
  return (
    <div className={styles.container}>
      <nav className={styles.nav}>
        <div className={styles.logo}>
          <span className={styles.logoIcon}>⚡</span>
          <span className={styles.logoText}>OKABE</span>
        </div>
        <div className={styles.navLinks}>
          <button className="btn btn-outline">Sign In</button>
          <button className="btn btn-primary">Get Started Free</button>
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
          <button className="btn btn-primary">Start Building →</button>
          <button className="btn btn-outline">View Demo</button>
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
