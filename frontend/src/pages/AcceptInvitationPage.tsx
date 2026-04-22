import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useAcceptInvitationMutation } from '../services/workspaceApi';
import { useAppSelector } from '../hooks/useRedux';

const AcceptInvitationPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();
  const isAuthenticated = useAppSelector((state) => state.auth.isAuthenticated);
  const [acceptInvitation, { isLoading }] = useAcceptInvitationMutation();
  const [error, setError] = useState<string | null>(null);

  const handleAccept = React.useCallback(async () => {
    console.log('Attempting to accept invitation with token:', token);
    try {
      await acceptInvitation(token!).unwrap();
      console.log('Invitation accepted successfully!');
      navigate('/dashboard');
    } catch (err: any) {
      console.error('Failed to accept invitation:', err);
      const msg = err.data?.message || 'Failed to accept invitation. It may have expired or is invalid.';
      setError(msg);
    }
  }, [token, acceptInvitation, navigate]);

  useEffect(() => {
    if (!token) {
      setError('Invalid or missing invitation token.');
      return;
    }

    if (!isAuthenticated) {
      // If not logged in, redirect to login but save the current URL to come back
      const currentPath = window.location.pathname + window.location.search;
      navigate(`/login?redirect=${encodeURIComponent(currentPath)}`);
      return;
    }

    handleAccept();
  }, [token, isAuthenticated, navigate, handleAccept]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8 bg-white p-10 rounded-xl shadow-lg text-center">
        <h2 className="mt-6 text-3xl font-extrabold text-gray-900">
          Workspace Invitation
        </h2>
        
        {isLoading && (
          <div className="flex flex-col items-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mb-4"></div>
            <p className="text-gray-600">Processing your invitation...</p>
          </div>
        )}

        {error && (
          <div className="mt-4 p-4 bg-red-50 text-red-700 rounded-md border border-red-200">
            <p className="font-medium">Error</p>
            <p className="text-sm">{error}</p>
            <button
              onClick={() => navigate('/dashboard')}
              className="mt-4 inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
            >
              Go to Dashboard
            </button>
          </div>
        )}

        {!isLoading && !error && !isAuthenticated && (
          <p className="text-gray-600">Please log in to accept the invitation.</p>
        )}
      </div>
    </div>
  );
};

export default AcceptInvitationPage;
