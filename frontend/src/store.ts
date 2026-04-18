import { configureStore } from '@reduxjs/toolkit';
import { apiSlice } from './services/apiSlice';
<<<<<<< HEAD
import authReducer from './features/auth/authSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
=======

export const store = configureStore({
  reducer: {
>>>>>>> ff3a2ec6328dbe8306d27b4f0eb9a549f3c65124
    [apiSlice.reducerPath]: apiSlice.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(apiSlice.middleware),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
