import { Navigate, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../hooks/useRedux';
import { useWebSocket } from '../../hooks/useWebSocket';
import { apiSlice } from '../../services/apiSlice';
import { setHighlightCommentId } from '../../utils/highlightComment';
import MainLayout from './MainLayout';

function ProtectedRoute() {
  const isAuthenticated = useAppSelector((state) => state.auth.isAuthenticated);
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  // Phase 3: Global WebSocket for Notifications
  useWebSocket({
    onMessage: (message) => {
      if (message.type === 'NEGATIVE_SENTIMENT' && message.payload) {
        const { cardId, boardId, commentId } = message.payload as Record<string, unknown>;
        setHighlightCommentId(Number(commentId));
        if (cardId && boardId) {
          navigate(`/board/${boardId}?cardId=${cardId}&highlightComment=${commentId}`);
        }
        dispatch(apiSlice.util.invalidateTags(['Notification']));
      }
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
