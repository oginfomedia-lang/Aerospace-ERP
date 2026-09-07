// api/notificationAxios.ts
// Mirrors the pattern of qaAxios.ts — points to notification-service on port 8083

import axios from 'axios';
import toast from 'react-hot-toast';
import { tokenStorage } from '../utils/tokenStorage';
import { API_URLS } from '../config/apiConfig';

const notificationApi = axios.create({
  baseURL: API_URLS.notification,
  headers: { 'Content-Type': 'application/json' },
});

// ✅ Request interceptor – attach JWT token
notificationApi.interceptors.request.use(
  (config) => {
    const token = tokenStorage.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);


notificationApi.interceptors.response.use(
  (res) => res,
  (err) => {
    const msg =
      err.response?.data?.message ||
      err.response?.data?.error ||
      'Notification service unreachable.';
    toast.error(msg);
    return Promise.reject(err);
  }
);

export default notificationApi;