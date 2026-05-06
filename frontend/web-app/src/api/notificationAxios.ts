// api/notificationAxios.ts
// Mirrors the pattern of qaAxios.ts — points to notification-service on port 8083

import axios from 'axios';
import toast from 'react-hot-toast';

const notificationApi = axios.create({
  baseURL: 'http://localhost:8083/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

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