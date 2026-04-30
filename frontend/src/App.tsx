import { Navigate, Route, Routes, useSearchParams } from 'react-router-dom';
import ProtectedRoute from './components/common/ProtectedRoute';
import { useAppSelector } from './hooks/useRedux';
import BoardPage from './pages/BoardPage';
import DashboardPage from './pages/DashboardPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import TwoFactorPage from './pages/TwoFactorPage';
import WorkspacePage from './pages/WorkspacePage';
import SettingsPage from './pages/SettingsPage';
import AcceptInvitationPage from './pages/AcceptInvitationPage';
import VerifyEmailPage from './pages/VerifyEmailPage';
import PublicBoardPage from './pages/PublicBoardPage';
import CalendarView from './pages/CalendarView';
import TimelineView from './pages/TimelineView';
import AnalyticsDashboard from './pages/AnalyticsDashboard';
import LandingPage from './pages/LandingPage';
import AiChatWidget from './components/ai/AiChatWidget';

function LoginRoute({ isAuthenticated }: { isAuthenticated: boolean }) {
  const [searchParams] = useSearchParams();
  const redirect = searchParams.get('redirect');

  if (isAuthenticated) {
    return <Navigate to={redirect || "/dashboard"} replace />;
  }
  return <LoginPage />;
}

function App() {
  const isAuthenticated = useAppSelector((state) => state.auth.isAuthenticated);

  return (
    <>
      <Routes>
        <Route path="/" element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <LandingPage />} />
        <Route path="/register" element={!isAuthenticated ? <RegisterPage /> : <Navigate to="/dashboard" />} />
        <Route path="/auth/2fa" element={<TwoFactorPage />} />
        <Route
          path="/login"
          element={<LoginRoute isAuthenticated={isAuthenticated} />}
        />
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/workspace/:workspaceId" element={<WorkspacePage />} />
          <Route path="/board/:boardId" element={<BoardPage />} />
          <Route path="/board/:boardId/calendar" element={<CalendarView />} />
          <Route path="/board/:boardId/timeline" element={<TimelineView />} />
          <Route path="/board/:boardId/analytics" element={<AnalyticsDashboard />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>
        <Route path="/invitations/accept" element={<AcceptInvitationPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/public/:token" element={<PublicBoardPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      {isAuthenticated && <AiChatWidget />}
    </>
  );
}

export default App;
