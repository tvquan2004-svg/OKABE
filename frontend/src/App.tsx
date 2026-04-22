import { Navigate, Route, Routes, useSearchParams } from 'react-router-dom';
import ProtectedRoute from './components/common/ProtectedRoute';
import { useAppSelector } from './hooks/useRedux';
import BoardPage from './pages/BoardPage';
import DashboardPage from './pages/DashboardPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import WorkspacePage from './pages/WorkspacePage';
import SettingsPage from './pages/SettingsPage';
import AcceptInvitationPage from './pages/AcceptInvitationPage';
import VerifyEmailPage from './pages/VerifyEmailPage';
import CalendarView from './pages/CalendarView';
import TimelineView from './pages/TimelineView';
import AnalyticsDashboard from './pages/AnalyticsDashboard';

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
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route
        path="/login"
        element={<LoginRoute isAuthenticated={isAuthenticated} />}
      />
      <Route
        path="/register"
        element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <RegisterPage />}
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
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default App;
