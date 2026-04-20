import { Navigate, Outlet } from 'react-router-dom';
import { useAppSelector } from '../../hooks/useRedux';
import Navbar from './Navbar';

function ProtectedRoute() {
  const isAuthenticated = useAppSelector((state) => state.auth.isAuthenticated);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <>
      <Navbar />
      <Outlet />
    </>
  );
}

export default ProtectedRoute;
