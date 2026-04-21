import React from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { useVerifyEmailQuery } from '../services/authApi';

const VerifyEmailPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  const navigate = useNavigate();
  
  const { data, isLoading, error } = useVerifyEmailQuery(token, {
    skip: !token
  });

  const getMessage = () => {
    if (!token) return 'Invalid or missing verification token.';
    if (error) {
      const err = error as any;
      return err.data?.message || 'Verification failed. The link may have expired or is invalid.';
    }
    return data?.message || 'Email verified successfully! You can now log in.';
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8 bg-white p-10 rounded-xl shadow-lg text-center">
        <h2 className="mt-6 text-3xl font-extrabold text-gray-900">
          Email Verification
        </h2>
        
        <div className="mt-8">
          {isLoading && (
            <div className="flex flex-col items-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mb-4"></div>
              <p className="text-gray-600">Verifying your email address...</p>
            </div>
          )}

          {!isLoading && !error && data && (
            <div className="p-4 bg-green-50 text-green-700 rounded-md border border-green-200">
              <p className="font-medium">Success!</p>
              <p className="text-sm">{getMessage()}</p>
              <Link
                to="/login"
                className="mt-4 inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
                style={{ textDecoration: 'none' }}
              >
                Go to Login
              </Link>
            </div>
          )}

          {!isLoading && (error || !token) && (
            <div className="p-4 bg-red-50 text-red-700 rounded-md border border-red-200">
              <p className="font-medium">Verification Failed</p>
              <p className="text-sm">{getMessage()}</p>
              <button
                onClick={() => navigate('/register')}
                className="mt-4 inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
              >
                Try Registering Again
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default VerifyEmailPage;
