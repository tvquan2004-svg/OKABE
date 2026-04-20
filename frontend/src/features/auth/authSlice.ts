import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface UserInfo {
  id: number;
  email: string;
  username: string;
  avatarUrl: string | null;
}

interface AuthState {
  user: UserInfo | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
}

const initialState: AuthState = {
  user: JSON.parse(localStorage.getItem('okabe_user') ?? 'null'),
  accessToken: localStorage.getItem('okabe_access_token'),
  refreshToken: localStorage.getItem('okabe_refresh_token'),
  isAuthenticated: !!localStorage.getItem('okabe_access_token') && !!sessionStorage.getItem('okabe_session_active'),
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (state, action: PayloadAction<{
      accessToken: string;
      refreshToken: string;
      user: UserInfo;
    }>) => {
      state.accessToken = action.payload.accessToken;
      state.refreshToken = action.payload.refreshToken;
      state.user = action.payload.user;
      state.isAuthenticated = true;

      localStorage.setItem('okabe_access_token', action.payload.accessToken);
      localStorage.setItem('okabe_refresh_token', action.payload.refreshToken);
      localStorage.setItem('okabe_user', JSON.stringify(action.payload.user));
      sessionStorage.setItem('okabe_session_active', 'true');
    },
    logout: (state) => {
      state.accessToken = null;
      state.refreshToken = null;
      state.user = null;
      state.isAuthenticated = false;

      localStorage.removeItem('okabe_access_token');
      localStorage.removeItem('okabe_refresh_token');
      localStorage.removeItem('okabe_user');
    },
  },
});

export const { setCredentials, logout } = authSlice.actions;
export default authSlice.reducer;
