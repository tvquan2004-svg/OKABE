import { Navigate, Outlet } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../hooks/useRedux';
import { useWebSocket } from '../../hooks/useWebSocket';
import { apiSlice } from '../../services/apiSlice';
import Navbar from './Navbar';
import MainLayout from './MainLayout';

function ProtectedRoute() {
  const isAuthenticated = useAppSelector((state) => state.auth.isAuthenticated);
  const dispatch = useAppDispatch();

  // Phase 3: Global WebSocket for Notifications
  useWebSocket({
    onMessage: (message) => {
      if (message.type === 'NOTIFICATION_RECEIVED') {
        dispatch(apiSlice.util.invalidateTags(['Notification']));
      }
    },
  });

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <MainLayout />;
}

export default ProtectedRoute;
